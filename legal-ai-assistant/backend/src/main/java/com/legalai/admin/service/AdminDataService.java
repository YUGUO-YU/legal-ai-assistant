package com.legalai.admin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
                where.append(" AND (biz_module = ? OR ? = '' OR biz_module IS NULL) ");
                args.add(module);
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
            if (userId != null && !userId.isEmpty()) { where.append(" AND user_id = ? "); args.add(Long.valueOf(userId)); }
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
}