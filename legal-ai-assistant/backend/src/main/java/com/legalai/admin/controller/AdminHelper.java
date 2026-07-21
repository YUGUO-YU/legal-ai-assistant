package com.legalai.admin.controller;

import com.legalai.admin.service.AdminDataService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class AdminHelper {
    private static final Logger log = LoggerFactory.getLogger(AdminHelper.class);

    @Autowired
    private AdminDataService adminDataService;

    public Map<String, Object> getCurrentAdminUser(HttpServletRequest request) {
        if (request == null) {
            return adminDataService.getCurrentAdminInfo(null);
        }
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return adminDataService.getCurrentAdminInfo(null);
        }
        String token = authHeader.substring(7);
        if (token.isEmpty()) {
            return adminDataService.getCurrentAdminInfo(null);
        }
        try {
            var rows = adminDataService.jdbc().queryForList(
                "SELECT user_id FROM auth_tokens WHERE token = ? AND expire_at > NOW()",
                token);
            if (rows.isEmpty()) {
                return adminDataService.getCurrentAdminInfo(null);
            }
            Long userId = Long.valueOf(rows.get(0).get("user_id").toString());
            return adminDataService.getCurrentAdminInfo(userId);
        } catch (Exception e) {
            log.warn("[Admin] getCurrentAdminUser 失败: {}", e.getMessage());
            return adminDataService.getCurrentAdminInfo(null);
        }
    }

    public void recordAudit(String operation, String bizType, String bizId) {
        try {
            adminDataService.recordAudit(null, "system", operation, "ADMIN",
                    bizType, bizId, "/api/v1/admin", "GET", null, "ok", null, 0, true, null);
        } catch (Exception e) {
            log.warn("审计日志写入失败: {}", e.getMessage());
        }
    }

    public String escapeCsv(Object value) {
        if (value == null) return "";
        String str = value.toString();
        if (str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains("\r")) {
            return "\"" + str.replace("\"", "\"\"") + "\"";
        }
        return str;
    }
}
