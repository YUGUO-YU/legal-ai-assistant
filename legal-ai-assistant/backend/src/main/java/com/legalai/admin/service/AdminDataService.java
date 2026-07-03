package com.legalai.admin.service;

import com.legalai.llm.LLMClient;
import com.legalai.service.MilvusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Service
public class AdminDataService {
    private static final Logger log = LoggerFactory.getLogger(AdminDataService.class);

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired(required = false)
    private LLMClient llmClient;

    @Autowired(required = false)
    private MilvusService milvusService;

    public JdbcTemplate jdbc() {
        return jdbc;
    }

    public Map<String, Object> list(String table, String module, int page, int pageSize, String keyword) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            StringBuilder where = new StringBuilder(" WHERE 1=1 ");
            java.util.List<Object> args = new java.util.ArrayList<>();
            if (module != null && !module.isEmpty()) {
                where.append(" AND biz_module = ? ");
                args.add(module);
            }
            if (keyword != null && !keyword.isEmpty()) {
                where.append(" AND (");
                where.append("CAST(id AS CHAR) LIKE ? OR ");
                where.append("COALESCE(username,'') LIKE ? OR ");
                where.append("COALESCE(real_name,'') LIKE ? OR ");
                where.append("COALESCE(title,'') LIKE ? OR ");
                where.append("COALESCE(template_name,'') LIKE ? OR ");
                where.append("COALESCE(role_name,'') LIKE ? OR ");
                where.append("COALESCE(menu_name,'') LIKE ? OR ");
                where.append("COALESCE(operation,'') LIKE ? OR ");
                where.append("COALESCE(rule_name,'') LIKE ? OR ");
                where.append("COALESCE(message,'') LIKE ? OR ");
                where.append("COALESCE(config_key,'') LIKE ? OR ");
                where.append("COALESCE(dict_type,'') LIKE ? OR ");
                where.append("COALESCE(model_code,'') LIKE ? OR ");
                where.append("COALESCE(prompt_code,'') LIKE ? OR ");
                where.append("COALESCE(task_name,'') LIKE ? OR ");
                where.append("COALESCE(content,'') LIKE ?");
                where.append(") ");
                String like = "%" + keyword + "%";
                for (int i = 0; i < 16; i++) args.add(like);
            }

            String safeTable = sanitize(table);

            // Check if table exists first
            try {
                jdbc.queryForObject("SELECT COUNT(*) FROM " + safeTable + " WHERE 1=0", Integer.class);
            } catch (Exception e) {
                log.error("[Admin] 表不存在或无法访问 table={}: {}", table, e.getMessage());
                result.put("total", 0);
                result.put("page", page);
                result.put("pageSize", pageSize);
                result.put("list", java.util.Collections.emptyList());
                result.put("error", "表不存在或数据库未初始化: " + table);
                result.put("errorType", "table_not_found");
                return result;
            }

            Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM " + safeTable + where, Integer.class, args.toArray());
            int offset = Math.max(0, (page - 1) * pageSize);

            StringBuilder q = new StringBuilder("SELECT * FROM ").append(safeTable).append(where);
            q.append(" ORDER BY id DESC LIMIT ? OFFSET ?");
            args.add(pageSize);
            args.add(offset);
            List<Map<String, Object>> rows = jdbc.queryForList(q.toString(), args.toArray());

