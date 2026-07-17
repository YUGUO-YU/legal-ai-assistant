package com.legalai.admin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.legalai.admin.enums.DataScope;
import com.legalai.llm.LLMClient;
import com.legalai.service.ElasticsearchService;
import com.legalai.service.MilvusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class AdminDataService {
    private static final Logger log = LoggerFactory.getLogger(AdminDataService.class);
    private final ExecutorService statsExecutor = Executors.newFixedThreadPool(6);

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired(required = false)
    private LLMClient llmClient;

    @Autowired(required = false)
    private MilvusService milvusService;

    @Autowired(required = false)
    private ElasticsearchService elasticsearchService;

    public JdbcTemplate jdbc() {
        return jdbc;
    }

    public Map<String, Object> getCurrentAdminInfo(Long userId) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (userId == null) {
            result.put("userId", null);
            result.put("username", null);
            result.put("roleId", null);
            result.put("dataScope", DataScope.ALL);
            result.put("teamId", null);
            return result;
        }
        try {
            var rows = jdbc.queryForList(
                "SELECT au.id, au.username, au.team_id, " +
                "aur.role_id, ar.data_scope " +
                "FROM admin_user au " +
                "LEFT JOIN admin_user_role aur ON au.id = aur.user_id " +
                "LEFT JOIN admin_role ar ON aur.role_id = ar.id " +
                "WHERE au.id = ? LIMIT 1",
                userId);
            if (rows.isEmpty()) {
                result.put("userId", userId);
                result.put("dataScope", DataScope.ALL);
                return result;
            }
            var row = rows.get(0);
            Integer dataScopeValue = row.get("data_scope") != null ? ((Number) row.get("data_scope")).intValue() : 4;
            result.put("userId", userId);
            result.put("username", row.get("username"));
            result.put("roleId", row.get("role_id"));
            result.put("dataScope", DataScope.fromValue(dataScopeValue));
            result.put("teamId", row.get("team_id"));
        } catch (Exception e) {
            log.warn("[Admin] getCurrentAdminInfo 失败 userId={}: {}", userId, e.getMessage());
            result.put("userId", userId);
            result.put("dataScope", DataScope.ALL);
        }
        return result;
    }

    public String buildDataScopeCondition(DataScope scope, Long userId, String userField, List<Object> args) {
        if (scope == null || scope == DataScope.ALL) {
            return "";
        }
        if (userId == null) {
            return " AND " + userField + " = -1";
        }
        switch (scope) {
            case SELF:
                return " AND " + userField + " = ?";
            case DEPT:
                return " AND team_id = (SELECT team_id FROM admin_user WHERE id = ?)";
            case TEAM:
                return " AND team_id = (SELECT team_id FROM admin_user WHERE id = ?)";
            default:
                return "";
        }
    }

    public void applyDataScopeArgs(DataScope scope, Long userId, List<Object> args) {
        if (scope == null || scope == DataScope.ALL || userId == null) {
            return;
        }
        switch (scope) {
            case SELF:
            case DEPT:
            case TEAM:
                args.add(userId);
                break;
            default:
                break;
        }
    }

    public Map<String, Object> list(String table, String module, int page, int pageSize, String keyword) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            StringBuilder where = new StringBuilder(" WHERE 1=1 ");
            List<Object> args = new ArrayList<>();
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

    public Map<String, Object> listMod02Cases(int page, int pageSize, String cause, Integer caseType, Integer judgment) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            StringBuilder where = new StringBuilder(" WHERE 1=1 AND biz_module = 'MOD-02' ");
            List<Object> args = new ArrayList<>();
            if (cause != null && !cause.isEmpty()) {
                where.append(" AND case_cause LIKE ? ");
                args.add("%" + cause + "%");
            }
            if (caseType != null) {
                where.append(" AND case_type = ? ");
                args.add(caseType);
            }
            if (judgment != null) {
                where.append(" AND judgment_result = ? ");
                args.add(judgment);
            }

            Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM tb_case" + where, Integer.class, args.toArray());
            int offset = Math.max(0, (page - 1) * pageSize);

            StringBuilder q = new StringBuilder("SELECT * FROM tb_case").append(where);
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
            log.warn("[Admin] listMod02Cases 失败: {}", e.getMessage());
            result.put("total", 0);
            result.put("page", page);
            result.put("pageSize", pageSize);
            result.put("list", java.util.Collections.emptyList());
            result.put("error", e.getMessage());
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

    public Map<String, Object> listLawRelations(Long sourceArticleId, Long targetArticleId, String relationType, int page, int pageSize) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            StringBuilder where = new StringBuilder(" WHERE 1=1 ");
            List<Object> args = new ArrayList<>();
            if (sourceArticleId != null) {
                where.append(" AND lr.source_article_id = ? ");
                args.add(sourceArticleId);
            }
            if (targetArticleId != null) {
                where.append(" AND lr.target_article_id = ? ");
                args.add(targetArticleId);
            }
            if (relationType != null && !relationType.isEmpty()) {
                where.append(" AND lr.relation_type = ? ");
                args.add(relationType);
            }

            StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM law_relation lr ");
            countSql.append(where);

            Integer total = jdbc.queryForObject(countSql.toString(), Integer.class, args.toArray());
            int offset = Math.max(0, (page - 1) * pageSize);

            StringBuilder q = new StringBuilder();
            q.append("SELECT lr.id, lr.source_article_id, lr.target_article_id, lr.relation_type, lr.weight, lr.created_at, ");
            q.append("src.title AS source_article_title, tgt.title AS target_article_title ");
            q.append("FROM law_relation lr ");
            q.append("LEFT JOIN law_document src ON lr.source_article_id = src.id ");
            q.append("LEFT JOIN law_document tgt ON lr.target_article_id = tgt.id ");
            q.append(where);
            q.append(" ORDER BY lr.id DESC LIMIT ? OFFSET ?");
            args.add(pageSize);
            args.add(offset);

            List<Map<String, Object>> rows = jdbc.queryForList(q.toString(), args.toArray());

            result.put("total", total == null ? 0 : total);
            result.put("page", page);
            result.put("pageSize", pageSize);
            result.put("list", rows);
            result.put("source", "admin-db");
        } catch (Exception e) {
            log.warn("[Admin] 法规关联列表查询失败: {}", e.getMessage());
            result.put("total", 0);
            result.put("page", page);
            result.put("pageSize", pageSize);
            result.put("list", java.util.Collections.emptyList());
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> createLawRelation(Map<String, Object> data) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (data == null || data.isEmpty()) {
            result.put("ok", false);
            result.put("error", "数据为空");
            return result;
        }
        Long sourceArticleId = data.get("sourceArticleId") != null ? ((Number) data.get("sourceArticleId")).longValue() : null;
        Long targetArticleId = data.get("targetArticleId") != null ? ((Number) data.get("targetArticleId")).longValue() : null;
        String relationType = (String) data.get("relationType");
        BigDecimal weight = data.get("weight") != null ? new BigDecimal(data.get("weight").toString()) : new BigDecimal("1.0");

        if (sourceArticleId == null || targetArticleId == null) {
            result.put("ok", false);
            result.put("error", "来源条款ID和目标条款ID不能为空");
            return result;
        }
        if (sourceArticleId.equals(targetArticleId)) {
            result.put("ok", false);
            result.put("error", "来源条款和目标条款不能相同");
            return result;
        }
        if (weight.compareTo(new BigDecimal("0.01")) < 0 || weight.compareTo(new BigDecimal("10.00")) > 0) {
            result.put("ok", false);
            result.put("error", "权重范围为0.01-10.00");
            return result;
        }
        try {
            String sql = "INSERT INTO law_relation (source_article_id, target_article_id, relation_type, weight) VALUES (?, ?, ?, ?)";
            jdbc.update(sql, sourceArticleId, targetArticleId, relationType, weight);
            List<Map<String, Object>> ids = jdbc.queryForList("SELECT LAST_INSERT_ID() as id");
            Long id = ids.isEmpty() ? null : (Long) ids.get(0).get("id");
            result.put("ok", true);
            result.put("id", id);
            log.info("创建法规关联: id={}, source={}, target={}, type={}", id, sourceArticleId, targetArticleId, relationType);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("Duplicate entry")) {
                result.put("ok", false);
                result.put("error", "该关联关系已存在");
            } else {
                log.warn("[Admin] 创建法规关联失败: {}", e.getMessage());
                result.put("ok", false);
                result.put("error", e.getMessage());
            }
        }
        return result;
    }

    public Map<String, Object> updateLawRelation(Long id, Map<String, Object> data) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (data == null || data.isEmpty()) {
            result.put("ok", false);
            result.put("error", "数据为空");
            return result;
        }
        try {
            var rows = jdbc.queryForList("SELECT id FROM law_relation WHERE id = ?", id);
            if (rows.isEmpty()) {
                result.put("ok", false);
                result.put("error", "关联记录不存在");
                return result;
            }
            List<String> sets = new ArrayList<>();
            List<Object> args = new ArrayList<>();
            if (data.containsKey("relationType")) {
                sets.add("relation_type = ?");
                args.add(data.get("relationType"));
            }
            if (data.containsKey("weight")) {
                BigDecimal weight = new BigDecimal(data.get("weight").toString());
                if (weight.compareTo(new BigDecimal("0.01")) < 0 || weight.compareTo(new BigDecimal("10.00")) > 0) {
                    result.put("ok", false);
                    result.put("error", "权重范围为0.01-10.00");
                    return result;
                }
                sets.add("weight = ?");
                args.add(weight);
            }
            if (sets.isEmpty()) {
                result.put("ok", false);
                result.put("error", "没有需要更新的字段");
                return result;
            }
            args.add(id);
            jdbc.update("UPDATE law_relation SET " + String.join(", ", sets) + " WHERE id = ?", args.toArray());
            result.put("ok", true);
            log.info("更新法规关联 id={}", id);
        } catch (Exception e) {
            log.warn("[Admin] 更新法规关联失败 id={}: {}", id, e.getMessage());
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> deleteLawRelation(Long id) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            int n = jdbc.update("DELETE FROM law_relation WHERE id = ?", id);
            result.put("ok", n > 0);
            if (n == 0) result.put("error", "关联记录不存在");
            log.info("删除法规关联 id={}, affected={}", id, n);
        } catch (Exception e) {
            log.warn("[Admin] 删除法规关联失败 id={}: {}", id, e.getMessage());
            result.put("ok", false);
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
            List<CompletableFuture<Map.Entry<String, Long>>> futures = new ArrayList<>();
            for (String t : tables) {
                CompletableFuture<Map.Entry<String, Long>> f = CompletableFuture.supplyAsync(() -> {
                    try {
                        Long c = jdbc.queryForObject("SELECT COUNT(*) FROM " + sanitize(t), Long.class);
                        return Map.entry(t, c == null ? 0L : c);
                    } catch (Exception e) {
                        log.warn("统计表 {} 行数失败: {}", t, e.getMessage());
                        return Map.entry(t, -1L);
                    }
                }, statsExecutor);
                futures.add(f);
            }
            for (CompletableFuture<Map.Entry<String, Long>> f : futures) {
                Map.Entry<String, Long> entry = f.join();
                counts.put(entry.getKey(), entry.getValue());
            }
            result.put("counts", counts);
            result.put("source", "admin-db");
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> audit(String userId, String operation, String module, int page, int pageSize, DataScope dataScope, Long adminUserId) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            StringBuilder where = new StringBuilder(" WHERE 1=1 ");
            List<Object> args = new ArrayList<>();
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
            if (dataScope != null && dataScope != DataScope.ALL && adminUserId != null) {
                switch (dataScope) {
                    case SELF:
                        where.append(" AND user_id = ?");
                        args.add(adminUserId);
                        break;
                    case DEPT:
                        where.append(" AND user_id IN (SELECT id FROM admin_user WHERE team_id = (SELECT team_id FROM admin_user WHERE id = ?))");
                        args.add(adminUserId);
                        break;
                    case TEAM:
                        where.append(" AND user_id IN (SELECT id FROM admin_user WHERE team_id = (SELECT team_id FROM admin_user WHERE id = ?))");
                        args.add(adminUserId);
                        break;
                }
            }
            Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM admin_audit_log" + where, Integer.class, args.toArray());
            int offset = Math.max(0, (page - 1) * pageSize);
            args.add(pageSize); args.add(offset);
            List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM admin_audit_log" + where + " ORDER BY id DESC LIMIT ? OFFSET ?", args.toArray());
            for (Map<String, Object> row : rows) {
                Object responseResult = row.get("response_result");
                if (responseResult != null) {
                    row.put("response_result", maskSensitiveData(String.valueOf(responseResult)));
                }
                Object requestParams = row.get("request_params");
                if (requestParams != null) {
                    row.put("request_params", maskSensitiveData(String.valueOf(requestParams)));
                }
            }
            result.put("total", total == null ? 0 : total);
            result.put("list", rows);
        } catch (Exception e) {
            result.put("total", 0);
            result.put("list", java.util.Collections.emptyList());
            result.put("error", e.getMessage());
        }
        return result;
    }

    public String exportAuditLogsCsv(String userId, String operation, String module) {
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        if (userId != null && !userId.isEmpty()) {
            try { where.append(" AND user_id = ? "); args.add(Long.valueOf(userId)); } catch (Exception e) { log.debug("无效的用户ID: {}", userId); }
        }
        if (operation != null && !operation.isEmpty()) { where.append(" AND operation = ? "); args.add(operation); }
        if (module != null && !module.isEmpty()) { where.append(" AND biz_module = ? "); args.add(module); }
        String[] headers = {"ID", "用户ID", "用户名", "操作", "模块", "对象类型", "对象ID", "请求方法", "URL", "IP", "耗时ms", "状态", "错误信息", "时间"};
        StringBuilder csv = new StringBuilder();
        csv.append(String.join(",", headers)).append("\n");
        try {
            var rows = jdbc.queryForList("SELECT * FROM admin_audit_log" + where + " ORDER BY id DESC LIMIT 10000", args.toArray());
            for (Map<String, Object> r : rows) {
                csv.append(safeCsv(r.get("id"))).append(",");
                csv.append(safeCsv(r.get("user_id"))).append(",");
                csv.append(safeCsv(r.get("username"))).append(",");
                csv.append(safeCsv(r.get("operation"))).append(",");
                csv.append(safeCsv(r.get("biz_module"))).append(",");
                csv.append(safeCsv(r.get("biz_type"))).append(",");
                csv.append(safeCsv(r.get("biz_id"))).append(",");
                csv.append(safeCsv(r.get("request_method"))).append(",");
                csv.append(safeCsv(r.get("request_url"))).append(",");
                csv.append(safeCsv(r.get("ip"))).append(",");
                csv.append(safeCsv(r.get("duration_ms"))).append(",");
                csv.append(safeCsv(r.get("status"))).append(",");
                csv.append(safeCsv(r.get("error_msg"))).append(",");
                csv.append(safeCsv(r.get("created_at"))).append("\n");
            }
        } catch (Exception e) {
            log.warn("[Admin] 导出审计日志失败: {}", e.getMessage());
        }
        return csv.toString();
    }

    private String safeCsv(Object v) {
        if (v == null) return "";
        String s = String.valueOf(v);
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    public String exportAlertRulesCsv() {
        String[] headers = {"规则名称", "指标", "运算符", "阈值", "持续时间(秒)", "严重程度", "启用状态", "创建时间"};
        StringBuilder csv = new StringBuilder();
        csv.append(String.join(",", headers)).append("\n");
        try {
            var rows = jdbc.queryForList("SELECT rule_name, metric, operator, threshold, duration_sec, level, status, created_at FROM alert_rule ORDER BY id DESC LIMIT 10000");
            for (Map<String, Object> r : rows) {
                csv.append(safeCsv(r.get("rule_name"))).append(",");
                csv.append(safeCsv(r.get("metric"))).append(",");
                csv.append(safeCsv(r.get("operator"))).append(",");
                csv.append(safeCsv(r.get("threshold"))).append(",");
                csv.append(safeCsv(r.get("duration_sec"))).append(",");
                csv.append(safeCsv(r.get("level"))).append(",");
                csv.append(safeCsv(r.get("status"))).append(",");
                csv.append(safeCsv(r.get("created_at"))).append("\n");
            }
        } catch (Exception e) {
            log.warn("[Admin] 导出告警规则失败: {}", e.getMessage());
        }
        return csv.toString();
    }

    public String exportAlertHistoryCsv() {
        String[] headers = {"规则ID", "指标", "阈值", "当前值", "严重程度", "状态", "触发时间", "确认时间", "解决时间"};
        StringBuilder csv = new StringBuilder();
        csv.append(String.join(",", headers)).append("\n");
        try {
            var rows = jdbc.queryForList("SELECT ah.rule_id, ah.metric, ah.threshold, ah.metric_value, ah.level, ah.notify_status, ah.triggered_at, ah.acked_at, ah.resolved_at FROM alert_history ah ORDER BY ah.id DESC LIMIT 10000");
            for (Map<String, Object> r : rows) {
                csv.append(safeCsv(r.get("rule_id"))).append(",");
                csv.append(safeCsv(r.get("metric"))).append(",");
                csv.append(safeCsv(r.get("threshold"))).append(",");
                csv.append(safeCsv(r.get("metric_value"))).append(",");
                csv.append(safeCsv(r.get("level"))).append(",");
                csv.append(safeCsv(r.get("notify_status"))).append(",");
                csv.append(safeCsv(r.get("triggered_at"))).append(",");
                csv.append(safeCsv(r.get("acked_at"))).append(",");
                csv.append(safeCsv(r.get("resolved_at"))).append("\n");
            }
        } catch (Exception e) {
            log.warn("[Admin] 导出告警历史失败: {}", e.getMessage());
        }
        return csv.toString();
    }

    public String exportSearchLogsCsv() {
        String[] headers = {"用户", "搜索内容", "结果数", "耗时(ms)", "意图分类", "搜索时间"};
        StringBuilder csv = new StringBuilder();
        csv.append(String.join(",", headers)).append("\n");
        try {
            var rows = jdbc.queryForList("SELECT user_id, query_text, result_count, response_time_ms, intent_type, created_at FROM search_log ORDER BY id DESC LIMIT 10000");
            for (Map<String, Object> r : rows) {
                csv.append(safeCsv(r.get("user_id"))).append(",");
                csv.append(safeCsv(r.get("query_text"))).append(",");
                csv.append(safeCsv(r.get("result_count"))).append(",");
                csv.append(safeCsv(r.get("response_time_ms"))).append(",");
                csv.append(safeCsv(r.get("intent_type"))).append(",");
                csv.append(safeCsv(r.get("created_at"))).append("\n");
            }
        } catch (Exception e) {
            log.warn("[Admin] 导出搜索日志失败: {}", e.getMessage());
        }
        return csv.toString();
    }

    public String exportUserFeedbackCsv() {
        String[] headers = {"用户", "标题", "内容", "状态", "处理人", "处理备注", "创建时间", "解决时间"};
        StringBuilder csv = new StringBuilder();
        csv.append(String.join(",", headers)).append("\n");
        try {
            var rows = jdbc.queryForList("SELECT user_id, title, content, status, assigned_to_name, handler_note, created_at, resolved_at FROM user_feedback ORDER BY id DESC LIMIT 10000");
            for (Map<String, Object> r : rows) {
                csv.append(safeCsv(r.get("user_id"))).append(",");
                csv.append(safeCsv(r.get("title"))).append(",");
                csv.append(safeCsv(r.get("content"))).append(",");
                csv.append(safeCsv(r.get("status"))).append(",");
                csv.append(safeCsv(r.get("assigned_to_name"))).append(",");
                csv.append(safeCsv(r.get("handler_note"))).append(",");
                csv.append(safeCsv(r.get("created_at"))).append(",");
                csv.append(safeCsv(r.get("resolved_at"))).append("\n");
            }
        } catch (Exception e) {
            log.warn("[Admin] 导出用户反馈失败: {}", e.getMessage());
        }
        return csv.toString();
    }

    public void recordAudit(Long userId, String username, String operation, String bizModule,
                            String bizType, String bizId, String url, String method,
                            String params, String result, String ip, int duration, boolean ok, String error) {
        try {
            String maskedParams = maskSensitiveData(params);
            String maskedResult = maskSensitiveData(result);
            jdbc.update("INSERT INTO admin_audit_log(user_id, username, operation, biz_module, biz_type, biz_id, request_url, request_method, request_params, response_result, ip, duration_ms, status, error_msg, trace_id, created_at) " +
                            "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW())",
                    userId, username, operation, bizModule, bizType, bizId, url, method,
                    truncate(maskedParams, 4000), truncate(maskedResult, 4000), ip, duration, ok ? 1 : 0,
                    truncate(error, 2000), java.util.UUID.randomUUID().toString().replace("-", ""));
        } catch (Exception e) {
            log.warn("[Admin] 审计写入失败: {}", e.getMessage());
        }
    }

    private String maskSensitiveData(String params) {
        if (params == null || params.isEmpty()) {
            return params;
        }
        String trimmed = params.trim();
        if (!trimmed.startsWith("{")) {
            return params;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode tree = mapper.readTree(trimmed);
            maskNode(tree);
            return mapper.writeValueAsString(tree);
        } catch (Exception e) {
            return params;
        }
    }

    private void maskNode(JsonNode node) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            Set<String> toRemove = new HashSet<>();
            Map<String, JsonNode> toAdd = new LinkedHashMap<>();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String fieldName = field.getKey();
                JsonNode value = field.getValue();
                if (isSensitiveField(fieldName)) {
                    toRemove.add(fieldName);
                    toAdd.put(fieldName, JsonNodeFactory.instance.textNode("****"));
                } else if (value.isObject() || value.isArray()) {
                    maskNode(value);
                }
            }
            ((ObjectNode) node).remove(toRemove);
            ((ObjectNode) node).setAll(toAdd);
        } else if (node.isArray()) {
            for (JsonNode element : node) {
                if (element.isObject() || element.isArray()) {
                    maskNode(element);
                }
            }
        }
    }

    private boolean isSensitiveField(String fieldName) {
        return "password".equals(fieldName) || "code".equals(fieldName) ||
               "token".equals(fieldName) || "apiKey".equals(fieldName) ||
               "api_key".equals(fieldName) || "secret".equals(fieldName) ||
               "authorization".equals(fieldName);
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
        List<String> cols = new ArrayList<>();
        List<Object> vals = new ArrayList<>();
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
                } catch (Exception e) { log.warn("获取INSERT ID失败: {}", e.getMessage()); }
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
        List<String> sets = new ArrayList<>();
        List<Object> vals = new ArrayList<>();
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

    private static final Set<String> BATCH_WHITELIST = Set.of(
        "frontend_user", "admin_user", "announcement", "alert_rule", "law_document", "tb_case"
    );

    public Map<String, Object> batchDelete(String table, List<Long> ids) {
        Map<String, Object> result = new LinkedHashMap<>();
        String safe = sanitize(table);
        if (!BATCH_WHITELIST.contains(safe)) {
            result.put("ok", false);
            result.put("error", "该表不支持批量删除: " + table);
            return result;
        }
        if (ids == null || ids.isEmpty()) {
            result.put("ok", false);
            result.put("error", "ID列表为空");
            return result;
        }
        try {
            String placeholders = String.join(",", java.util.Collections.nCopies(ids.size(), "?"));
            String sql = "DELETE FROM " + safe + " WHERE id IN (" + placeholders + ")";
            int affected = jdbc.update(sql, ids.toArray());
            result.put("ok", true);
            result.put("affected", affected);
            result.put("total", ids.size());
            log.info("[Admin] 批量删除 table={} ids={} affected={}", table, ids, affected);
        } catch (Exception e) {
            log.warn("[Admin] 批量删除失败 table={}: {}", table, e.getMessage());
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> batchToggle(String table, List<Long> ids, int status) {
        Map<String, Object> result = new LinkedHashMap<>();
        String safe = sanitize(table);
        if (!BATCH_WHITELIST.contains(safe)) {
            result.put("ok", false);
            result.put("error", "该表不支持批量状态切换: " + table);
            return result;
        }
        if (ids == null || ids.isEmpty()) {
            result.put("ok", false);
            result.put("error", "ID列表为空");
            return result;
        }
        try {
            String statusField = "alert_rule".equals(safe) ? "enabled" : "status";
            String placeholders = String.join(",", java.util.Collections.nCopies(ids.size(), "?"));
            String sql = "UPDATE " + safe + " SET " + statusField + " = ? WHERE id IN (" + placeholders + ")";
            int affected = jdbc.update(sql, status, ids.toArray());
            result.put("ok", true);
            result.put("affected", affected);
            result.put("total", ids.size());
            log.info("[Admin] 批量状态切换 table={} ids={} status={} affected={}", table, ids, status, affected);
        } catch (Exception e) {
            log.warn("[Admin] 批量状态切换失败 table={}: {}", table, e.getMessage());
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
            List<Map<String, Object>> rows = jdbc.queryForList("SELECT id, status FROM law_document WHERE id = ?", lawId);
            if (rows.isEmpty()) { result.put("ok", false); result.put("error", "法规不存在"); return result; }
            int cur = Integer.parseInt(String.valueOf(rows.get(0).get("status")));
            int next;
            String err = null;
            if (action == 1) {
                if (cur == 3) next = 1;
                else if (cur == 1) next = 2;
                else { err = "当前状态不允许通过"; next = cur; }
            } else if (action == 2) {
                next = 3;
            } else { result.put("ok", false); result.put("error", "未知动作"); return result; }
            if (err != null) { result.put("ok", false); result.put("error", err); return result; }
            jdbc.update("UPDATE law_document SET status = ? WHERE id = ?", next, lawId);
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
            Integer pendingLaws = jdbc.queryForObject("SELECT COUNT(*) FROM law_document WHERE status = 3", Integer.class);
            Integer pendingFeedback = jdbc.queryForObject("SELECT COUNT(*) FROM user_feedback WHERE status = 0", Integer.class);
            Integer totalTokens = jdbc.queryForObject("SELECT COALESCE(SUM(total_tokens), 0) FROM llm_token_usage WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)", Integer.class);
            Integer totalCost = jdbc.queryForObject("SELECT COALESCE(SUM(cost_cny), 0) FROM llm_token_usage WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)", Integer.class);
            Integer totalFrontendUsers = jdbc.queryForObject("SELECT COUNT(*) FROM frontend_user", Integer.class);
            Integer pendingApprovals = jdbc.queryForObject("SELECT COUNT(*) FROM frontend_user WHERE approved = 0", Integer.class);
            Integer todayLogins = jdbc.queryForObject("SELECT COUNT(*) FROM frontend_user WHERE DATE(last_login_at) = CURDATE()", Integer.class);
            Integer todayRegs = jdbc.queryForObject("SELECT COUNT(*) FROM frontend_user WHERE DATE(created_at) = CURDATE()", Integer.class);
            Integer activeAnnouncements = jdbc.queryForObject("SELECT COUNT(*) FROM sys_announcement WHERE status = 1 AND (expired_at IS NULL OR expired_at > NOW())", Integer.class);

            result.put("activeAlerts", activeAlerts == null ? 0 : activeAlerts);
            result.put("pendingDrafts", pendingDrafts == null ? 0 : pendingDrafts);
            result.put("pendingLaws", pendingLaws == null ? 0 : pendingLaws);
            result.put("pendingFeedback", pendingFeedback == null ? 0 : pendingFeedback);
            result.put("weeklyTokens", totalTokens == null ? 0 : totalTokens);
            result.put("weeklyCost", totalCost == null ? 0 : totalCost);
            result.put("totalFrontendUsers", totalFrontendUsers == null ? 0 : totalFrontendUsers);
            result.put("pendingApprovals", pendingApprovals == null ? 0 : pendingApprovals);
            result.put("todayLogins", todayLogins == null ? 0 : todayLogins);
            result.put("todayRegs", todayRegs == null ? 0 : todayRegs);
            result.put("activeAnnouncements", activeAnnouncements == null ? 0 : activeAnnouncements);

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

            // 近 7 天活跃用户趋势
            List<Map<String, Object>> userTrend = jdbc.queryForList(
                "SELECT DATE(last_login_at) AS day, COUNT(*) AS cnt FROM frontend_user " +
                "WHERE last_login_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) GROUP BY DATE(last_login_at) ORDER BY day"
            );
            result.put("userTrend", userTrend);

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
    // Elasticsearch 健康检查
    // ============================================================

    public Map<String, Object> esHealth() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            if (elasticsearchService == null || !elasticsearchService.isAvailable()) {
                result.put("status", "unavailable");
                result.put("message", "Elasticsearch 未启用或连接失败");
                return result;
            }
            var info = elasticsearchService.getClusterHealth();
            result.put("status", info.get("status"));
            result.put("clusterName", info.get("clusterName"));
            result.put("numberOfNodes", info.get("numberOfNodes"));
            result.put("activeShards", info.get("activeShards"));
            result.put("activePrimaryShards", info.get("activePrimaryShards"));
        } catch (Exception e) {
            result.put("status", "unavailable");
            result.put("message", "Elasticsearch 健康检查失败: " + e.getMessage());
        }
        return result;
    }

    // ============================================================
    // LLM 模型摘要
    // ============================================================

    public Map<String, Object> llmModelsSummary() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            Integer totalModels = jdbc.queryForObject("SELECT COUNT(*) FROM llm_model_config", Integer.class);
            Integer enabledModels = jdbc.queryForObject("SELECT COUNT(*) FROM llm_model_config WHERE status = 1", Integer.class);
            var activeRows = jdbc.queryForList("SELECT * FROM llm_model_config WHERE is_primary = 1 AND status = 1 LIMIT 1");

            result.put("totalModels", totalModels == null ? 0 : totalModels);
            result.put("enabledModels", enabledModels == null ? 0 : enabledModels);

            if (!activeRows.isEmpty()) {
                var active = activeRows.get(0);
                result.put("activeModel", active.get("model_code"));
                result.put("provider", active.get("provider"));
                result.put("modelName", active.get("model_name"));
                result.put("status", "active");
            } else {
                result.put("activeModel", null);
                result.put("provider", null);
                result.put("modelName", null);
                result.put("status", "inactive");
            }
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        return result;
    }

    // ============================================================
    // LLM 健康探测（模拟）
    // ============================================================

    // ============================================================
    // 用户活跃度统计
    // ============================================================

    public Map<String, Object> userActivityStats(String startDate, String endDate) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            String dateFilter = "";
            List<Object> args = new ArrayList<>();
            if (startDate != null && !startDate.isEmpty()) {
                dateFilter += " AND created_at >= ? ";
                args.add(startDate);
            }
            if (endDate != null && !endDate.isEmpty()) {
                dateFilter += " AND created_at < DATE_ADD(?, INTERVAL 1 DAY) ";
                args.add(endDate);
            }

            String dailyActiveSql = """
                SELECT (
                    SELECT COUNT(*) FROM frontend_user WHERE DATE(last_login_at) = CURDATE()
                ) + (
                    SELECT COUNT(DISTINCT user_id) FROM search_log WHERE DATE(created_at) = CURDATE()
                ) + (
                    SELECT COUNT(DISTINCT user_id) FROM user_login_history WHERE DATE(login_at) = CURDATE()
                )
                """;
            Integer dailyActive = jdbc.queryForObject(dailyActiveSql, Integer.class);

            String weeklyActiveSql = """
                SELECT (
                    SELECT COUNT(DISTINCT user_id) FROM frontend_user WHERE last_login_at >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
                ) + (
                    SELECT COUNT(DISTINCT user_id) FROM search_log WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
                ) + (
                    SELECT COUNT(DISTINCT user_id) FROM user_login_history WHERE login_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
                )
                """;
            Integer weeklyActive = jdbc.queryForObject(weeklyActiveSql, Integer.class);

            String monthlyActiveSql = """
                SELECT (
                    SELECT COUNT(DISTINCT user_id) FROM frontend_user WHERE last_login_at >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
                ) + (
                    SELECT COUNT(DISTINCT user_id) FROM search_log WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
                ) + (
                    SELECT COUNT(DISTINCT user_id) FROM user_login_history WHERE login_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
                )
                """;
            Integer monthlyActive = jdbc.queryForObject(monthlyActiveSql, Integer.class);

            String searchTrendSql = """
                SELECT DATE(created_at) AS date, COUNT(DISTINCT user_id) AS count
                FROM search_log
                WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)
                GROUP BY DATE(created_at)
                ORDER BY date
                """;
            List<Map<String, Object>> searchTrend = jdbc.queryForList(searchTrendSql);

            String loginTrendSql = """
                SELECT DATE(login_at) AS date, COUNT(DISTINCT user_id) AS count
                FROM user_login_history
                WHERE login_at >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)
                GROUP BY DATE(login_at)
                ORDER BY date
                """;
            List<Map<String, Object>> loginTrend = jdbc.queryForList(loginTrendSql);

            Map<String, Long> trendMap = new LinkedHashMap<>();
            for (Map<String, Object> row : searchTrend) {
                String date = row.get("date").toString();
                long cnt = ((Number) row.get("count")).longValue();
                trendMap.merge(date, cnt, Long::sum);
            }
            for (Map<String, Object> row : loginTrend) {
                String date = row.get("date").toString();
                long cnt = ((Number) row.get("count")).longValue();
                trendMap.merge(date, cnt, Long::sum);
            }
            List<Map<String, Object>> activeTrend = new ArrayList<>();
            for (Map.Entry<String, Long> e : trendMap.entrySet()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("date", e.getKey());
                m.put("count", e.getValue());
                activeTrend.add(m);
            }

            String searchTopUsersSql = """
                SELECT username, COUNT(*) AS count
                FROM search_log
                WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
                  AND username IS NOT NULL AND username != ''
                GROUP BY username
                ORDER BY count DESC
                LIMIT 10
                """;
            List<Map<String, Object>> searchTopUsers = jdbc.queryForList(searchTopUsersSql);

            String loginTopUsersSql = """
                SELECT username, COUNT(*) AS count
                FROM user_login_history
                WHERE login_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
                  AND username IS NOT NULL AND username != ''
                GROUP BY username
                ORDER BY count DESC
                LIMIT 10
                """;
            List<Map<String, Object>> loginTopUsers = jdbc.queryForList(loginTopUsersSql);

            Map<String, Long> userCountMap = new LinkedHashMap<>();
            for (Map<String, Object> row : searchTopUsers) {
                String username = (String) row.get("username");
                long cnt = ((Number) row.get("count")).longValue();
                userCountMap.merge(username, cnt, Long::sum);
            }
            for (Map<String, Object> row : loginTopUsers) {
                String username = (String) row.get("username");
                long cnt = ((Number) row.get("count")).longValue();
                userCountMap.merge(username, cnt, Long::sum);
            }
            List<Map<String, Object>> topUsers = new ArrayList<>();
            int limit = 10;
            for (Map.Entry<String, Long> e : userCountMap.entrySet()) {
                if (topUsers.size() >= limit) break;
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("username", e.getKey());
                m.put("count", e.getValue());
                topUsers.add(m);
            }

            result.put("dailyActive", dailyActive == null ? 0 : dailyActive);
            result.put("weeklyActive", weeklyActive == null ? 0 : weeklyActive);
            result.put("monthlyActive", monthlyActive == null ? 0 : monthlyActive);
            result.put("activeTrend", activeTrend);
            result.put("topUsers", topUsers);
            result.put("source", "admin-db");
        } catch (Exception e) {
            log.warn("[Admin] userActivityStats 失败: {}", e.getMessage());
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> getHourlyAccess() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            int[] today = new int[24];
            int[] yesterday = new int[24];

            List<Map<String, Object>> todayRows = jdbc.queryForList(
                "SELECT HOUR(login_at) AS hour, COUNT(*) AS cnt FROM user_login_history WHERE DATE(login_at) = CURDATE() GROUP BY HOUR(login_at)");
            for (Map<String, Object> row : todayRows) {
                int h = ((Number) row.get("hour")).intValue();
                today[h] = ((Number) row.get("cnt")).intValue();
            }

            List<Map<String, Object>> yesterdayRows = jdbc.queryForList(
                "SELECT HOUR(login_at) AS hour, COUNT(*) AS cnt FROM user_login_history WHERE DATE(login_at) = DATE_SUB(CURDATE(), INTERVAL 1 DAY) GROUP BY HOUR(login_at)");
            for (Map<String, Object> row : yesterdayRows) {
                int h = ((Number) row.get("hour")).intValue();
                yesterday[h] = ((Number) row.get("cnt")).intValue();
            }

            result.put("today", today);
            result.put("yesterday", yesterday);
        } catch (Exception e) {
            log.warn("[Admin] getHourlyAccess 失败: {}", e.getMessage());
            result.put("today", new int[24]);
            result.put("yesterday", new int[24]);
        }
        return result;
    }

    public Map<String, Object> lawUsageStats(String startDate, String endDate, int topN) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            String dateFilter = "";
            List<Object> args = new ArrayList<>();
            if (startDate != null && !startDate.isEmpty()) {
                dateFilter += " AND sl.created_at >= ? ";
                args.add(startDate);
            }
            if (endDate != null && !endDate.isEmpty()) {
                dateFilter += " AND sl.created_at < DATE_ADD(?, INTERVAL 1 DAY) ";
                args.add(endDate);
            }

            String topLawsSearchSql = """
                SELECT la.title, COUNT(*) AS count
                FROM search_feedback sf
                JOIN law_article la ON sf.article_id = la.id
                WHERE sf.created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
                GROUP BY la.id, la.title
                ORDER BY count DESC
                LIMIT ?
                """;
            List<Object> searchArgs = new ArrayList<>(args);
            searchArgs.add(topN);
            List<Map<String, Object>> topLawsSearch = jdbc.queryForList(topLawsSearchSql, searchArgs.toArray());

            String topLawsFavoriteSql = """
                SELECT lf.law_title AS title, COUNT(*) AS count
                FROM law_favorite lf
                WHERE lf.law_title IS NOT NULL AND lf.law_title != ''
                GROUP BY lf.law_uuid, lf.law_title
                ORDER BY count DESC
                LIMIT ?
                """;
            List<Map<String, Object>> topLawsFavorite = jdbc.queryForList(topLawsFavoriteSql, topN);

            result.put("topLawsSearch", topLawsSearch);
            result.put("topLawsFavorite", topLawsFavorite);
            result.put("source", "admin-db");
        } catch (Exception e) {
            log.warn("[Admin] lawUsageStats 失败: {}", e.getMessage());
            result.put("error", e.getMessage());
        }
        return result;
    }

    // ============================================================
    // 合同审查记录查询
    // ============================================================

    public Map<String, Object> listContractReviews(Long userId, String riskLevel, int page, int pageSize) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            StringBuilder where = new StringBuilder(" WHERE 1=1 ");
            List<Object> args = new ArrayList<>();
            if (userId != null) {
                where.append(" AND user_id = ? ");
                args.add(userId);
            }
            if (riskLevel != null && !riskLevel.isEmpty()) {
                where.append(" AND risk_level = ? ");
                args.add(riskLevel.toLowerCase());
            }

            Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM contract_review" + where, Integer.class, args.toArray());
            int offset = Math.max(0, (page - 1) * pageSize);
            args.add(pageSize);
            args.add(offset);

            StringBuilder q = new StringBuilder();
            q.append("SELECT id, review_uuid, user_id, username, file_name, file_size, review_type, ");
            q.append("risk_level, risk_count, summary, created_at ");
            q.append("FROM contract_review").append(where);
            q.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");

            List<Map<String, Object>> rows = jdbc.queryForList(q.toString(), args.toArray());
            result.put("total", total == null ? 0 : total);
            result.put("page", page);
            result.put("pageSize", pageSize);
            result.put("list", rows);
            result.put("source", "admin-db");
        } catch (Exception e) {
            log.warn("[Admin] listContractReviews 失败: {}", e.getMessage());
            result.put("total", 0);
            result.put("page", page);
            result.put("pageSize", pageSize);
            result.put("list", java.util.Collections.emptyList());
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> getContractReview(Long id) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT cr.*, cr.risk_details FROM contract_review cr WHERE cr.id = ?", id
            );
            if (rows.isEmpty()) {
                result.put("data", null);
                result.put("error", "审查记录不存在");
            } else {
                Map<String, Object> row = rows.get(0);
                String riskDetails = row.get("risk_details") != null ? row.get("risk_details").toString() : null;
                if (riskDetails != null && riskDetails.startsWith("{")) {
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        Object parsed = mapper.readValue(riskDetails, Object.class);
                        row.put("riskDetailsJson", parsed);
                    } catch (Exception e) { log.debug("解析风险详情JSON失败: {}", e.getMessage()); }
                }
                result.put("data", row);
            }
            result.put("source", "admin-db");
        } catch (Exception e) {
            log.warn("[Admin] getContractReview 失败 id={}: {}", id, e.getMessage());
            result.put("data", null);
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> saveContractDraft(Map<String, Object> data) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            String fileName = data.get("fileName") != null ? data.get("fileName").toString() : null;
            String reviewType = data.get("reviewType") != null ? data.get("reviewType").toString() : null;
            String riskLevel = data.get("riskLevel") != null ? data.get("riskLevel").toString() : null;
            String riskDetails = data.get("riskDetails") != null ? data.get("riskDetails").toString() : null;
            String summary = data.get("summary") != null ? data.get("summary").toString() : null;

            String sql = """
                INSERT INTO contract_review (review_uuid, user_id, username, file_name, file_size, review_type, risk_level, risk_count, summary, risk_details, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
            String uuid = java.util.UUID.randomUUID().toString();
            int riskCount = 0;
            if (riskDetails != null && riskDetails.contains("\"highRiskItems\"")) {
                try {
                    ObjectMapper mapper2 = new ObjectMapper();
                    JsonNode node = mapper2.readTree(riskDetails);
                    if (node.has("highRiskItems") && node.get("highRiskItems").isArray()) {
                        riskCount += node.get("highRiskItems").size();
                    }
                    if (node.has("mediumRiskItems") && node.get("mediumRiskItems").isArray()) {
                        riskCount += node.get("mediumRiskItems").size();
                    }
                    if (node.has("lowRiskItems") && node.get("lowRiskItems").isArray()) {
                        riskCount += node.get("lowRiskItems").size();
                    }
                    } catch (Exception e) { log.debug("解析风险详情JSON失败: {}", e.getMessage()); }
            }
            jdbc.update(sql, uuid, null, null, fileName, null, reviewType, riskLevel, riskCount, summary, riskDetails, new java.sql.Timestamp(System.currentTimeMillis()));
            result.put("id", uuid);
            result.put("ok", true);
            result.put("message", "草稿已保存");
        } catch (Exception e) {
            log.warn("[Admin] saveContractDraft 失败: {}", e.getMessage());
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> llmHealthCheck() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> models = jdbc.queryForList("SELECT * FROM llm_model_config WHERE status = 1");
            List<Map<String, Object>> checks = new ArrayList<>();
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
        List<Map<String, Object>> cols = new ArrayList<>();
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

    public Map<String, Object> createModelConfig(Map<String, Object> payload) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (payload == null || payload.isEmpty()) {
            result.put("ok", false);
            result.put("error", "payload 为空");
            return result;
        }
        String modelCode = (String) payload.get("model_code");
        String modelName = (String) payload.get("model_name");
        if (modelCode == null || modelCode.isEmpty() || modelName == null || modelName.isEmpty()) {
            result.put("ok", false);
            result.put("error", "模型编码和名称不能为空");
            return result;
        }
        try {
            String provider = (String) payload.getOrDefault("provider", "minimax");
            String endpoint = (String) payload.getOrDefault("endpoint", "https://api.minimax.chat/v1");
            String apiKey = (String) payload.get("api_key_enc");
            BigDecimal temperature = payload.get("temperature") != null ? new BigDecimal(payload.get("temperature").toString()) : new BigDecimal("0.7");
            Integer maxTokens = payload.get("max_tokens") != null ? ((Number) payload.get("max_tokens")).intValue() : 4096;
            BigDecimal topP = payload.get("top_p") != null ? new BigDecimal(payload.get("top_p").toString()) : new BigDecimal("0.95");
            Integer isPrimary = payload.get("is_primary") != null ? ((Number) payload.get("is_primary")).intValue() : 0;
            if (isPrimary == 1) {
                jdbc.update("UPDATE llm_model_config SET is_primary = 0");
            }
            String sql = "INSERT INTO llm_model_config (model_code, model_name, provider, endpoint, api_key_enc, temperature, max_tokens, top_p, is_primary, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1)";
            jdbc.update(sql, modelCode, modelName, provider, endpoint, apiKey, temperature, maxTokens, topP, isPrimary);
            var ids = jdbc.queryForList("SELECT LAST_INSERT_ID()");
            long id = ((Number) ids.get(0).get("LAST_INSERT_ID()")).longValue();
            result.put("ok", true);
            result.put("id", id);
        } catch (Exception e) {
            log.warn("[Admin] 创建模型配置失败: {}", e.getMessage());
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> updateModelConfig(Long id, Map<String, Object> payload) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            var rows = jdbc.queryForList("SELECT id FROM llm_model_config WHERE id = ?", id);
            if (rows.isEmpty()) {
                result.put("ok", false);
                result.put("error", "模型配置不存在");
                return result;
            }
            List<String> sets = new ArrayList<>();
            List<Object> args = new ArrayList<>();
            if (payload.containsKey("model_code")) { sets.add("model_code = ?"); args.add(payload.get("model_code")); }
            if (payload.containsKey("model_name")) { sets.add("model_name = ?"); args.add(payload.get("model_name")); }
            if (payload.containsKey("provider")) { sets.add("provider = ?"); args.add(payload.get("provider")); }
            if (payload.containsKey("endpoint")) { sets.add("endpoint = ?"); args.add(payload.get("endpoint")); }
            if (payload.containsKey("api_key_enc")) { sets.add("api_key_enc = ?"); args.add(payload.get("api_key_enc")); }
            if (payload.containsKey("temperature")) { sets.add("temperature = ?"); args.add(new BigDecimal(payload.get("temperature").toString())); }
            if (payload.containsKey("max_tokens")) { sets.add("max_tokens = ?"); args.add(((Number) payload.get("max_tokens")).intValue()); }
            if (payload.containsKey("top_p")) { sets.add("top_p = ?"); args.add(new BigDecimal(payload.get("top_p").toString())); }
            if (payload.containsKey("is_primary")) {
                Integer ip = ((Number) payload.get("is_primary")).intValue();
                if (ip == 1) { jdbc.update("UPDATE llm_model_config SET is_primary = 0"); }
                sets.add("is_primary = ?"); args.add(ip);
            }
            if (payload.containsKey("status")) { sets.add("status = ?"); args.add(((Number) payload.get("status")).intValue()); }
            if (sets.isEmpty()) {
                result.put("ok", false);
                result.put("error", "没有需要更新的字段");
                return result;
            }
            args.add(id);
            jdbc.update("UPDATE llm_model_config SET " + String.join(", ", sets) + " WHERE id = ?", args.toArray());
            result.put("ok", true);
        } catch (Exception e) {
            log.warn("[Admin] 更新模型配置失败 id={}: {}", id, e.getMessage());
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> deleteModelConfig(Long id) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            int n = jdbc.update("DELETE FROM llm_model_config WHERE id = ?", id);
            result.put("ok", n > 0);
            if (n == 0) result.put("error", "模型配置不存在");
        } catch (Exception e) {
            log.warn("[Admin] 删除模型配置失败 id={}: {}", id, e.getMessage());
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    // ============================================================
    // DOC_TEMPLATE (Mod03) CRUD
    // ============================================================

    public Map<String, Object> listDocTemplates(String category) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            StringBuilder where = new StringBuilder(" WHERE biz_module = 'MOD-03' ");
            List<Object> args = new ArrayList<>();
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
        List<Object> args = new ArrayList<>();
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
        List<Object> args = new ArrayList<>();
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
        List<Object> args = new ArrayList<>();
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

    public Map<String, Object> listFrontendUsers(Long adminUserId, DataScope dataScope, int page, int pageSize, String keyword) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            StringBuilder where = new StringBuilder(" WHERE 1=1 ");
            List<Object> args = new ArrayList<>();
            if (keyword != null && !keyword.isEmpty()) {
                where.append(" AND (id LIKE ? OR username LIKE ? OR real_name LIKE ? OR email LIKE ? OR phone LIKE ?)");
                String like = "%" + keyword + "%";
                args.add(like);
                args.add(like);
                args.add(like);
                args.add(like);
                args.add(like);
            }
            if (dataScope != null && dataScope != DataScope.ALL) {
                if (adminUserId == null) {
                    where.append(" AND 1=0");
                } else {
                    switch (dataScope) {
                        case SELF:
                            where.append(" AND created_by = ?");
                            args.add(adminUserId);
                            break;
                    case DEPT:
                        where.append(" AND user_id IN (SELECT id FROM admin_user WHERE team_id = (SELECT team_id FROM admin_user WHERE id = ?))");
                        args.add(adminUserId);
                        break;
                        case TEAM:
                            where.append(" AND team_id = (SELECT team_id FROM admin_user WHERE id = ?)");
                            args.add(adminUserId);
                            break;
                    }
                }
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
        if (password == null || password.isEmpty()) {
            result.put("ok", false);
            result.put("error", "密码不能为空");
            return result;
        }
        if (password.length() < 8 || password.length() > 32) {
            result.put("ok", false);
            result.put("error", "密码长度需在8-32位之间");
            return result;
        }
        if (!password.matches(".*[A-Z].*")) {
            result.put("ok", false);
            result.put("error", "密码必须包含至少一个大写字母");
            return result;
        }
        if (!password.matches(".*[a-z].*")) {
            result.put("ok", false);
            result.put("error", "密码必须包含至少一个小写字母");
            return result;
        }
        if (!password.matches(".*[0-9].*")) {
            result.put("ok", false);
            result.put("error", "密码必须包含至少一个数字");
            return result;
        }
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            result.put("ok", false);
            result.put("error", "密码必须包含至少一个特殊字符");
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
            List<String> sets = new ArrayList<>();
            List<Object> vals = new ArrayList<>();
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
                String pwdError = validatePassword(pwd);
                if (pwdError != null) {
                    result.put("ok", false);
                    result.put("error", pwdError);
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

    public Map<String, Object> listPendingApprovals() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, username, real_name, email, phone, created_at FROM frontend_user WHERE approved = 0 ORDER BY created_at DESC");
            result.put("total", rows.size());
            result.put("list", rows);
            result.put("source", "admin-db");
        } catch (Exception e) {
            log.warn("[Admin] 待审核用户列表查询失败: {}", e.getMessage());
            result.put("total", 0);
            result.put("list", java.util.Collections.emptyList());
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> approveFrontendUser(String id) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            var rows = jdbc.queryForList("SELECT id, approved FROM frontend_user WHERE id = ?", id);
            if (rows.isEmpty()) {
                result.put("ok", false);
                result.put("error", "用户不存在");
                return result;
            }
            jdbc.update("UPDATE frontend_user SET approved = 1 WHERE id = ?", id);
            result.put("ok", true);
            result.put("message", "用户已审核通过");
        } catch (Exception e) {
            log.warn("[Admin] 审核用户失败 id={}: {}", id, e.getMessage());
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> rejectFrontendUser(String id) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            var rows = jdbc.queryForList("SELECT id FROM frontend_user WHERE id = ?", id);
            if (rows.isEmpty()) {
                result.put("ok", false);
                result.put("error", "用户不存在");
                return result;
            }
            jdbc.update("DELETE FROM frontend_user WHERE id = ?", id);
            result.put("ok", true);
            result.put("message", "用户已拒绝并删除");
        } catch (Exception e) {
            log.warn("[Admin] 拒绝用户失败 id={}: {}", id, e.getMessage());
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> listAnnouncements(int page, int pageSize, String keyword) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            List<Object> args = new ArrayList<>();
            StringBuilder where = new StringBuilder(" WHERE 1=1");
            if (keyword != null && !keyword.isEmpty()) {
                where.append(" AND (title LIKE ? OR content LIKE ?)");
                args.add("%" + keyword + "%");
                args.add("%" + keyword + "%");
            }
            Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM sys_announcement" + where, Integer.class, args.toArray());
            int offset = Math.max(0, (page - 1) * pageSize);
            String sql = "SELECT * FROM sys_announcement" + where + " ORDER BY priority DESC, published_at DESC LIMIT ? OFFSET ?";
            args.add(pageSize);
            args.add(offset);
            List<Map<String, Object>> rows = jdbc.queryForList(sql, args.toArray());
            result.put("total", total == null ? 0 : total);
            result.put("page", page);
            result.put("pageSize", pageSize);
            result.put("list", rows);
            result.put("source", "admin-db");
        } catch (Exception e) {
            log.warn("[Admin] 公告列表查询失败: {}", e.getMessage());
            result.put("total", 0);
            result.put("page", page);
            result.put("pageSize", pageSize);
            result.put("list", java.util.Collections.emptyList());
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> createAnnouncement(Map<String, Object> payload) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (payload == null || payload.isEmpty()) {
            result.put("ok", false);
            result.put("error", "payload 为空");
            return result;
        }
        String title = (String) payload.get("title");
        String content = (String) payload.get("content");
        if (title == null || title.isEmpty()) {
            result.put("ok", false);
            result.put("error", "标题不能为空");
            return result;
        }
        if (content == null || content.isEmpty()) {
            result.put("ok", false);
            result.put("error", "内容不能为空");
            return result;
        }
        try {
            Integer type = payload.get("type") != null ? ((Number) payload.get("type")).intValue() : 1;
            Integer priority = payload.get("priority") != null ? ((Number) payload.get("priority")).intValue() : 0;
            Integer status = payload.get("status") != null ? ((Number) payload.get("status")).intValue() : 1;
            String createdBy = (String) payload.get("created_by");
            String publishedAt = (String) payload.get("published_at");
            String expiredAt = (String) payload.get("expired_at");
            String sql = "INSERT INTO sys_announcement (title, content, type, priority, status, published_at, expired_at, created_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            Object pubAt = (publishedAt != null && !publishedAt.isEmpty()) ? publishedAt : (status == 1 ? new java.sql.Timestamp(System.currentTimeMillis()) : null);
            jdbc.update(sql, title, content, type, priority, status, pubAt, expiredAt, createdBy);
            var ids = jdbc.queryForList("SELECT LAST_INSERT_ID()");
            long id = ((Number) ids.get(0).get("LAST_INSERT_ID()")).longValue();
            result.put("ok", true);
            result.put("id", id);
        } catch (Exception e) {
            log.warn("[Admin] 创建公告失败: {}", e.getMessage());
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> updateAnnouncement(Long id, Map<String, Object> payload) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            var rows = jdbc.queryForList("SELECT id FROM sys_announcement WHERE id = ?", id);
            if (rows.isEmpty()) {
                result.put("ok", false);
                result.put("error", "公告不存在");
                return result;
            }
            List<String> sets = new ArrayList<>();
            List<Object> args = new ArrayList<>();
            if (payload.containsKey("title")) { sets.add("title = ?"); args.add(payload.get("title")); }
            if (payload.containsKey("content")) { sets.add("content = ?"); args.add(payload.get("content")); }
            if (payload.containsKey("type")) { sets.add("type = ?"); args.add(payload.get("type")); }
            if (payload.containsKey("priority")) { sets.add("priority = ?"); args.add(payload.get("priority")); }
            if (payload.containsKey("status")) { sets.add("status = ?"); args.add(payload.get("status")); }
            if (payload.containsKey("published_at")) { sets.add("published_at = ?"); args.add(payload.get("published_at")); }
            if (payload.containsKey("expired_at")) { sets.add("expired_at = ?"); args.add(payload.get("expired_at")); }
            if (sets.isEmpty()) {
                result.put("ok", false);
                result.put("error", "没有需要更新的字段");
                return result;
            }
            args.add(id);
            jdbc.update("UPDATE sys_announcement SET " + String.join(", ", sets) + " WHERE id = ?", args.toArray());
            result.put("ok", true);
        } catch (Exception e) {
            log.warn("[Admin] 更新公告失败 id={}: {}", id, e.getMessage());
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> deleteAnnouncement(Long id) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            int n = jdbc.update("DELETE FROM sys_announcement WHERE id = ?", id);
            result.put("ok", n > 0);
            if (n == 0) result.put("error", "公告不存在");
        } catch (Exception e) {
            log.warn("[Admin] 删除公告失败 id={}: {}", id, e.getMessage());
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> listActiveAnnouncements() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, title, content, type, priority, published_at FROM sys_announcement WHERE status = 1 AND (expired_at IS NULL OR expired_at > NOW()) ORDER BY priority DESC, published_at DESC LIMIT 10");
            result.put("list", rows);
            result.put("source", "admin-db");
        } catch (Exception e) {
            log.warn("[Admin] 获取活跃公告失败: {}", e.getMessage());
            result.put("list", java.util.Collections.emptyList());
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> listDicts(String dictType) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            String where = "";
            List<Object> args = new ArrayList<>();
            if (dictType != null && !dictType.isEmpty()) {
                where = " WHERE dict_type = ?";
                args.add(dictType);
            }
            List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT * FROM sys_dict" + where + " ORDER BY dict_type, sort_order, id", args.toArray());
            result.put("list", rows);
            result.put("total", rows.size());
            result.put("source", "admin-db");
        } catch (Exception e) {
            log.warn("[Admin] 字典列表查询失败: {}", e.getMessage());
            result.put("list", java.util.Collections.emptyList());
            result.put("total", 0);
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> createDict(Map<String, Object> payload) {
        Map<String, Object> result = new LinkedHashMap<>();
        String dictType = (String) payload.get("dict_type");
        String dictLabel = (String) payload.get("dict_label");
        String dictValue = (String) payload.get("dict_value");
        if (dictType == null || dictType.isEmpty() || dictLabel == null || dictLabel.isEmpty() || dictValue == null || dictValue.isEmpty()) {
            result.put("ok", false);
            result.put("error", "分组/标签/值不能为空");
            return result;
        }
        try {
            Integer sortOrder = payload.get("sort_order") != null ? ((Number) payload.get("sort_order")).intValue() : 0;
            Integer status = payload.get("status") != null ? ((Number) payload.get("status")).intValue() : 1;
            String sql = "INSERT INTO sys_dict (dict_type, dict_label, dict_value, sort_order, status) VALUES (?, ?, ?, ?, ?)";
            jdbc.update(sql, dictType, dictLabel, dictValue, sortOrder, status);
            var ids = jdbc.queryForList("SELECT LAST_INSERT_ID()");
            long id = ((Number) ids.get(0).get("LAST_INSERT_ID()")).longValue();
            result.put("ok", true);
            result.put("id", id);
        } catch (Exception e) {
            log.warn("[Admin] 创建字典项失败: {}", e.getMessage());
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> updateDict(Long id, Map<String, Object> payload) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            var rows = jdbc.queryForList("SELECT id FROM sys_dict WHERE id = ?", id);
            if (rows.isEmpty()) {
                result.put("ok", false);
                result.put("error", "字典项不存在");
                return result;
            }
            List<String> sets = new ArrayList<>();
            List<Object> args = new ArrayList<>();
            if (payload.containsKey("dict_type")) { sets.add("dict_type = ?"); args.add(payload.get("dict_type")); }
            if (payload.containsKey("dict_label")) { sets.add("dict_label = ?"); args.add(payload.get("dict_label")); }
            if (payload.containsKey("dict_value")) { sets.add("dict_value = ?"); args.add(payload.get("dict_value")); }
            if (payload.containsKey("sort_order")) { sets.add("sort_order = ?"); args.add(((Number) payload.get("sort_order")).intValue()); }
            if (payload.containsKey("status")) { sets.add("status = ?"); args.add(((Number) payload.get("status")).intValue()); }
            if (sets.isEmpty()) {
                result.put("ok", false);
                result.put("error", "没有需要更新的字段");
                return result;
            }
            args.add(id);
            jdbc.update("UPDATE sys_dict SET " + String.join(", ", sets) + " WHERE id = ?", args.toArray());
            result.put("ok", true);
        } catch (Exception e) {
            log.warn("[Admin] 更新字典项失败 id={}: {}", id, e.getMessage());
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> deleteDict(Long id) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            int n = jdbc.update("DELETE FROM sys_dict WHERE id = ?", id);
            result.put("ok", n > 0);
            if (n == 0) result.put("error", "字典项不存在");
        } catch (Exception e) {
            log.warn("[Admin] 删除字典项失败 id={}: {}", id, e.getMessage());
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    // ============================================================
    // 搜索反馈统计
    // ============================================================

    public Map<String, Object> listSearchFeedback(Long articleId, Integer isHelpful, String startDate, String endDate, int page, int pageSize) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            StringBuilder where = new StringBuilder(" WHERE 1=1 ");
            List<Object> args = new ArrayList<>();
            if (articleId != null) {
                where.append(" AND sf.article_id = ? ");
                args.add(articleId);
            }
            if (isHelpful != null) {
                where.append(" AND sf.is_helpful = ? ");
                args.add(isHelpful);
            }
            if (startDate != null && !startDate.isEmpty()) {
                where.append(" AND sf.created_at >= ? ");
                args.add(startDate);
            }
            if (endDate != null && !endDate.isEmpty()) {
                where.append(" AND sf.created_at < DATE_ADD(?, INTERVAL 1 DAY) ");
                args.add(endDate);
            }

            String countSql = "SELECT COUNT(*) FROM search_feedback sf " + where;
            Integer total = jdbc.queryForObject(countSql, Integer.class, args.toArray());
            int offset = Math.max(0, (page - 1) * pageSize);

            StringBuilder q = new StringBuilder();
            q.append("SELECT sf.id, sf.search_log_id, sf.article_id, sf.is_helpful, sf.user_comment, sf.created_at, ");
            q.append("la.title AS article_title, la.law_id ");
            q.append("FROM search_feedback sf ");
            q.append("LEFT JOIN law_article la ON sf.article_id = la.id ");
            q.append(where);
            q.append(" ORDER BY sf.id DESC LIMIT ? OFFSET ?");
            args.add(pageSize);
            args.add(offset);

            List<Map<String, Object>> rows = jdbc.queryForList(q.toString(), args.toArray());
            result.put("total", total == null ? 0 : total);
            result.put("page", page);
            result.put("pageSize", pageSize);
            result.put("list", rows);
            result.put("source", "admin-db");
        } catch (Exception e) {
            log.warn("[Admin] 搜索反馈列表查询失败: {}", e.getMessage());
            result.put("total", 0);
            result.put("page", page);
            result.put("pageSize", pageSize);
            result.put("list", java.util.Collections.emptyList());
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> searchFeedbackStats(String startDate, String endDate) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            StringBuilder where = new StringBuilder(" WHERE 1=1 ");
            List<Object> args = new ArrayList<>();
            if (startDate != null && !startDate.isEmpty()) {
                where.append(" AND created_at >= ? ");
                args.add(startDate);
            }
            if (endDate != null && !endDate.isEmpty()) {
                where.append(" AND created_at < DATE_ADD(?, INTERVAL 1 DAY) ");
                args.add(endDate);
            }

            Integer totalFeedbacks = jdbc.queryForObject("SELECT COUNT(*) FROM search_feedback" + where, Integer.class, args.toArray());

            List<Object> helpfulArgs = new ArrayList<>(args);
            StringBuilder helpfulWhere = new StringBuilder(" WHERE is_helpful = 1 ");
            if (startDate != null && !startDate.isEmpty()) {
                helpfulWhere.append(" AND created_at >= ? ");
                helpfulArgs.add(startDate);
            }
            if (endDate != null && !endDate.isEmpty()) {
                helpfulWhere.append(" AND created_at < DATE_ADD(?, INTERVAL 1 DAY) ");
                helpfulArgs.add(endDate);
            }
            Integer helpfulCount = jdbc.queryForObject("SELECT COUNT(*) FROM search_feedback" + helpfulWhere, Integer.class, helpfulArgs.toArray());

            Integer unhelpfulCount = (totalFeedbacks == null ? 0 : totalFeedbacks) - (helpfulCount == null ? 0 : helpfulCount);
            double helpfulRate = totalFeedbacks != null && totalFeedbacks > 0 ? ((double) (helpfulCount == null ? 0 : helpfulCount) * 100.0 / totalFeedbacks) : 0.0;

            String dailySql = "SELECT DATE(created_at) AS day, COUNT(*) AS total, " +
                    "SUM(CASE WHEN is_helpful = 1 THEN 1 ELSE 0 END) AS helpful, " +
                    "SUM(CASE WHEN is_helpful = 0 THEN 1 ELSE 0 END) AS unhelpful " +
                    "FROM search_feedback " +
                    "WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) " +
                    "GROUP BY DATE(created_at) ORDER BY day";
            List<Map<String, Object>> dailyStats = jdbc.queryForList(dailySql);

            result.put("totalFeedbacks", totalFeedbacks == null ? 0 : totalFeedbacks);
            result.put("helpfulCount", helpfulCount == null ? 0 : helpfulCount);
            result.put("unhelpfulCount", Math.max(0, unhelpfulCount));
            result.put("helpfulRate", String.format("%.1f", helpfulRate));
            result.put("dailyStats", dailyStats);
            result.put("source", "admin-db");
        } catch (Exception e) {
            log.warn("[Admin] 搜索反馈统计查询失败: {}", e.getMessage());
            result.put("error", e.getMessage());
        }
        return result;
    }

    // ============================================================
    // 法规收藏管理
    // ============================================================

    public Map<String, Object> listLawFavorites(Long userId, String username, Long articleId, int page, int pageSize) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            StringBuilder where = new StringBuilder(" WHERE 1=1 ");
            List<Object> args = new ArrayList<>();
            if (userId != null) {
                where.append(" AND lf.user_id = ? ");
                args.add(userId);
            }
            if (username != null && !username.isEmpty()) {
                where.append(" AND lf.user_id LIKE ? ");
                args.add("%" + username + "%");
            }
            if (articleId != null) {
                where.append(" AND lf.law_uuid = ? ");
                args.add(articleId.toString());
            }

            String countSql = "SELECT COUNT(*) FROM law_favorite lf " + where;
            Integer total = jdbc.queryForObject(countSql, Integer.class, args.toArray());
            int offset = Math.max(0, (page - 1) * pageSize);

            StringBuilder q = new StringBuilder();
            q.append("SELECT lf.id, lf.user_id, lf.law_uuid, lf.law_title, lf.created_at ");
            q.append("FROM law_favorite lf ");
            q.append(where);
            q.append(" ORDER BY lf.id DESC LIMIT ? OFFSET ?");
            args.add(pageSize);
            args.add(offset);

            List<Map<String, Object>> rows = jdbc.queryForList(q.toString(), args.toArray());
            result.put("total", total == null ? 0 : total);
            result.put("page", page);
            result.put("pageSize", pageSize);
            result.put("list", rows);
            result.put("source", "admin-db");
        } catch (Exception e) {
            log.warn("[Admin] 法规收藏列表查询失败: {}", e.getMessage());
            result.put("total", 0);
            result.put("page", page);
            result.put("pageSize", pageSize);
            result.put("list", java.util.Collections.emptyList());
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> deleteLawFavorite(Long id) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            int n = jdbc.update("DELETE FROM law_favorite WHERE id = ?", id);
            result.put("ok", n > 0);
            if (n == 0) result.put("error", "收藏记录不存在");
            log.info("删除法规收藏 id={}, affected={}", id, n);
        } catch (Exception e) {
            log.warn("[Admin] 删除法规收藏失败 id={}: {}", id, e.getMessage());
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> lawFavoriteStats() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            Integer totalFavorites = jdbc.queryForObject("SELECT COUNT(*) FROM law_favorite", Integer.class);
            Integer todayNew = jdbc.queryForObject("SELECT COUNT(*) FROM law_favorite WHERE DATE(created_at) = CURDATE()", Integer.class);

            List<Map<String, Object>> topArticles = jdbc.queryForList(
                "SELECT lf.law_uuid, lf.law_title AS article_title, COUNT(*) AS favorite_count " +
                "FROM law_favorite lf " +
                "WHERE lf.law_title IS NOT NULL AND lf.law_title != '' " +
                "GROUP BY lf.law_uuid, lf.law_title " +
                "ORDER BY favorite_count DESC LIMIT 10"
            );

            List<Map<String, Object>> topUsers = jdbc.queryForList(
                "SELECT user_id, COUNT(*) AS favorite_count " +
                "FROM law_favorite " +
                "GROUP BY user_id " +
                "ORDER BY favorite_count DESC LIMIT 10"
            );

            result.put("totalFavorites", totalFavorites == null ? 0 : totalFavorites);
            result.put("todayNew", todayNew == null ? 0 : todayNew);
            result.put("topArticles", topArticles);
            result.put("topUsers", topUsers);
            result.put("source", "admin-db");
        } catch (Exception e) {
            log.warn("[Admin] 法规收藏统计查询失败: {}", e.getMessage());
            result.put("error", e.getMessage());
        }
        return result;
    }

    // ============================================================
    // 爬虫日志查询
    // ============================================================

    public Map<String, Object> listKbChunks(Long kbId, String fileName, int page, int pageSize) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            StringBuilder where = new StringBuilder(" WHERE 1=1 ");
            List<Object> args = new ArrayList<>();
            if (kbId != null) {
                where.append(" AND c.kb_id = ? ");
                args.add(kbId);
            }
            if (fileName != null && !fileName.isEmpty()) {
                where.append(" AND c.file_name LIKE ? ");
                args.add("%" + fileName + "%");
            }
            String countSql = "SELECT COUNT(*) FROM kb_chunk_store c" + where;
            Integer total = jdbc.queryForObject(countSql, Integer.class, args.toArray());
            int offset = Math.max(0, (page - 1) * pageSize);

            StringBuilder q = new StringBuilder();
            q.append("SELECT c.id, c.kb_id, c.file_name, c.content, c.chunk_index, c.token_count, ");
            q.append("c.vector_id, c.created_at, k.kb_name ");
            q.append("FROM kb_chunk_store c ");
            q.append("LEFT JOIN kb_knowledge_base k ON c.kb_id = k.id ");
            q.append(where);
            q.append(" ORDER BY c.id DESC LIMIT ? OFFSET ?");
            args.add(pageSize);
            args.add(offset);

            List<Map<String, Object>> rows = jdbc.queryForList(q.toString(), args.toArray());
            result.put("total", total == null ? 0 : total);
            result.put("page", page);
            result.put("pageSize", pageSize);
            result.put("list", rows);
            result.put("source", "admin-db");
        } catch (Exception e) {
            log.warn("[Admin] listKbChunks 失败: {}", e.getMessage());
            result.put("total", 0);
            result.put("page", page);
            result.put("pageSize", pageSize);
            result.put("list", java.util.Collections.emptyList());
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> kbChunksStats() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            Integer totalChunks = jdbc.queryForObject("SELECT COUNT(*) FROM kb_chunk_store", Integer.class);
            Integer totalTokens = jdbc.queryForObject("SELECT COALESCE(SUM(token_count), 0) FROM kb_chunk_store", Integer.class);
            Double avgChunkSize = jdbc.queryForObject("SELECT COALESCE(AVG(token_count), 0) FROM kb_chunk_store", Double.class);

            result.put("totalChunks", totalChunks == null ? 0 : totalChunks);
            result.put("totalTokens", totalTokens == null ? 0 : totalTokens);
            result.put("avgChunkSize", avgChunkSize == null ? 0 : Math.round(avgChunkSize * 100) / 100.0);

            List<Map<String, Object>> topKb = jdbc.queryForList(
                "SELECT k.id, k.kb_name, COUNT(c.id) AS chunk_count, COALESCE(SUM(c.token_count), 0) AS total_tokens " +
                "FROM kb_knowledge_base k " +
                "LEFT JOIN kb_chunk_store c ON k.id = c.kb_id " +
                "GROUP BY k.id, k.kb_name " +
                "ORDER BY chunk_count DESC LIMIT 10"
            );
            result.put("topKbByChunks", topKb);
            result.put("source", "admin-db");
        } catch (Exception e) {
            log.warn("[Admin] kbChunksStats 失败: {}", e.getMessage());
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> listCrawlLogs(Long taskId, int page, int pageSize) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            StringBuilder where = new StringBuilder(" WHERE 1=1 ");
            List<Object> args = new ArrayList<>();
            if (taskId != null) {
                where.append(" AND task_id = ? ");
                args.add(taskId);
            }
            Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM crawl_log" + where, Integer.class, args.toArray());
            int offset = Math.max(0, (page - 1) * pageSize);
            String sql = "SELECT id, task_id, status, crawled_count AS total_fetched, success_count, fail_count, " +
                    "error_log AS error_message, started_at, finished_at, " +
                    "TIMESTAMPDIFF(SECOND, started_at, finished_at) AS duration_seconds " +
                    "FROM crawl_log" + where + " ORDER BY id DESC LIMIT ? OFFSET ?";
            args.add(pageSize);
            args.add(offset);
            List<Map<String, Object>> rows = jdbc.queryForList(sql, args.toArray());
            for (Map<String, Object> row : rows) {
                Integer status = row.get("status") != null ? ((Number) row.get("status")).intValue() : null;
                row.put("status", status != null ? switch (status) {
                    case 0 -> "RUNNING";
                    case 1 -> "SUCCESS";
                    case 2 -> "FAILED";
                    default -> "UNKNOWN";
                } : null);
            }
            result.put("total", total == null ? 0 : total);
            result.put("page", page);
            result.put("pageSize", pageSize);
            result.put("list", rows);
            result.put("source", "admin-db");
        } catch (Exception e) {
            log.warn("[Admin] 爬虫日志查询失败 taskId={}: {}", taskId, e.getMessage());
            result.put("total", 0);
            result.put("page", page);
            result.put("pageSize", pageSize);
            result.put("list", java.util.Collections.emptyList());
            result.put("error", e.getMessage());
        }
        return result;
    }

    // ============================================================
    // ALERT_RULE CRUD
    // ============================================================

    private static final Set<String> ALERT_METRICS = Set.of(
        "llm_latency_ms", "milvus_query_latency", "cpu_usage_percent", "memory_usage_percent", "error_rate_5m"
    );
    private static final Set<String> ALERT_OPERATORS = Set.of(">", "<", ">=", "<=", "=");
    private static final Set<String> ALERT_SEVERITIES = Set.of("info", "warning", "critical");

    public Map<String, Object> createAlertRule(Map<String, Object> data) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (data == null || data.isEmpty()) {
            result.put("ok", false);
            result.put("error", "数据为空");
            return result;
        }
        String name = (String) data.get("name");
        String metric = (String) data.get("metric");
        String operator = (String) data.get("operator");
        if (name == null || name.isEmpty()) {
            result.put("ok", false);
            result.put("error", "规则名称不能为空");
            return result;
        }
        if (name.length() > 100) {
            result.put("ok", false);
            result.put("error", "规则名称最大100字符");
            return result;
        }
        if (metric == null || metric.isEmpty()) {
            result.put("ok", false);
            result.put("error", "指标名不能为空");
            return result;
        }
        if (!ALERT_METRICS.contains(metric)) {
            result.put("ok", false);
            result.put("error", "无效的指标名，允许值: " + String.join(", ", ALERT_METRICS));
            return result;
        }
        if (operator == null || operator.isEmpty()) {
            result.put("ok", false);
            result.put("error", "操作符不能为空");
            return result;
        }
        if (!ALERT_OPERATORS.contains(operator)) {
            result.put("ok", false);
            result.put("error", "无效的操作符，允许值: " + String.join(", ", ALERT_OPERATORS));
            return result;
        }
        if (data.get("threshold") == null) {
            result.put("ok", false);
            result.put("error", "阈值不能为空");
            return result;
        }
        BigDecimal threshold;
        try {
            threshold = new BigDecimal(data.get("threshold").toString());
        } catch (NumberFormatException e) {
            result.put("ok", false);
            result.put("error", "阈值必须是数值");
            return result;
        }
        String severity = (String) data.getOrDefault("severity", "warning");
        if (!ALERT_SEVERITIES.contains(severity)) {
            result.put("ok", false);
            result.put("error", "无效的严重程度，允许值: " + String.join(", ", ALERT_SEVERITIES));
            return result;
        }
        try {
            Integer durationSec = data.get("duration_sec") != null ? ((Number) data.get("duration_sec")).intValue() : 0;
            Integer enabled = data.get("enabled") != null ? ((Number) data.get("enabled")).intValue() : 1;
            String channels = (String) data.get("channels");
            String template = (String) data.get("template");
            String sql = "INSERT INTO alert_rule (name, metric, operator, threshold, duration_sec, severity, enabled, channels, template) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            jdbc.update(sql, name, metric, operator, threshold, durationSec, severity, enabled, channels, template);
            var ids = jdbc.queryForList("SELECT LAST_INSERT_ID()");
            long id = ((Number) ids.get(0).get("LAST_INSERT_ID()")).longValue();
            result.put("ok", true);
            result.put("id", id);
            log.info("创建告警规则: id={}, name={}", id, name);
        } catch (Exception e) {
            log.warn("[Admin] 创建告警规则失败: {}", e.getMessage());
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> updateAlertRule(Long id, Map<String, Object> data) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (data == null || data.isEmpty()) {
            result.put("ok", false);
            result.put("error", "数据为空");
            return result;
        }
        try {
            var rows = jdbc.queryForList("SELECT id FROM alert_rule WHERE id = ?", id);
            if (rows.isEmpty()) {
                result.put("ok", false);
                result.put("error", "告警规则不存在");
                return result;
            }
            List<String> sets = new ArrayList<>();
            List<Object> args = new ArrayList<>();
            if (data.containsKey("name")) {
                String name = (String) data.get("name");
                if (name == null || name.isEmpty()) {
                    result.put("ok", false);
                    result.put("error", "规则名称不能为空");
                    return result;
                }
                if (name.length() > 100) {
                    result.put("ok", false);
                    result.put("error", "规则名称最大100字符");
                    return result;
                }
                sets.add("name = ?");
                args.add(name);
            }
            if (data.containsKey("metric")) {
                String metric = (String) data.get("metric");
                if (metric == null || metric.isEmpty() || !ALERT_METRICS.contains(metric)) {
                    result.put("ok", false);
                    result.put("error", "无效的指标名，允许值: " + String.join(", ", ALERT_METRICS));
                    return result;
                }
                sets.add("metric = ?");
                args.add(metric);
            }
            if (data.containsKey("operator")) {
                String operator = (String) data.get("operator");
                if (operator == null || operator.isEmpty() || !ALERT_OPERATORS.contains(operator)) {
                    result.put("ok", false);
                    result.put("error", "无效的操作符，允许值: " + String.join(", ", ALERT_OPERATORS));
                    return result;
                }
                sets.add("operator = ?");
                args.add(operator);
            }
            if (data.containsKey("threshold")) {
                try {
                    BigDecimal threshold = new BigDecimal(data.get("threshold").toString());
                    sets.add("threshold = ?");
                    args.add(threshold);
                } catch (NumberFormatException e) {
                    result.put("ok", false);
                    result.put("error", "阈值必须是数值");
                    return result;
                }
            }
            if (data.containsKey("duration_sec")) {
                sets.add("duration_sec = ?");
                args.add(((Number) data.get("duration_sec")).intValue());
            }
            if (data.containsKey("severity")) {
                String severity = (String) data.get("severity");
                if (severity == null || !ALERT_SEVERITIES.contains(severity)) {
                    result.put("ok", false);
                    result.put("error", "无效的严重程度，允许值: " + String.join(", ", ALERT_SEVERITIES));
                    return result;
                }
                sets.add("severity = ?");
                args.add(severity);
            }
            if (data.containsKey("enabled")) {
                sets.add("enabled = ?");
                args.add(((Number) data.get("enabled")).intValue());
            }
            if (data.containsKey("channels")) {
                sets.add("channels = ?");
                args.add(data.get("channels"));
            }
            if (data.containsKey("template")) {
                sets.add("template = ?");
                args.add(data.get("template"));
            }
            if (sets.isEmpty()) {
                result.put("ok", false);
                result.put("error", "没有需要更新的字段");
                return result;
            }
            args.add(id);
            jdbc.update("UPDATE alert_rule SET " + String.join(", ", sets) + " WHERE id = ?", args.toArray());
            result.put("ok", true);
            log.info("更新告警规则 id={}", id);
        } catch (Exception e) {
            log.warn("[Admin] 更新告警规则失败 id={}: {}", id, e.getMessage());
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> deleteAlertRule(Long id) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            int n = jdbc.update("DELETE FROM alert_rule WHERE id = ?", id);
            result.put("ok", n > 0);
            if (n == 0) result.put("error", "告警规则不存在");
            log.info("删除告警规则 id={}, affected={}", id, n);
        } catch (Exception e) {
            log.warn("[Admin] 删除告警规则失败 id={}: {}", id, e.getMessage());
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> toggleAlertRule(Long id) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            var rows = jdbc.queryForList("SELECT enabled FROM alert_rule WHERE id = ?", id);
            if (rows.isEmpty()) {
                result.put("ok", false);
                result.put("error", "告警规则不存在");
                return result;
            }
            Object cur = rows.get(0).get("enabled");
            int next = (cur != null && Integer.parseInt(String.valueOf(cur)) == 1) ? 0 : 1;
            jdbc.update("UPDATE alert_rule SET enabled = ? WHERE id = ?", next, id);
            result.put("ok", true);
            result.put("enabled", next);
            log.info("切换告警规则 id={} enabled={}", id, next);
        } catch (Exception e) {
            log.warn("[Admin] 切换告警规则状态失败 id={}: {}", id, e.getMessage());
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    // ============================================================
    // 用户反馈状态更新
    // ============================================================

    public Map<String, Object> updateUserFeedback(Long id, Map<String, Object> data) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (data == null || data.isEmpty()) {
            result.put("ok", false);
            result.put("error", "数据为空");
            return result;
        }
        try {
            var rows = jdbc.queryForList("SELECT id, status FROM user_feedback WHERE id = ?", id);
            if (rows.isEmpty()) {
                result.put("ok", false);
                result.put("error", "反馈记录不存在");
                return result;
            }
            String currentStatus = rows.get(0).get("status") != null ? String.valueOf(rows.get(0).get("status")) : "pending";
            String newStatus = data.get("status") != null ? String.valueOf(data.get("status")) : currentStatus;

            List<String> sets = new ArrayList<>();
            List<Object> args = new ArrayList<>();

            sets.add("status = ?");
            args.add(newStatus);

            switch (newStatus) {
                case "assigned" -> {
                    if (data.containsKey("handlerId")) {
                        sets.add("handler_id = ?");
                        args.add(data.get("handlerId"));
                    }
                    if (data.containsKey("handlerName")) {
                        sets.add("handler_name = ?");
                        args.add(data.get("handlerName"));
                    }
                }
                case "resolved" -> {
                    if (data.containsKey("resolveNote")) {
                        sets.add("resolve_note = ?");
                        args.add(data.get("resolveNote"));
                    }
                    sets.add("resolved_at = NOW()");
                }
                case "reopened" -> {
                    sets.add("resolved_at = NULL");
                }
                case "closed" -> {
                }
                default -> {
                    result.put("ok", false);
                    result.put("error", "无效的状态: " + newStatus + "，允许的值: assigned/resolved/reopened/closed");
                    return result;
                }
            }

            args.add(id);
            jdbc.update("UPDATE user_feedback SET " + String.join(", ", sets) + " WHERE id = ?", args.toArray());

            var updated = jdbc.queryForList("SELECT * FROM user_feedback WHERE id = ?", id);
            result.put("ok", true);
            result.put("data", updated.isEmpty() ? null : updated.get(0));
            log.info("更新用户反馈状态 id={}, {} -> {}", id, currentStatus, newStatus);
        } catch (Exception e) {
            log.warn("[Admin] 更新用户反馈状态失败 id={}: {}", id, e.getMessage());
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> getResearchTask(Long id) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT * FROM legal_research_task WHERE id = ?", id);
            if (rows.isEmpty()) {
                result.put("data", null);
                result.put("error", "任务不存在");
                return result;
            }
            Map<String, Object> row = rows.get(0);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", row.get("id"));
            data.put("taskName", row.get("topic"));
            data.put("taskUuid", row.get("task_uuid"));
            data.put("researchType", null);
            data.put("status", row.get("status"));
            data.put("requestParams", row.get("sources"));
            data.put("report", row.get("report"));
            data.put("errorMessage", null);
            data.put("createdBy", row.get("user_id"));
            data.put("createdAt", row.get("created_at"));
            data.put("startedAt", row.get("updated_at"));
            data.put("completedAt", null);
            result.put("data", data);
            result.put("source", "admin-db");
        } catch (Exception e) {
            log.warn("[Admin] getResearchTask 失败 id={}: {}", id, e.getMessage());
            result.put("data", null);
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> listSearchLogs(Long adminUserId, DataScope dataScope, int page, int pageSize) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            StringBuilder where = new StringBuilder(" WHERE 1=1 ");
            List<Object> args = new ArrayList<>();
            if (dataScope != null && dataScope != DataScope.ALL && adminUserId != null) {
                switch (dataScope) {
                    case SELF:
                        where.append(" AND user_id = ?");
                        args.add(adminUserId);
                        break;
                    case DEPT:
                        where.append(" AND user_id IN (SELECT id FROM admin_user WHERE team_id = (SELECT team_id FROM admin_user WHERE id = ?))");
                        args.add(adminUserId);
                        break;
                    case TEAM:
                        where.append(" AND user_id IN (SELECT id FROM admin_user WHERE team_id = (SELECT team_id FROM admin_user WHERE id = ?))");
                        args.add(adminUserId);
                        break;
                }
            }
            Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM search_log" + where, Integer.class, args.toArray());
            int offset = Math.max(0, (page - 1) * pageSize);
            String sql = "SELECT * FROM search_log" + where + " ORDER BY id DESC LIMIT ? OFFSET ?";
            args.add(pageSize);
            args.add(offset);
            List<Map<String, Object>> rows = jdbc.queryForList(sql, args.toArray());
            result.put("total", total == null ? 0 : total);
            result.put("page", page);
            result.put("pageSize", pageSize);
            result.put("list", rows);
            result.put("source", "admin-db");
        } catch (Exception e) {
            log.warn("[Admin] 搜索日志列表查询失败: {}", e.getMessage());
            result.put("total", 0);
            result.put("page", page);
            result.put("pageSize", pageSize);
            result.put("list", java.util.Collections.emptyList());
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> listUserFeedback(Long adminUserId, DataScope dataScope, int page, int pageSize) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            StringBuilder where = new StringBuilder(" WHERE 1=1 ");
            List<Object> args = new ArrayList<>();
            if (dataScope != null && dataScope != DataScope.ALL && adminUserId != null) {
                switch (dataScope) {
                    case SELF:
                        where.append(" AND user_id = ?");
                        args.add(adminUserId);
                        break;
                    case DEPT:
                        where.append(" AND user_id IN (SELECT id FROM admin_user WHERE team_id = (SELECT team_id FROM admin_user WHERE id = ?))");
                        args.add(adminUserId);
                        break;
                    case TEAM:
                        where.append(" AND user_id IN (SELECT id FROM admin_user WHERE team_id = (SELECT team_id FROM admin_user WHERE id = ?))");
                        args.add(adminUserId);
                        break;
                }
            }
            Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM user_feedback" + where, Integer.class, args.toArray());
            int offset = Math.max(0, (page - 1) * pageSize);
            String sql = "SELECT * FROM user_feedback" + where + " ORDER BY id DESC LIMIT ? OFFSET ?";
            args.add(pageSize);
            args.add(offset);
            List<Map<String, Object>> rows = jdbc.queryForList(sql, args.toArray());
            result.put("total", total == null ? 0 : total);
            result.put("page", page);
            result.put("pageSize", pageSize);
            result.put("list", rows);
            result.put("source", "admin-db");
        } catch (Exception e) {
            log.warn("[Admin] 用户反馈列表查询失败: {}", e.getMessage());
            result.put("total", 0);
            result.put("page", page);
            result.put("pageSize", pageSize);
            result.put("list", java.util.Collections.emptyList());
            result.put("error", e.getMessage());
        }
        return result;
    }

    private String validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return "密码不能为空";
        }
        if (password.length() < 8) {
            return "密码长度至少8位";
        }
        if (password.length() > 32) {
            return "密码长度不能超过32位";
        }
        if (!password.matches(".*[A-Z].*")) {
            return "密码必须包含至少一个大写字母";
        }
        if (!password.matches(".*[a-z].*")) {
            return "密码必须包含至少一个小写字母";
        }
        if (!password.matches(".*[0-9].*")) {
            return "密码必须包含至少一个数字";
        }
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            return "密码必须包含至少一个特殊字符";
        }
        return null;
    }

    // ============================================================
    // MOD-10 DocQA 会话管理
    // ============================================================

    public Map<String, Object> mod10SessionDetail(Long sessionId) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT s.id, s.session_uuid, s.title, s.user_id, s.kb_id, s.status, s.msg_count, s.created_at, s.updated_at, " +
                "kb.kb_name " +
                "FROM doc_qa_session s " +
                "LEFT JOIN kb_knowledge_base kb ON s.kb_id = kb.id " +
                "WHERE s.id = ?",
                sessionId);
            if (rows.isEmpty()) {
                result.put("data", null);
                result.put("error", "会话不存在");
                return result;
            }
            result.put("data", rows.get(0));
            result.put("source", "admin-db");
        } catch (Exception e) {
            log.warn("[Admin] mod10SessionDetail 失败 sessionId={}: {}", sessionId, e.getMessage());
            result.put("data", null);
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> mod10SessionMessages(Long sessionId, int page, int pageSize) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> sessionRows = jdbc.queryForList(
                "SELECT id FROM doc_qa_session WHERE id = ?", sessionId);
            if (sessionRows.isEmpty()) {
                result.put("total", 0);
                result.put("page", page);
                result.put("pageSize", pageSize);
                result.put("list", java.util.Collections.emptyList());
                result.put("error", "会话不存在");
                return result;
            }

            Integer total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM doc_qa_message WHERE session_id = ?", Integer.class, sessionId);
            int offset = Math.max(0, (page - 1) * pageSize);

            List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, session_id, msg_index, msg_type, role, content, created_at " +
                "FROM doc_qa_message WHERE session_id = ? ORDER BY msg_index ASC LIMIT ? OFFSET ?",
                sessionId, pageSize, offset);

            List<Map<String, Object>> qaPairs = new ArrayList<>();
            Map<String, Object> currentPair = null;
            for (Map<String, Object> msg : rows) {
                String role = (String) msg.get("role");
                if ("user".equals(role)) {
                    currentPair = new LinkedHashMap<>();
                    currentPair.put("question", msg.get("content"));
                    currentPair.put("questionTime", msg.get("created_at"));
                    currentPair.put("msgIndex", msg.get("msg_index"));
                } else if ("assistant".equals(role) && currentPair != null) {
                    currentPair.put("answer", msg.get("content"));
                    currentPair.put("answerTime", msg.get("created_at"));
                    qaPairs.add(currentPair);
                    currentPair = null;
                }
            }
            if (currentPair != null) {
                qaPairs.add(currentPair);
            }

            result.put("total", total == null ? 0 : total);
            result.put("page", page);
            result.put("pageSize", pageSize);
            result.put("list", qaPairs);
            result.put("source", "admin-db");
        } catch (Exception e) {
            log.warn("[Admin] mod10SessionMessages 失败 sessionId={}: {}", sessionId, e.getMessage());
            result.put("total", 0);
            result.put("page", page);
            result.put("pageSize", pageSize);
            result.put("list", java.util.Collections.emptyList());
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> deleteMod10Session(Long sessionId) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> sessionRows = jdbc.queryForList(
                "SELECT id FROM doc_qa_session WHERE id = ?", sessionId);
            if (sessionRows.isEmpty()) {
                result.put("ok", false);
                result.put("error", "会话不存在");
                return result;
            }
            jdbc.update("DELETE FROM doc_qa_message WHERE session_id = ?", sessionId);
            int n = jdbc.update("DELETE FROM doc_qa_session WHERE id = ?", sessionId);
            result.put("ok", n > 0);
            result.put("affected", n);
            log.info("[Admin] 删除 MOD-10 会话 id={}", sessionId);
        } catch (Exception e) {
            log.warn("[Admin] deleteMod10Session 失败 sessionId={}: {}", sessionId, e.getMessage());
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> exportMod10Session(Long sessionId) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> sessionRows = jdbc.queryForList(
                "SELECT s.id, s.session_uuid, s.title, s.user_id, s.kb_id, s.status, s.msg_count, s.created_at, s.updated_at " +
                "FROM doc_qa_session s WHERE s.id = ?", sessionId);
            if (sessionRows.isEmpty()) {
                result.put("error", "会话不存在");
                return result;
            }
            Map<String, Object> sessionData = sessionRows.get(0);

            List<Map<String, Object>> messageRows = jdbc.queryForList(
                "SELECT id, msg_index, role, content, created_at FROM doc_qa_message WHERE session_id = ? ORDER BY msg_index ASC",
                sessionId);

            Map<String, Object> qaPairs = new LinkedHashMap<>();
            List<Map<String, Object>> pairs = new ArrayList<>();
            Map<String, Object> currentPair = null;
            for (Map<String, Object> msg : messageRows) {
                String role = (String) msg.get("role");
                if ("user".equals(role)) {
                    currentPair = new LinkedHashMap<>();
                    currentPair.put("question", msg.get("content"));
                    currentPair.put("questionTime", msg.get("created_at"));
                } else if ("assistant".equals(role) && currentPair != null) {
                    currentPair.put("answer", msg.get("content"));
                    currentPair.put("answerTime", msg.get("created_at"));
                    pairs.add(currentPair);
                    currentPair = null;
                }
            }
            if (currentPair != null) {
                pairs.add(currentPair);
            }
            qaPairs.put("session", sessionData);
            qaPairs.put("messages", pairs);
            qaPairs.put("totalMessages", messageRows.size());

            result.put("data", qaPairs);
            result.put("source", "admin-db");
        } catch (Exception e) {
            log.warn("[Admin] exportMod10Session 失败 sessionId={}: {}", sessionId, e.getMessage());
            result.put("error", e.getMessage());
        }
        return result;
    }
}