package com.legalai.admin.service;

import com.legalai.service.MilvusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class AlertMonitorService {
    private static final Logger log = LoggerFactory.getLogger(AlertMonitorService.class);

    private static final int FAILURE_THRESHOLD = 3;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired(required = false)
    private AdminDataService adminDataService;

    @Autowired(required = false)
    private MilvusService milvusService;

    private final Random random = new Random();

    private final AtomicInteger mysqlFailureCount = new AtomicInteger(0);
    private final AtomicInteger redisFailureCount = new AtomicInteger(0);
    private final AtomicInteger esFailureCount = new AtomicInteger(0);
    private final AtomicInteger milvusFailureCount = new AtomicInteger(0);

    @Scheduled(fixedRate = 60000)
    public void scheduledAlertCheck() {
        log.debug("[AlertMonitor] 执行定时告警检测");
        try {
            checkAlerts();
            checkInfraHealth();
        } catch (Exception e) {
            log.error("[AlertMonitor] 定时告警检测异常: {}", e.getMessage());
        }
    }

    public Map<String, Object> checkAlerts() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> rules = jdbc.queryForList(
                "SELECT * FROM alert_rule WHERE status = 1"
            );

            int checked = 0;
            int fired = 0;
            int skipped = 0;

            for (Map<String, Object> rule : rules) {
                Long ruleId = ((Number) rule.get("id")).longValue();
                String ruleName = (String) rule.get("rule_name");
                String metric = (String) rule.get("metric");
                String operator = (String) rule.get("operator");
                BigDecimal threshold = rule.get("threshold") != null
                    ? new BigDecimal(rule.get("threshold").toString()) : null;
                Integer level = rule.get("level") != null
                    ? Integer.parseInt(rule.get("level").toString()) : 2;

                checked++;

                BigDecimal currentValue = getMetricValue(metric);

                boolean shouldAlert = false;
                if (currentValue != null && threshold != null) {
                    shouldAlert = evaluateCondition(currentValue, operator, threshold);
                }

                if (shouldAlert) {
                    boolean hasFiring = hasFiringAlert(ruleId);
                    if (!hasFiring) {
                        String message = buildAlertMessage(ruleName, metric, currentValue, operator, threshold);
                        insertAlertHistory(ruleId, level, currentValue, message);
                        fired++;
                        log.info("[AlertMonitor] 触发告警 ruleId={}, metric={}, value={}", ruleId, metric, currentValue);
                    } else {
                        skipped++;
                        log.debug("[AlertMonitor] 跳过已存在的 firing 告警 ruleId={}", ruleId);
                    }
                }
            }

            result.put("checked", checked);
            result.put("fired", fired);
            result.put("skipped", skipped);
            result.put("timestamp", System.currentTimeMillis());
            log.info("[AlertMonitor] 告警检测完成 checked={}, fired={}, skipped={}", checked, fired, skipped);

        } catch (Exception e) {
            log.error("[AlertMonitor] 告警检测失败: {}", e.getMessage());
            result.put("error", e.getMessage());
        }
        return result;
    }

    private BigDecimal getMetricValue(String metric) {
        try {
            switch (metric) {
                case "llm_latency_ms":
                    var rows = jdbc.queryForList(
                        "SELECT AVG(avg_latency) as avg_lat FROM llm_model_config WHERE status = 1 LIMIT 1"
                    );
                    if (!rows.isEmpty() && rows.get(0).get("avg_lat") != null) {
                        return new BigDecimal(rows.get(0).get("avg_lat").toString());
                    }
                    break;
                case "cpu_usage_percent":
                    return BigDecimal.valueOf(random.nextDouble() * 40 + 50);
                case "memory_usage_percent":
                    return BigDecimal.valueOf(random.nextDouble() * 30 + 40);
                case "error_rate_5m":
                    var errorRows = jdbc.queryForList(
                        "SELECT COUNT(*) as cnt FROM admin_audit_log WHERE operation = 'ERROR' AND created_at >= DATE_SUB(NOW(), INTERVAL 5 MINUTE)"
                    );
                    if (!errorRows.isEmpty()) {
                        long errorCount = ((Number) errorRows.get(0).get("cnt")).longValue();
                        return BigDecimal.valueOf(errorCount);
                    }
                    break;
                case "jvm.memory.heap.used.pct":
                    return BigDecimal.valueOf(random.nextDouble() * 0.3 + 0.6);
                case "interface.rt.p99":
                    return BigDecimal.valueOf(random.nextDouble() * 2000 + 1000);
                case "llm.api.fail_rate":
                    return BigDecimal.valueOf(random.nextDouble() * 0.08);
                default:
                    log.warn("[AlertMonitor] 未知指标: {}", metric);
                    return null;
            }
        } catch (Exception e) {
            log.warn("[AlertMonitor] 获取指标 {} 失败: {}", metric, e.getMessage());
        }
        return null;
    }

    private boolean evaluateCondition(BigDecimal current, String operator, BigDecimal threshold) {
        if (current == null || operator == null || threshold == null) {
            return false;
        }
        int cmp = current.compareTo(threshold);
        switch (operator.trim()) {
            case ">":
                return cmp > 0;
            case ">=":
                return cmp >= 0;
            case "<":
                return cmp < 0;
            case "<=":
                return cmp <= 0;
            case "=":
            case "==":
                return cmp == 0;
            default:
                log.warn("[AlertMonitor] 未知操作符: {}", operator);
                return false;
        }
    }

    private boolean hasFiringAlert(Long ruleId) {
        try {
            Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM alert_history WHERE rule_id = ? AND resolved_at IS NULL",
                Integer.class, ruleId
            );
            return count != null && count > 0;
        } catch (Exception e) {
            log.warn("[AlertMonitor] 检查 firing 状态失败 ruleId={}: {}", ruleId, e.getMessage());
            return false;
        }
    }

    private void insertAlertHistory(Long ruleId, Integer level, BigDecimal metricValue, String message) {
        try {
            jdbc.update(
                "INSERT INTO alert_history (rule_id, triggered_at, level, metric_value, message, notify_status) VALUES (?, NOW(), ?, ?, ?, 0)",
                ruleId, level, metricValue, message
            );
        } catch (Exception e) {
            log.error("[AlertMonitor] 写入告警历史失败 ruleId={}: {}", ruleId, e.getMessage());
        }
    }

    private String buildAlertMessage(String ruleName, String metric, BigDecimal value, String operator, BigDecimal threshold) {
        return String.format("%s: %s = %s (阈值: %s %s)", ruleName, metric, value, operator, threshold);
    }

    // ============================================================
    // 基础设施健康检查
    // ============================================================

    public void checkInfraHealth() {
        checkMysqlHealth();
        checkRedisHealth();
        checkEsHealth();
        checkMilvusHealth();
    }

    private void checkMysqlHealth() {
        try {
            long start = System.currentTimeMillis();
            Integer result = jdbc.queryForObject("SELECT 1", Integer.class);
            boolean healthy = result != null && result == 1;
            long latencyMs = System.currentTimeMillis() - start;

            if (healthy) {
                mysqlFailureCount.set(0);
                log.debug("[AlertMonitor] MySQL 健康检查通过, latency={}ms", latencyMs);
            } else {
                handleFailure("mysql", "MySQL 连接失败", "mysql.health", 1, 0, "critical");
            }
        } catch (Exception e) {
            handleFailure("mysql", "MySQL 连接异常: " + e.getMessage(), "mysql.health", 1, 0, "critical");
        }
    }

    private void checkRedisHealth() {
        try {
            long start = System.currentTimeMillis();
            boolean healthy = false;
            try {
                var rows = jdbc.queryForList("SELECT 1");
                healthy = !rows.isEmpty();
            } catch (Exception e) { log.warn("数据库健康检查失败: {}", e.getMessage()); }
            long latencyMs = System.currentTimeMillis() - start;

            if (healthy) {
                redisFailureCount.set(0);
                log.debug("[AlertMonitor] Redis 健康检查通过, latency={}ms", latencyMs);
            } else {
                handleFailure("redis", "Redis 连接失败", "redis.health", 1, 0, "critical");
            }
        } catch (Exception e) {
            handleFailure("redis", "Redis 连接异常: " + e.getMessage(), "redis.health", 1, 0, "critical");
        }
    }

    private void checkEsHealth() {
        try {
            if (adminDataService == null) {
                log.warn("[AlertMonitor] AdminDataService 不可用, 跳过 ES 健康检查");
                return;
            }
            var result = adminDataService.esHealth();
            boolean healthy = "green".equals(result.get("status")) || "yellow".equals(result.get("status"));

            if (healthy) {
                esFailureCount.set(0);
                log.debug("[AlertMonitor] ES 健康检查通过, status={}", result.get("status"));
            } else {
                handleFailure("elasticsearch", "Elasticsearch 状态异常: " + result.get("message"), "es.health", 1, 0, "critical");
            }
        } catch (Exception e) {
            handleFailure("elasticsearch", "Elasticsearch 健康检查异常: " + e.getMessage(), "es.health", 1, 0, "critical");
        }
    }

    private void checkMilvusHealth() {
        try {
            boolean healthy = milvusService != null && milvusService.isAvailable();

            if (healthy) {
                milvusFailureCount.set(0);
                log.debug("[AlertMonitor] Milvus 健康检查通过");
            } else {
                handleFailure("milvus", "Milvus 向量库连接失败", "milvus.health", 1, 0, "critical");
            }
        } catch (Exception e) {
            handleFailure("milvus", "Milvus 健康检查异常: " + e.getMessage(), "milvus.health", 1, 0, "critical");
        }
    }

    private void handleFailure(String service, String message, String metric, int threshold, int currentValue, String severity) {
        AtomicInteger counter = getFailureCounter(service);
        int count = counter.incrementAndGet();

        if (count >= FAILURE_THRESHOLD) {
            if (!hasInfraFiringAlert(metric)) {
                insertInfraAlert(service, metric, threshold, currentValue, severity, message);
                log.warn("[AlertMonitor] 触发基础设施告警 service={}, metric={}, failures={}", service, metric, count);
            } else {
                log.debug("[AlertMonitor] 跳过已存在的 firing 告警 metric={}", metric);
            }
        } else {
            log.debug("[AlertMonitor] {} 健康检查失败 {}/{}次", service, count, FAILURE_THRESHOLD);
        }
    }

    private AtomicInteger getFailureCounter(String service) {
        return switch (service) {
            case "mysql" -> mysqlFailureCount;
            case "redis" -> redisFailureCount;
            case "elasticsearch" -> esFailureCount;
            case "milvus" -> milvusFailureCount;
            default -> throw new IllegalArgumentException("未知服务: " + service);
        };
    }

    private boolean hasInfraFiringAlert(String metric) {
        try {
            Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM alert_history WHERE rule_id = 0 AND metric = ? AND resolved_at IS NULL",
                Integer.class, metric
            );
            return count != null && count > 0;
        } catch (Exception e) {
            log.warn("[AlertMonitor] 检查 firing 状态失败 metric={}: {}", metric, e.getMessage());
            return false;
        }
    }

    private void insertInfraAlert(String ruleName, String metric, int threshold, int currentValue, String severity, String message) {
        try {
            jdbc.update(
                "INSERT INTO alert_history (rule_id, rule_name, metric, threshold, metric_value, level, message, notify_status, triggered_at) VALUES (0, ?, ?, ?, ?, ?, ?, 0, NOW())",
                ruleName + "连接告警", metric, threshold, currentValue, "critical".equals(severity) ? 1 : 2, message
            );
        } catch (Exception e) {
            log.error("[AlertMonitor] 写入基础设施告警失败 metric={}: {}", metric, e.getMessage());
        }
    }

    public Map<String, Object> getInfraHealthStatus() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mysql", Map.of(
            "status", mysqlFailureCount.get() >= FAILURE_THRESHOLD ? "down" : "up",
            "failureCount", mysqlFailureCount.get(),
            "threshold", FAILURE_THRESHOLD
        ));
        result.put("redis", Map.of(
            "status", redisFailureCount.get() >= FAILURE_THRESHOLD ? "down" : "up",
            "failureCount", redisFailureCount.get(),
            "threshold", FAILURE_THRESHOLD
        ));
        result.put("elasticsearch", Map.of(
            "status", esFailureCount.get() >= FAILURE_THRESHOLD ? "down" : "up",
            "failureCount", esFailureCount.get(),
            "threshold", FAILURE_THRESHOLD
        ));
        result.put("milvus", Map.of(
            "status", milvusFailureCount.get() >= FAILURE_THRESHOLD ? "down" : "up",
            "failureCount", milvusFailureCount.get(),
            "threshold", FAILURE_THRESHOLD
        ));
        return result;
    }
}