            result.put("total", total == null ? 0 : total);
            result.put("page", page);
            result.put("pageSize", pageSize);
            result.put("list", rows);
            result.put("source", "admin-db");
        } catch (Exception e) {
            log.warn("[Admin] 列表查询失败 table={}: {}", table, e.getMessage());
            result.put("total", 0);
            result.put("page", page);
            result.put("pageSize", pageSize);
            result.put("list", java.util.Collections.emptyList());
            result.put("error", e.getMessage());
            result.put("errorType", "query_failed");
        }
        return result;
    }

    public Map<String, Object> listRevisionsByLawId(Long lawId) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            String sql = "SELECT * FROM law_revision WHERE law_id = ? ORDER BY id DESC";
            List<Map<String, Object>> rows = jdbc.queryForList(sql, lawId);
            result.put("total", rows.size());
            result.put("page", 1);
            result.put("pageSize", rows.size());
            result.put("list", rows);
            result.put("source", "admin-db");
        } catch (Exception e) {
            log.warn("[Admin] 法规修订查询失败 lawId={}: {}", lawId, e.getMessage());
            result.put("total", 0);
            result.put("page", 1);
            result.put("pageSize", 0);
            result.put("list", java.util.Collections.emptyList());
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> detail(String table, Long id) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            String safeTable = sanitize(table);
            List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM " + safeTable + " WHERE id = ?", id);
            result.put("data", rows.isEmpty() ? null : rows.get(0));
            result.put("source", "admin-db");
        } catch (Exception e) {
            log.warn("[Admin] 详情查询失败 table={} id={}: {}", table, id, e.getMessage());
            result.put("data", null);
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> stats() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            String[] tables = {
                "admin_user", "admin_role", "admin_audit_log",
                "doc_template", "doc_draft", "prompt_template", "llm_model_config", "llm_token_usage",
                "alert_rule", "alert_history",
                "law_document", "legal_case", "tb_case",
                "kb_knowledge_base", "kb_document",
                "search_log", "user_feedback", "sys_config"
            };
            Map<String, Long> counts = new LinkedHashMap<>();
            for (String t : tables) {
                try {
                    Long c = jdbc.queryForObject("SELECT COUNT(*) FROM " + sanitize(t), Long.class);
                    counts.put(t, c == null ? 0L : c);
                } catch (Exception ignore) {
                    counts.put(t, -1L);
                }
            }
            result.put("counts", counts);
            result.put("source", "admin-db");
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> audit(String userId, String operation, String module, int page, int pageSize) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            StringBuilder where = new StringBuilder(" WHERE 1=1 ");
            List<Object> args = new java.util.ArrayList<>();
            if (userId != null && !userId.isEmpty()) {
                try {
                    where.append(" AND user_id = ? ");
                    args.add(Long.valueOf(userId));
                } catch (NumberFormatException e) {
                    result.put("total", 0);
                    result.put("list", java.util.Collections.emptyList());
                    result.put("error", "userId 格式非法，请传入数字");
                    return result;
                }
            }
            if (operation != null && !operation.isEmpty()) { where.append(" AND operation = ? "); args.add(operation); }
            if (module != null && !module.isEmpty()) { where.append(" AND biz_module = ? "); args.add(module); }
            Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM admin_audit_log" + where, Integer.class, args.toArray());
            int offset = Math.max(0, (page - 1) * pageSize);
            args.add(pageSize); args.add(offset);
            List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM admin_audit_log" + where + " ORDER BY id DESC LIMIT ? OFFSET ?", args.toArray());
            result.put("total", total == null ? 0 : total);
            result.put("list", rows);
        } catch (Exception e) {
            result.put("total", 0);
            result.put("list", java.util.Collections.emptyList());
            result.put("error", e.getMessage());
        }
        return result;
    }

    public void recordAudit(Long userId, String username, String operation, String bizModule,
                            String bizType, String bizId, String url, String method,
                            String params, String result, String ip, int duration, boolean ok, String error) {
        try {
            jdbc.update("INSERT INTO admin_audit_log(user_id, username, operation, biz_module, biz_type, biz_id, request_url, request_method, request_params, response_result, ip, duration_ms, status, error_msg, trace_id, created_at) " +
                            "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW())",
                    userId, username, operation, bizModule, bizType, bizId, url, method,
                    truncate(params, 4000), truncate(result, 4000), ip, duration, ok ? 1 : 0,
                    truncate(error, 2000), java.util.UUID.randomUUID().toString().replace("-", ""));
        } catch (Exception e) {
            log.warn("[Admin] 审计写入失败: {}", e.getMessage());
        }
    }

    private static String sanitize(String table) {
        if (table == null) return "admin_user";
        String t = table.trim();
        if (!t.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
            throw new IllegalArgumentException("非法的表名: " + table);
        }
        return t;
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    // ============================================================
    // CRUD：通用 create / update / delete
    // ============================================================

    public Map<String, Object> create(String table, Map<String, Object> payload) {
        Map<String, Object> result = new LinkedHashMap<>();
        String safe = sanitize(table);
        if (payload == null || payload.isEmpty()) {
            result.put("ok", false);
            result.put("error", "payload 为空");
            return result;
        }
        List<String> cols = new java.util.ArrayList<>();
        List<Object> vals = new java.util.ArrayList<>();
        for (Map.Entry<String, Object> e : payload.entrySet()) {
            String k = e.getKey();
            if (k == null || k.isEmpty() || "id".equals(k) || "created_at".equals(k) || "updated_at".equals(k)) continue;
            if (!k.matches("[a-zA-Z_][a-zA-Z0-9_]*")) continue;
            cols.add(k);
            vals.add(e.getValue());
        }
        if (cols.isEmpty()) {
            result.put("ok", false);
            result.put("error", "无可写字段");
            return result;
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(cols.size(), "?"));
        String sql = "INSERT INTO " + safe + " (" + String.join(",", cols) + ") VALUES (" + placeholders + ")";
        try {
            int n = jdbc.update(sql, vals.toArray());
            result.put("ok", n > 0);
            result.put("affected", n);
            // 取最新 ID
            try {
                Long newId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
                result.put("id", newId);
            } catch (Exception ignore) {}
        } catch (Exception e) {
            log.warn("[Admin] create 失败 table={}: {}", table, e.getMessage());
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> update(String table, Long id, Map<String, Object> payload) {
        Map<String, Object> result = new LinkedHashMap<>();
        String safe = sanitize(table);
        if (payload == null || payload.isEmpty()) {
            result.put("ok", false);
            result.put("error", "payload 为空");
            return result;
        }
        List<String> sets = new java.util.ArrayList<>();
        List<Object> vals = new java.util.ArrayList<>();
        for (Map.Entry<String, Object> e : payload.entrySet()) {
            String k = e.getKey();
            if (k == null || k.isEmpty() || "id".equals(k) || "created_at".equals(k)) continue;
            if (!k.matches("[a-zA-Z_][a-zA-Z0-9_]*")) continue;
            sets.add(k + " = ?");
            vals.add(e.getValue());
        }
        if (sets.isEmpty()) {
            result.put("ok", false);
            result.put("error", "无可写字段");
            return result;
        }
        vals.add(id);
        String sql = "UPDATE " + safe + " SET " + String.join(",", sets) + " WHERE id = ?";
        try {
            int n = jdbc.update(sql, vals.toArray());
            result.put("ok", n > 0);
            result.put("affected", n);
        } catch (Exception e) {
            log.warn("[Admin] update 失败 table={} id={}: {}", table, id, e.getMessage());
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> delete(String table, Long id) {
        Map<String, Object> result = new LinkedHashMap<>();
        String safe = sanitize(table);
        try {
            int n = jdbc.update("DELETE FROM " + safe + " WHERE id = ?", id);
            result.put("ok", n > 0);
            result.put("affected", n);
        } catch (Exception e) {
            log.warn("[Admin] delete 失败 table={} id={}: {}", table, id, e.getMessage());
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> toggle(String table, Long id, String column) {
        Map<String, Object> result = new LinkedHashMap<>();
        String safe = sanitize(table);
        if (column == null || !column.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
            result.put("ok", false);
            result.put("error", "非法列名");
            return result;
        }
        try {
            List<Map<String, Object>> rows = jdbc.queryForList("SELECT " + column + " FROM " + safe + " WHERE id = ?", id);
            if (rows.isEmpty()) { result.put("ok", false); result.put("error", "记录不存在"); return result; }
            Object cur = rows.get(0).get(column);
            int next = (cur != null && Integer.parseInt(String.valueOf(cur)) == 1) ? 0 : 1;
            jdbc.update("UPDATE " + safe + " SET " + column + " = ? WHERE id = ?", next, id);
            result.put("ok", true);
            result.put("value", next);
        } catch (Exception e) {
            log.warn("[Admin] toggle 失败 table={} id={} col={}: {}", table, id, column, e.getMessage());
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    // ============================================================
    // 告警工作流：ack / resolve
    // ============================================================

    public Map<String, Object> ackAlert(Long historyId, Long handlerId) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            int n = jdbc.update("UPDATE alert_history SET notify_status = 2, handler_id = ? WHERE id = ? AND notify_status IN (0, 1)", handlerId, historyId);
            result.put("ok", n > 0);
            result.put("affected", n);
            if (n == 0) result.put("error", "该告警已确认或不存在");
        } catch (Exception e) {
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> resolveAlert(Long historyId) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            int n = jdbc.update("UPDATE alert_history SET resolved_at = NOW(), notify_status = 3 WHERE id = ? AND resolved_at IS NULL", historyId);
            result.put("ok", n > 0);
            result.put("affected", n);
            if (n == 0) result.put("error", "该告警已解决或不存在");
        } catch (Exception e) {
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    // ============================================================
    // 法规审核状态机：audit_law
    // ============================================================

    public Map<String, Object> auditLaw(Long lawId, int action, Long auditorId) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> rows = jdbc.queryForList("SELECT id, audit_status FROM law_document WHERE id = ?", lawId);
            if (rows.isEmpty()) { result.put("ok", false); result.put("error", "法规不存在"); return result; }
            int cur = Integer.parseInt(String.valueOf(rows.get(0).get("audit_status")));
            int next;
            String err = null;
            if (action == 1) {
                if (cur == 0) next = 1;
                else if (cur == 1) next = 2;
                else { err = "当前状态不允许通过"; next = cur; }
            } else if (action == 2) {
                next = 3;
            } else { result.put("ok", false); result.put("error", "未知动作"); return result; }
            if (err != null) { result.put("ok", false); result.put("error", err); return result; }
            jdbc.update("UPDATE law_document SET audit_status = ?, auditor_id = ?, audit_time = NOW() WHERE id = ?", next, auditorId, lawId);
            result.put("ok", true);
            result.put("fromStatus", cur);
            result.put("toStatus", next);
        } catch (Exception e) {
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    // ============================================================
    // 草稿复核
    // ============================================================

    public Map<String, Object> reviewDraft(Long draftId, int action, Long reviewerId, String note) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> rows = jdbc.queryForList("SELECT id, review_status FROM doc_draft WHERE id = ?", draftId);
            if (rows.isEmpty()) { result.put("ok", false); result.put("error", "草稿不存在"); return result; }
            int cur = Integer.parseInt(String.valueOf(rows.get(0).get("review_status")));
            int next;
            if (action == 1) { next = 1; }       // 通过
            else if (action == 2) { next = 2; }   // 驳回
            else if (action == 3) { next = 0; }   // 退回修改
            else { result.put("ok", false); result.put("error", "未知动作"); return result; }
            jdbc.update("UPDATE doc_draft SET review_status = ?, reviewer_id = ?, review_note = ?, reviewed_at = NOW() WHERE id = ?", next, reviewerId, note, draftId);
            result.put("ok", true);
            result.put("fromStatus", cur);
            result.put("toStatus", next);
        } catch (Exception e) {
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    // ============================================================
    // Prompt 工作流：发布 / 灰度 / 回滚
    // ============================================================

    public Map<String, Object> publishPrompt(Long promptId) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> rows = jdbc.queryForList("SELECT prompt_code, version FROM prompt_template WHERE id = ?", promptId);
            if (rows.isEmpty()) { result.put("ok", false); result.put("error", "Prompt 不存在"); return result; }
            String code = String.valueOf(rows.get(0).get("prompt_code"));
            jdbc.update("UPDATE prompt_template SET is_active = 0 WHERE prompt_code = ?", code);
            jdbc.update("UPDATE prompt_template SET is_active = 1, is_gray = 0, gray_ratio = 100 WHERE id = ?", promptId);
            jdbc.update("INSERT INTO prompt_gray_release(prompt_id, to_version, ratio, started_at, created_at) VALUES(?, ?, 100, NOW(), NOW())",
                    promptId, String.valueOf(rows.get(0).get("version")));
            result.put("ok", true);
        } catch (Exception e) {
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> grayPrompt(Long promptId, int ratio, String teams) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (ratio < 0 || ratio > 100) { result.put("ok", false); result.put("error", "灰度比例 0-100"); return result; }
        try {
            jdbc.update("UPDATE prompt_template SET is_active = 1, is_gray = 1, gray_ratio = ?, gray_teams = ? WHERE id = ?",
                    ratio, teams, promptId);
            result.put("ok", true);
        } catch (Exception e) {
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> rollbackPrompt(Long promptId, String reason) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> rows = jdbc.queryForList("SELECT prompt_code, version FROM prompt_template WHERE id = ?", promptId);
            if (rows.isEmpty()) { result.put("ok", false); result.put("error", "Prompt 不存在"); return result; }
            String code = String.valueOf(rows.get(0).get("prompt_code"));
            String ver = String.valueOf(rows.get(0).get("version"));
            jdbc.update("UPDATE prompt_template SET is_gray = 0, gray_ratio = 0 WHERE prompt_code = ? AND id <> ?", code, promptId);
            jdbc.update("UPDATE prompt_gray_release SET ended_at = NOW(), rollback_reason = ? WHERE prompt_id = ? AND ended_at IS NULL", reason, promptId);
            result.put("ok", true);
            result.put("rolledBackVersion", ver);
        } catch (Exception e) {
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    // ============================================================
    // 系统监控概览
    // ============================================================

    public Map<String, Object> monitorOverview() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            Integer activeAlerts = jdbc.queryForObject("SELECT COUNT(*) FROM alert_history WHERE resolved_at IS NULL", Integer.class);
            Integer pendingDrafts = jdbc.queryForObject("SELECT COUNT(*) FROM doc_draft WHERE review_status = 0", Integer.class);
            Integer pendingLaws = jdbc.queryForObject("SELECT COUNT(*) FROM law_document WHERE audit_status = 0", Integer.class);
            Integer pendingFeedback = jdbc.queryForObject("SELECT COUNT(*) FROM user_feedback WHERE status = 0", Integer.class);
            Integer totalTokens = jdbc.queryForObject("SELECT COALESCE(SUM(total_tokens), 0) FROM llm_token_usage WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)", Integer.class);
            Integer totalCost = jdbc.queryForObject("SELECT COALESCE(SUM(cost_cny), 0) FROM llm_token_usage WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)", Integer.class);

            result.put("activeAlerts", activeAlerts == null ? 0 : activeAlerts);
            result.put("pendingDrafts", pendingDrafts == null ? 0 : pendingDrafts);
            result.put("pendingLaws", pendingLaws == null ? 0 : pendingLaws);
            result.put("pendingFeedback", pendingFeedback == null ? 0 : pendingFeedback);
            result.put("weeklyTokens", totalTokens == null ? 0 : totalTokens);
            result.put("weeklyCost", totalCost == null ? 0 : totalCost);

            // 按模块 token 分布
            List<Map<String, Object>> moduleTokens = jdbc.queryForList(
                "SELECT module, SUM(total_tokens) AS tokens FROM llm_token_usage WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) GROUP BY module ORDER BY tokens DESC"
            );
            result.put("moduleTokens", moduleTokens);

            // 按状态 alert 分布
            List<Map<String, Object>> alertByLevel = jdbc.queryForList(
                "SELECT level, COUNT(*) AS cnt FROM alert_history WHERE resolved_at IS NULL GROUP BY level"
            );
            result.put("alertByLevel", alertByLevel);

            // 近 7 天 token 趋势
            List<Map<String, Object>> tokenTrend = jdbc.queryForList(
                "SELECT DATE(created_at) AS day, SUM(total_tokens) AS tokens FROM llm_token_usage " +
                "WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) GROUP BY DATE(created_at) ORDER BY day"
            );
            result.put("tokenTrend", tokenTrend);

            // 告警最近 10 条
            List<Map<String, Object>> recentAlerts = jdbc.queryForList(
                "SELECT * FROM alert_history ORDER BY triggered_at DESC LIMIT 10"
            );
            result.put("recentAlerts", recentAlerts);
        } catch (Exception e) {
            log.warn("[Admin] monitorOverview 失败: {}", e.getMessage());
            result.put("error", e.getMessage());
        }
        return result;
    }

    // ============================================================
    // LLM 健康探测（模拟）
    // ============================================================

    public Map<String, Object> llmHealthCheck() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> models = jdbc.queryForList("SELECT * FROM llm_model_config WHERE status = 1");
            List<Map<String, Object>> checks = new java.util.ArrayList<>();
            for (Map<String, Object> m : models) {
                Map<String, Object> ck = new LinkedHashMap<>();
                ck.put("model_code", m.get("model_code"));
                ck.put("model_name", m.get("model_name"));
                ck.put("is_primary", m.get("is_primary"));
                long t0 = System.currentTimeMillis();
                boolean healthy;
                long dur;
                if (llmClient != null) {
                    Map<String, Object> probe = llmClient.healthCheck();
                    healthy = (boolean) probe.get("healthy");
                    dur = (long) probe.get("latencyMs");
                } else {
                    healthy = Integer.parseInt(String.valueOf(m.getOrDefault("health_status", 1))) == 1;
                    dur = (long) (Math.random() * 80 + 20);
                }
                ck.put("healthy", healthy);
                ck.put("latencyMs", dur);
                ck.put("checkedAt", new java.sql.Timestamp(System.currentTimeMillis()).toString());
                checks.add(ck);
                jdbc.update("UPDATE llm_model_config SET last_check_at = NOW(), health_status = ? WHERE id = ?",
                        healthy ? 1 : 0, m.get("id"));
            }
            result.put("checks", checks);
            result.put("checkedAt", new java.sql.Timestamp(System.currentTimeMillis()).toString());
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        return result;
    }

    // ============================================================
    // Milvus 集合状态（模拟）
    // ============================================================

    public Map<String, Object> milvusCollections() {
        if (milvusService != null) {
            return milvusService.getCollectionsStatus();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> cols = new java.util.ArrayList<>();
        String[] names = {"legal_law_articles", "legal_cases", "legal_contracts", "kb_documents"};
        Random r = new Random(42);
        for (String n : names) {
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("name", n);
            c.put("count", 1000 + r.nextInt(50000));
            c.put("indexStatus", r.nextBoolean() ? "健康" : "重建中");
            c.put("dim", 1536);
            c.put("metricType", "IP");
            c.put("indexType", "HNSW");
            cols.add(c);
        }
        result.put("collections", cols);
        result.put("checkedAt", new java.sql.Timestamp(System.currentTimeMillis()).toString());
        return result;
    }

    public boolean setActiveModel(Long id) {
        try {
            jdbc.update("UPDATE llm_model_config SET is_primary = 0");
            jdbc.update("UPDATE llm_model_config SET is_primary = 1 WHERE id = ?", id);
            log.info("已将模型 id={} 设为活跃模型", id);
            return true;
        } catch (Exception e) {
            log.error("设置活跃模型失败: {}", e.getMessage());
            return false;
        }
    }

    public boolean updateModelApiKey(Long id, String apiKey) {
        try {
            if (apiKey == null || apiKey.trim().isEmpty()) {
                return false;
            }
            jdbc.update("UPDATE llm_model_config SET api_key_enc = ? WHERE id = ?", apiKey.trim(), id);
            log.info("已更新模型 id={} 的API密钥", id);
            return true;
        } catch (Exception e) {
            log.error("更新API密钥失败: {}", e.getMessage());
            return false;
        }
    }

    public Map<String, Object> getActiveModelConfigFromDb() {
        try {
            var list = jdbc.queryForList(
                "SELECT * FROM llm_model_config WHERE is_primary = 1 AND status = 1 LIMIT 1");
            if (!list.isEmpty()) {
                return list.get(0);
            }
        } catch (Exception e) {
            log.warn("获取活跃模型失败: {}", e.getMessage());
        }
        return null;
    }

    // ============================================================
    // DOC_TEMPLATE (Mod03) CRUD
    // ============================================================

    public Map<String, Object> listDocTemplates(String category) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            StringBuilder where = new StringBuilder(" WHERE biz_module = 'MOD-03' ");
            java.util.List<Object> args = new java.util.ArrayList<>();
            if (category != null && !category.isEmpty()) {
                where.append(" AND category = ? ");
                args.add(category);
            }
            Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM doc_template " + where, Integer.class, args.toArray());
            StringBuilder q = new StringBuilder("SELECT * FROM doc_template ").append(where).append(" ORDER BY id DESC LIMIT 100 OFFSET 0");
            List<Map<String, Object>> rows = jdbc.queryForList(q.toString(), args.toArray());
            result.put("total", total == null ? 0 : total);
            result.put("list", rows);
            result.put("source", "admin-db");
        } catch (Exception e) {
            log.error("查询 doc_template 失败: {}", e.getMessage());
            result.put("total", 0);
            result.put("list", java.util.Collections.emptyList());
            result.put("error", e.getMessage());
        }
        return result;
    }

    public void createDocTemplate(Map<String, Object> data) {
        String sql = """
            INSERT INTO doc_template (template_code, template_name, category, schema_json, risk_rules, review_required, status, version, biz_module)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'MOD-03')
            """;
        jdbc.update(sql,
            data.get("template_code"),
            data.get("template_name"),
            data.get("category"),
            data.get("schema_json"),
            data.get("risk_rules"),
            data.get("review_required") != null ? data.get("review_required") : 1,
            data.get("status") != null ? data.get("status") : 1,
            data.get("version")
        );
        log.info("创建文书模板: {}", data.get("template_code"));
    }

    public void updateDocTemplate(Long id, Map<String, Object> data) {
        StringBuilder sql = new StringBuilder("UPDATE doc_template SET ");
        java.util.List<Object> args = new java.util.ArrayList<>();
        if (data.get("template_name") != null) { sql.append("template_name = ?, "); args.add(data.get("template_name")); }
        if (data.get("category") != null) { sql.append("category = ?, "); args.add(data.get("category")); }
        if (data.get("schema_json") != null) { sql.append("schema_json = ?, "); args.add(data.get("schema_json")); }
        if (data.get("risk_rules") != null) { sql.append("risk_rules = ?, "); args.add(data.get("risk_rules")); }
        if (data.get("review_required") != null) { sql.append("review_required = ?, "); args.add(data.get("review_required")); }
        if (data.get("status") != null) { sql.append("status = ?, "); args.add(data.get("status")); }
        if (data.get("version") != null) { sql.append("version = ?, "); args.add(data.get("version")); }
        if (args.isEmpty()) return;
        sql.setLength(sql.length() - 2);
        sql.append(" WHERE id = ?");
        args.add(id);
        jdbc.update(sql.toString(), args.toArray());
        log.info("更新文书模板 id={}", id);
    }

    public void deleteDocTemplate(Long id) {
        jdbc.update("DELETE FROM doc_template WHERE id = ?", id);
        log.info("删除文书模板 id={}", id);
    }

    public void toggleDocTemplate(Long id, Integer status) {
        jdbc.update("UPDATE doc_template SET status = ? WHERE id = ?", status, id);
        log.info("切换文书模板 id={} status={}", id, status);
    }

    // ============================================================
    // DOC_REVIEW_RULE CRUD
    // ============================================================

    public void createDocReviewRule(Map<String, Object> data) {
        String sql = """
            INSERT INTO doc_review_rule (template_code, rule_type, operator, threshold, trigger_action, status, rule_name)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        jdbc.update(sql,
            data.get("template_code"),
            data.get("rule_type"),
            data.get("operator"),
            data.get("threshold"),
            data.get("trigger_action"),
            data.get("status") != null ? data.get("status") : 1,
            data.get("rule_name") != null ? data.get("rule_name") : data.get("template_code")
        );
        log.info("创建文书复核规则: {}", data.get("template_code"));
    }

    public void updateDocReviewRule(Long id, Map<String, Object> data) {
        StringBuilder sql = new StringBuilder("UPDATE doc_review_rule SET ");
        java.util.List<Object> args = new java.util.ArrayList<>();
        if (data.get("template_code") != null) { sql.append("template_code = ?, "); args.add(data.get("template_code")); }
        if (data.get("rule_type") != null) { sql.append("rule_type = ?, "); args.add(data.get("rule_type")); }
        if (data.get("operator") != null) { sql.append("operator = ?, "); args.add(data.get("operator")); }
        if (data.get("threshold") != null) { sql.append("threshold = ?, "); args.add(data.get("threshold")); }
        if (data.get("trigger_action") != null) { sql.append("trigger_action = ?, "); args.add(data.get("trigger_action")); }
        if (data.get("status") != null) { sql.append("status = ?, "); args.add(data.get("status")); }
        if (data.get("rule_name") != null) { sql.append("rule_name = ?, "); args.add(data.get("rule_name")); }
        if (args.isEmpty()) return;
        sql.setLength(sql.length() - 2);
        sql.append(" WHERE id = ?");
        args.add(id);
        jdbc.update(sql.toString(), args.toArray());
        log.info("更新文书复核规则 id={}", id);
    }

    public void deleteDocReviewRule(Long id) {
        jdbc.update("DELETE FROM doc_review_rule WHERE id = ?", id);
        log.info("删除文书复核规则 id={}", id);
    }

    public void toggleDocReviewRule(Long id, Integer status) {
        jdbc.update("UPDATE doc_review_rule SET status = ? WHERE id = ?", status, id);
        log.info("切换文书复核规则 id={} status={}", id, status);
    }

    // ============================================================
    // PROMPT_TEMPLATE CRUD
    // ============================================================

    public Map<String, Object> createPromptTemplate(Map<String, Object> data) {
        Map<String, Object> result = new LinkedHashMap<>();
        String sql = """
            INSERT INTO prompt_template (prompt_code, module, scene, version, content, variables, is_active, is_gray, gray_ratio, adopt_rate, feedback_score, biz_module)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try {
            jdbc.update(sql,
                data.get("prompt_code"),
                data.get("module"),
                data.get("scene"),
                data.get("version"),
                data.get("content"),
                data.get("variables"),
                0,
                0,
                0,
                data.get("adopt_rate") != null ? data.get("adopt_rate") : 0.0,
                data.get("feedback_score") != null ? data.get("feedback_score") : 0.0,
                data.get("module") != null ? data.get("module") : "MOD-01"
            );
            log.info("创建 Prompt 模板: {}", data.get("prompt_code"));
            List<Map<String, Object>> rows = jdbc.queryForList("SELECT LAST_INSERT_ID() as id");
            Long id = rows.isEmpty() ? null : (Long) rows.get(0).get("id");
            result.put("ok", true);
            result.put("id", id);
        } catch (Exception e) {
            log.error("创建 Prompt 模板失败: {}", e.getMessage());
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> updatePromptTemplate(Long id, Map<String, Object> data) {
        Map<String, Object> result = new LinkedHashMap<>();
        StringBuilder sql = new StringBuilder("UPDATE prompt_template SET ");
        java.util.List<Object> args = new java.util.ArrayList<>();
        if (data.get("prompt_code") != null) { sql.append("prompt_code = ?, "); args.add(data.get("prompt_code")); }
        if (data.get("module") != null) { sql.append("module = ?, "); args.add(data.get("module")); }
        if (data.get("scene") != null) { sql.append("scene = ?, "); args.add(data.get("scene")); }
        if (data.get("version") != null) { sql.append("version = ?, "); args.add(data.get("version")); }
        if (data.get("content") != null) { sql.append("content = ?, "); args.add(data.get("content")); }
        if (data.get("variables") != null) { sql.append("variables = ?, "); args.add(data.get("variables")); }
        if (data.get("adopt_rate") != null) { sql.append("adopt_rate = ?, "); args.add(data.get("adopt_rate")); }
        if (data.get("feedback_score") != null) { sql.append("feedback_score = ?, "); args.add(data.get("feedback_score")); }
        if (args.isEmpty()) {
            result.put("ok", false);
            result.put("error", "无可更新字段");
            return result;
        }
        sql.setLength(sql.length() - 2);
        sql.append(" WHERE id = ?");
        args.add(id);
        try {
            jdbc.update(sql.toString(), args.toArray());
            log.info("更新 Prompt 模板 id={}", id);
            result.put("ok", true);
        } catch (Exception e) {
            log.error("更新 Prompt 模板失败: {}", e.getMessage());
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> deletePromptTemplate(Long id) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            jdbc.update("DELETE FROM prompt_template WHERE id = ?", id);
            log.info("删除 Prompt 模板 id={}", id);
            result.put("ok", true);
        } catch (Exception e) {
            log.error("删除 Prompt 模板失败: {}", e.getMessage());
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> listFrontendUsers(int page, int pageSize, String keyword) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            StringBuilder where = new StringBuilder(" WHERE 1=1 ");
            java.util.List<Object> args = new java.util.ArrayList<>();
            if (keyword != null && !keyword.isEmpty()) {
                where.append(" AND (id LIKE ? OR username LIKE ? OR real_name LIKE ? OR email LIKE ? OR phone LIKE ?)");
                String like = "%" + keyword + "%";
                args.add(like);
                args.add(like);
                args.add(like);
                args.add(like);
                args.add(like);
            }
            Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM frontend_user" + where, Integer.class, args.toArray());
            int offset = Math.max(0, (page - 1) * pageSize);
            String sql = "SELECT * FROM frontend_user" + where + " ORDER BY created_at DESC LIMIT ? OFFSET ?";
            args.add(pageSize);
            args.add(offset);
            List<Map<String, Object>> rows = jdbc.queryForList(sql, args.toArray());
            result.put("total", total == null ? 0 : total);
            result.put("page", page);
            result.put("pageSize", pageSize);
            result.put("list", rows);
            result.put("source", "admin-db");
        } catch (Exception e) {
            log.warn("[Admin] 前端用户列表查询失败: {}", e.getMessage());
            result.put("total", 0);
            result.put("page", page);
            result.put("pageSize", pageSize);
            result.put("list", java.util.Collections.emptyList());
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> createFrontendUser(Map<String, Object> payload) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (payload == null || payload.isEmpty()) {
            result.put("ok", false);
            result.put("error", "payload 为空");
            return result;
        }
        String username = (String) payload.get("username");
        String password = (String) payload.get("password");
        String realName = (String) payload.get("real_name");
        String email = (String) payload.get("email");
        String phone = (String) payload.get("phone");
        if (username == null || username.isEmpty()) {
            result.put("ok", false);
            result.put("error", "用户名为空");
            return result;
        }
        if (password == null || password.length() < 6) {
            result.put("ok", false);
            result.put("error", "密码长度需至少6位");
            return result;
        }
        try {
            var existing = jdbc.queryForList("SELECT id FROM frontend_user WHERE username = ?", username);
            if (!existing.isEmpty()) {
                result.put("ok", false);
                result.put("error", "用户名已被注册");
                return result;
            }
            if (email != null && !email.isEmpty()) {
                var existingEmail = jdbc.queryForList("SELECT id FROM frontend_user WHERE email = ?", email);
                if (!existingEmail.isEmpty()) {
                    result.put("ok", false);
                    result.put("error", "邮箱已被注册");
                    return result;
                }
            }
            String userId = "u-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            String hashedPassword = org.apache.commons.codec.digest.DigestUtils.sha256Hex(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            jdbc.update(
                "INSERT INTO frontend_user (id, username, password, real_name, email, phone, status) VALUES (?, ?, ?, ?, ?, ?, 1)",
                userId, username, hashedPassword, realName, email, phone);
            log.info("创建前端用户: username={}, userId={}", username, userId);
            result.put("ok", true);
            result.put("id", userId);
        } catch (Exception e) {
            log.warn("[Admin] 创建前端用户失败: {}", e.getMessage());
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> updateFrontendUser(String id, Map<String, Object> payload) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (payload == null || payload.isEmpty()) {
            result.put("ok", false);
            result.put("error", "payload 为空");
            return result;
        }
        try {
            var existing = jdbc.queryForList("SELECT id FROM frontend_user WHERE id = ?", id);
            if (existing.isEmpty()) {
                result.put("ok", false);
                result.put("error", "用户不存在");
                return result;
            }
            java.util.List<String> sets = new java.util.ArrayList<>();
            java.util.List<Object> vals = new java.util.ArrayList<>();
            if (payload.containsKey("real_name")) {
                sets.add("real_name = ?");
                vals.add(payload.get("real_name"));
            }
            if (payload.containsKey("email")) {
                String email = (String) payload.get("email");
                if (email != null && !email.isEmpty()) {
                    var existingEmail = jdbc.queryForList("SELECT id FROM frontend_user WHERE email = ? AND id != ?", email, id);
                    if (!existingEmail.isEmpty()) {
                        result.put("ok", false);
                        result.put("error", "邮箱已被其他用户使用");
                        return result;
                    }
                }
                sets.add("email = ?");
                vals.add(email);
            }
            if (payload.containsKey("phone")) {
                sets.add("phone = ?");
                vals.add(payload.get("phone"));
            }
            if (payload.containsKey("status")) {
                sets.add("status = ?");
                vals.add(payload.get("status"));
            }
            if (payload.containsKey("password") && payload.get("password") != null && !((String) payload.get("password")).isEmpty()) {
                String pwd = (String) payload.get("password");
                if (pwd.length() < 6) {
                    result.put("ok", false);
                    result.put("error", "密码长度需至少6位");
                    return result;
                }
                sets.add("password = ?");
                vals.add(org.apache.commons.codec.digest.DigestUtils.sha256Hex(pwd.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            }
            if (sets.isEmpty()) {
                result.put("ok", false);
                result.put("error", "无可更新字段");
                return result;
            }
            vals.add(id);
            String sql = "UPDATE frontend_user SET " + String.join(", ", sets) + " WHERE id = ?";
            int n = jdbc.update(sql, vals.toArray());
            result.put("ok", n > 0);
            result.put("affected", n);
        } catch (Exception e) {
            log.warn("[Admin] 更新前端用户失败 id={}: {}", id, e.getMessage());
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> deleteFrontendUser(String id) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            int n = jdbc.update("DELETE FROM frontend_user WHERE id = ?", id);
            result.put("ok", n > 0);
            result.put("affected", n);
        } catch (Exception e) {
            log.warn("[Admin] 删除前端用户失败 id={}: {}", id, e.getMessage());
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> toggleFrontendUserStatus(String id) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            var rows = jdbc.queryForList("SELECT status FROM frontend_user WHERE id = ?", id);
            if (rows.isEmpty()) {
                result.put("ok", false);
                result.put("error", "用户不存在");
                return result;
            }
            Object cur = rows.get(0).get("status");
            int next = (cur != null && Integer.parseInt(String.valueOf(cur)) == 1) ? 0 : 1;
            jdbc.update("UPDATE frontend_user SET status = ? WHERE id = ?", next, id);
            result.put("ok", true);
            result.put("status", next);
        } catch (Exception e) {
            log.warn("[Admin] 切换前端用户状态失败 id={}: {}", id, e.getMessage());
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return result;
    }
}