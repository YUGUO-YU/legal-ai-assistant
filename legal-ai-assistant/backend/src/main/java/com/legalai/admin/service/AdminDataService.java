package com.legalai.admin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class AdminDataService {
    private static final Logger log = LoggerFactory.getLogger(AdminDataService.class);

    @Autowired
    private JdbcTemplate jdbc;

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
                // 真实环境应发 HEAD/GET 请求；骨架阶段用 DB 状态模拟
                boolean healthy = Integer.parseInt(String.valueOf(m.getOrDefault("health_status", 1))) == 1;
                long dur = System.currentTimeMillis() - t0 + (long) (Math.random() * 80 + 20);
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
}