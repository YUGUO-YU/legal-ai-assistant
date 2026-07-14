package com.legalai.admin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class OperationRollbackService {

    private static final Logger log = LoggerFactory.getLogger(OperationRollbackService.class);

    private static final String ROLLBACK_TABLE = "sys_operation_rollback";
    private static final int MAX_HISTORY = 100;

    @Autowired
    private JdbcTemplate jdbc;

    public void initTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS sys_operation_rollback (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                operation_id VARCHAR(64) NOT NULL,
                operation_type VARCHAR(32) NOT NULL,
                target_table VARCHAR(64) NOT NULL,
                target_id VARCHAR(64) NOT NULL,
                before_state JSON,
                after_state JSON,
                operator_id VARCHAR(64),
                operator_name VARCHAR(128),
                reason VARCHAR(512),
                status VARCHAR(16) DEFAULT 'PENDING',
                rolled_back_at DATETIME,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_operation_id (operation_id),
                INDEX idx_target (target_table, target_id),
                INDEX idx_status (status),
                INDEX idx_created (created_at)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """;
        try {
            jdbc.execute(sql);
            log.info("操作回滚表初始化完成");
        } catch (Exception e) {
            log.warn("操作回滚表已存在或创建失败: {}", e.getMessage());
        }
    }

    public String recordOperation(String operationType, String targetTable, String targetId,
                                   Object beforeState, Object afterState, String operatorId, String operatorName) {
        String operationId = UUID.randomUUID().toString();

        try {
            String beforeJson = toJson(beforeState);
            String afterJson = toJson(afterState);

            jdbc.update("""
                INSERT INTO sys_operation_rollback (operation_id, operation_type, target_table, target_id,
                    before_state, after_state, operator_id, operator_name, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PENDING')
            """, operationId, operationType, targetTable, targetId, beforeJson, afterJson, operatorId, operatorName);

            log.info("操作已记录: operationId={}, type={}, target={}:{}",
                    operationId, operationType, targetTable, targetId);

            cleanupOldRecords();

            return operationId;
        } catch (Exception e) {
            log.error("记录操作失败: {}", e.getMessage());
            return null;
        }
    }

    public boolean rollback(String operationId, String reason) {
        try {
            List<Map<String, Object>> records = jdbc.queryForList(
                    "SELECT * FROM sys_operation_rollback WHERE operation_id = ? AND status = 'PENDING'",
                    operationId);

            if (records.isEmpty()) {
                log.warn("操作记录不存在或已回滚: {}", operationId);
                return false;
            }

            Map<String, Object> record = records.get(0);
            String targetTable = (String) record.get("target_table");
            String targetId = (String) record.get("target_id");
            String beforeState = (String) record.get("before_state");

            if (beforeState == null || beforeState.isEmpty()) {
                log.warn("无法回滚，缺少前置状态: {}", operationId);
                return false;
            }

            executeRollback(targetTable, targetId, beforeState);

            jdbc.update("""
                UPDATE sys_operation_rollback SET status = 'ROLLED_BACK',
                    rolled_back_at = NOW(), reason = ? WHERE operation_id = ?
            """, reason, operationId);

            log.info("操作已回滚: operationId={}, reason={}", operationId, reason);
            return true;

        } catch (Exception e) {
            log.error("回滚失败: operationId={}, error={}", operationId, e.getMessage());
            try {
                jdbc.update("UPDATE sys_operation_rollback SET status = 'FAILED' WHERE operation_id = ?", operationId);
            } catch (Exception e) { log.error("更新回滚状态失败: operationId={}, error={}", operationId, e.getMessage()); }
            return false;
        }
    }

    public List<Map<String, Object>> getPendingRollbacks() {
        return jdbc.queryForList(
                "SELECT * FROM sys_operation_rollback WHERE status = 'PENDING' ORDER BY created_at DESC LIMIT 50");
    }

    public List<Map<String, Object>> getRollbackHistory(String targetTable, String targetId) {
        return jdbc.queryForList(
                "SELECT * FROM sys_operation_rollback WHERE target_table = ? AND target_id = ? ORDER BY created_at DESC",
                targetTable, targetId);
    }

    private void executeRollback(String table, String id, String beforeState) {
        String primaryKey = getPrimaryKey(table);
        if (primaryKey == null) {
            primaryKey = "id";
        }

        try {
            String updateSql = String.format("UPDATE %s SET %s = ? WHERE %s = ?",
                    table, beforeState.contains("\"deleted\":true") ? "deleted = 1" : "status = 'ACTIVE'", primaryKey);

            jdbc.update(updateSql, parseJson(beforeState), id);
        } catch (Exception e) {
            log.error("执行回滚SQL失败: table={}, id={}, error={}", table, id, e.getMessage());
            throw e;
        }
    }

    private String getPrimaryKey(String table) {
        try {
            List<Map<String, Object>> cols = jdbc.queryForList(
                    "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE " +
                    "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND CONSTRAINT_NAME = 'PRIMARY'",
                    table);
            if (!cols.isEmpty()) {
                return (String) cols.get(0).get("COLUMN_NAME");
            }
        } catch (Exception e) {
            log.debug("获取主键失败: table={}", table);
        }
        return "id";
    }

    private void cleanupOldRecords() {
        try {
            jdbc.update(String.format("""
                DELETE FROM sys_operation_rollback WHERE status IN ('ROLLED_BACK', 'FAILED')
                AND id NOT IN (SELECT id FROM (SELECT id FROM sys_operation_rollback ORDER BY created_at DESC LIMIT %d) t)
            """, MAX_HISTORY));
        } catch (Exception e) {
            log.debug("清理旧记录失败: {}", e.getMessage());
        }
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }

    private String parseJson(String json) {
        if (json == null || json.isEmpty()) return json;
        try {
            var map = new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Map.class);
            if (map.containsKey("status")) {
                return String.format("status = '%s'", map.get("status"));
            }
            if (map.containsKey("deleted")) {
                return String.format("deleted = %s", Boolean.TRUE.equals(map.get("deleted")) ? 1 : 0);
            }
        } catch (Exception e) {}
        return json;
    }
}
