package com.legalai.admin.controller;

import com.legalai.admin.annotation.RateLimit;
import com.legalai.admin.enums.DataScope;
import com.legalai.admin.service.AdminDataService;
import com.legalai.dto.ApiResponse;
import com.legalai.service.CacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/infra")
@CrossOrigin
@Tag(name = "管理后台-基础设施", description = "用户、角色、菜单、审计、公告、字典等基础设施管理")
public class InfraController {
    private static final Logger log = LoggerFactory.getLogger(InfraController.class);

    @Autowired
    private AdminDataService adminDataService;

    @Autowired
    private AdminHelper adminHelper;

    @Autowired(required = false)
    private CacheService cacheService;

    @Operation(summary = "查询管理员用户")
    @GetMapping("/users")
    public ApiResponse<Map<String, Object>> listUsers(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(description = "关键词搜索") @RequestParam(required = false) String keyword) {
        return ApiResponse.success(adminDataService.list("admin_user", null, page, pageSize, keyword));
    }

    @Operation(summary = "查询角色列表")
    @GetMapping("/roles")
    public ApiResponse<Map<String, Object>> listRoles(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(adminDataService.list("admin_role", null, page, pageSize, null));
    }

    @Operation(summary = "查询菜单列表")
    @GetMapping("/menus")
    public ApiResponse<Map<String, Object>> listMenus() {
        return ApiResponse.success(adminDataService.list("admin_menu", null, 1, 200, null));
    }

    @Operation(summary = "获取角色菜单")
    @GetMapping("/roles/{roleId}/menus")
    public ApiResponse<Map<String, Object>> getRoleMenus(@PathVariable Long roleId) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            var rows = adminDataService.jdbc().queryForList(
                "SELECT menu_id FROM admin_role_menu WHERE role_id = ?", roleId);
            var menuIds = rows.stream().map(r -> ((Number) r.get("menu_id")).longValue()).toList();
            result.put("menu_ids", menuIds);
        } catch (Exception e) {
            result.put("menu_ids", java.util.Collections.emptyList());
            result.put("error", e.getMessage());
        }
        return ApiResponse.success(result);
    }

    @Operation(summary = "保存角色菜单")
    @PostMapping("/roles/{roleId}/menus")
    public ApiResponse<Map<String, Object>> saveRoleMenus(@PathVariable Long roleId, @RequestBody Map<String, Object> payload) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            @SuppressWarnings("unchecked")
            var menuIds = (List<Integer>) payload.get("menu_ids");
            if (menuIds == null) menuIds = java.util.Collections.emptyList();
            adminDataService.jdbc().update("DELETE FROM admin_role_menu WHERE role_id = ?", roleId);
            for (Integer mid : menuIds) {
                adminDataService.jdbc().update(
                    "INSERT IGNORE INTO admin_role_menu (role_id, menu_id) VALUES (?, ?)", roleId, mid);
            }
            result.put("ok", true);
            result.put("count", menuIds.size());
        } catch (Exception e) {
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return ApiResponse.success(result);
    }

    @Operation(summary = "设置用户角色")
    @PostMapping("/users/{userId}/roles")
    public ApiResponse<Map<String, Object>> setUserRoles(@PathVariable Long userId, @RequestBody Map<String, Object> payload) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            @SuppressWarnings("unchecked")
            var roleIds = (List<Integer>) payload.get("role_ids");
            if (roleIds == null) roleIds = java.util.Collections.emptyList();
            adminDataService.jdbc().update("DELETE FROM admin_user_role WHERE user_id = ?", userId);
            for (Integer rid : roleIds) {
                adminDataService.jdbc().update(
                    "INSERT IGNORE INTO admin_user_role (user_id, role_id) VALUES (?, ?)", userId, rid);
            }
            result.put("ok", true);
            result.put("count", roleIds.size());
        } catch (Exception e) {
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return ApiResponse.success(result);
    }

    @Operation(summary = "获取用户角色")
    @GetMapping("/users/{userId}/roles")
    public ApiResponse<Map<String, Object>> getUserRoles(@PathVariable Long userId) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            var rows = adminDataService.jdbc().queryForList(
                "SELECT role_id FROM admin_user_role WHERE user_id = ?", userId);
            var roleIds = rows.stream().map(r -> ((Number) r.get("role_id")).longValue()).toList();
            result.put("roleIds", roleIds);
        } catch (Exception e) {
            result.put("roleIds", java.util.Collections.emptyList());
            result.put("error", e.getMessage());
        }
        return ApiResponse.success(result);
    }

    @Operation(summary = "查询审计日志")
    @GetMapping("/audit-logs")
    public ApiResponse<Map<String, Object>> listAuditLogs(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) String module,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request) {
        Map<String, Object> adminInfo = adminHelper.getCurrentAdminUser(request);
        Long adminUserId = (Long) adminInfo.get("userId");
        DataScope dataScope = (DataScope) adminInfo.get("dataScope");
        return ApiResponse.success(adminDataService.audit(userId, operation, module, page, pageSize, dataScope, adminUserId));
    }

    @Operation(summary = "查询检索反馈")
    @GetMapping("/search-feedback")
    public ApiResponse<Map<String, Object>> listSearchFeedback(
            @RequestParam(required = false) Long articleId,
            @RequestParam(required = false) Integer isHelpful,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(adminDataService.listSearchFeedback(articleId, isHelpful, startDate, endDate, page, pageSize));
    }

    @Operation(summary = "检索反馈统计")
    @GetMapping("/search-feedback/stats")
    public ApiResponse<Map<String, Object>> searchFeedbackStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ApiResponse.success(adminDataService.searchFeedbackStats(startDate, endDate));
    }

    @Operation(summary = "查询法律收藏")
    @GetMapping("/law-favorites")
    public ApiResponse<Map<String, Object>> listLawFavorites(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Long articleId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(adminDataService.listLawFavorites(userId, username, articleId, page, pageSize));
    }

    @Operation(summary = "删除法律收藏")
    @DeleteMapping("/law-favorites/{id}")
    public ApiResponse<Map<String, Object>> deleteLawFavorite(@PathVariable Long id) {
        return ApiResponse.success(adminDataService.deleteLawFavorite(id));
    }

    @GetMapping("/law-favorites/stats")
    public ApiResponse<Map<String, Object>> lawFavoriteStats() {
        return ApiResponse.success(adminDataService.lawFavoriteStats());
    }

    @Operation(summary = "导出审计日志")
    @RateLimit(qps = 10, key = "admin_export")
    @GetMapping("/audit-logs/export")
    public void exportAuditLogs(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) String module,
            HttpServletResponse response) throws IOException {
        adminHelper.recordAudit("EXPORT", "audit_log", null);
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=audit-logs.csv");
        OutputStreamWriter os = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8);
        BufferedWriter writer = new BufferedWriter(os);
        writer.write("ID,用户名,操作,模块,对象类型,对象ID,请求URL,请求方法,IP,耗时,状态,时间");
        writer.newLine();
        int page = 1;
        int pageSize = 1000;
        int totalWritten = 0;
        int maxExport = 10000;
        while (totalWritten < maxExport) {
            Map<String, Object> result = adminDataService.audit(userId, operation, module, page, pageSize, null, null);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rows = (List<Map<String, Object>>) result.get("rows");
            if (rows == null || rows.isEmpty()) break;
            for (Map<String, Object> row : rows) {
                writer.write(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                        row.get("id"),
                        adminHelper.escapeCsv(row.get("username")),
                        adminHelper.escapeCsv(row.get("operation")),
                        adminHelper.escapeCsv(row.get("biz_module")),
                        adminHelper.escapeCsv(row.get("biz_type")),
                        adminHelper.escapeCsv(row.get("biz_id")),
                        adminHelper.escapeCsv(row.get("request_url")),
                        row.get("request_method"),
                        adminHelper.escapeCsv(row.get("ip")),
                        row.get("duration_ms"),
                        row.get("status"),
                        row.get("created_at")
                ));
                writer.newLine();
                totalWritten++;
            }
            page++;
            if (rows.size() < pageSize) break;
        }
        writer.flush();
    }

    @Operation(summary = "查询前端用户")
    @GetMapping("/frontend-users")
    public ApiResponse<Map<String, Object>> listFrontendUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        Map<String, Object> adminInfo = adminHelper.getCurrentAdminUser(request);
        Long adminUserId = (Long) adminInfo.get("userId");
        DataScope dataScope = (DataScope) adminInfo.get("dataScope");
        return ApiResponse.success(adminDataService.listFrontendUsers(adminUserId, dataScope, page, pageSize, keyword));
    }

    @PostMapping("/frontend-users")
    public ApiResponse<Map<String, Object>> createFrontendUser(@RequestBody Map<String, Object> payload) {
        Map<String, Object> data = adminDataService.createFrontendUser(payload);
        adminHelper.recordAudit("CREATE", "frontend_user", String.valueOf(data.get("id")));
        return ApiResponse.success(data);
    }

    @PutMapping("/frontend-users/{id}")
    public ApiResponse<Map<String, Object>> updateFrontendUser(@PathVariable String id, @RequestBody Map<String, Object> payload) {
        Map<String, Object> data = adminDataService.updateFrontendUser(id, payload);
        adminHelper.recordAudit("UPDATE", "frontend_user", id);
        return ApiResponse.success(data);
    }

    @DeleteMapping("/frontend-users/{id}")
    public ApiResponse<Map<String, Object>> deleteFrontendUser(@PathVariable String id) {
        Map<String, Object> data = adminDataService.deleteFrontendUser(id);
        adminHelper.recordAudit("DELETE", "frontend_user", id);
        return ApiResponse.success(data);
    }

    @PostMapping("/frontend-users/{id}/toggle")
    public ApiResponse<Map<String, Object>> toggleFrontendUserStatus(@PathVariable String id) {
        Map<String, Object> data = adminDataService.toggleFrontendUserStatus(id);
        adminHelper.recordAudit("TOGGLE", "frontend_user", id);
        return ApiResponse.success(data);
    }

    @GetMapping("/frontend-users/pending")
    public ApiResponse<Map<String, Object>> listPendingApprovals() {
        return ApiResponse.success(adminDataService.listPendingApprovals());
    }

    @PostMapping("/frontend-users/{id}/approve")
    public ApiResponse<Map<String, Object>> approveFrontendUser(@PathVariable String id) {
        Map<String, Object> data = adminDataService.approveFrontendUser(id);
        adminHelper.recordAudit("APPROVE", "frontend_user", id);
        return ApiResponse.success(data);
    }

    @PostMapping("/frontend-users/{id}/reject")
    public ApiResponse<Map<String, Object>> rejectFrontendUser(@PathVariable String id) {
        Map<String, Object> data = adminDataService.rejectFrontendUser(id);
        adminHelper.recordAudit("REJECT", "frontend_user", id);
        return ApiResponse.success(data);
    }

    @Operation(summary = "查询公告列表")
    @GetMapping("/announcements")
    public ApiResponse<Map<String, Object>> listAnnouncements(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(adminDataService.listAnnouncements(page, pageSize, keyword));
    }

    @PostMapping("/announcements")
    public ApiResponse<Map<String, Object>> createAnnouncement(@RequestBody Map<String, Object> payload) {
        Map<String, Object> data = adminDataService.createAnnouncement(payload);
        adminHelper.recordAudit("CREATE", "announcement", String.valueOf(data.get("id")));
        return ApiResponse.success(data);
    }

    @PutMapping("/announcements/{id}")
    public ApiResponse<Map<String, Object>> updateAnnouncement(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Map<String, Object> data = adminDataService.updateAnnouncement(id, payload);
        adminHelper.recordAudit("UPDATE", "announcement", String.valueOf(id));
        return ApiResponse.success(data);
    }

    @DeleteMapping("/announcements/{id}")
    public ApiResponse<Map<String, Object>> deleteAnnouncement(@PathVariable Long id) {
        Map<String, Object> data = adminDataService.deleteAnnouncement(id);
        adminHelper.recordAudit("DELETE", "announcement", String.valueOf(id));
        return ApiResponse.success(data);
    }

    @GetMapping("/announcements/active")
    public ApiResponse<Map<String, Object>> listActiveAnnouncements() {
        return ApiResponse.success(adminDataService.listActiveAnnouncements());
    }

    @Operation(summary = "查询字典列表")
    @GetMapping("/dicts/list")
    public ApiResponse<Map<String, Object>> listDicts(@RequestParam(required = false) String dict_type) {
        return ApiResponse.success(adminDataService.listDicts(dict_type));
    }

    @PostMapping("/dicts")
    public ApiResponse<Map<String, Object>> createDict(@RequestBody Map<String, Object> payload) {
        Map<String, Object> data = adminDataService.createDict(payload);
        adminHelper.recordAudit("CREATE", "sys_dict", String.valueOf(data.get("id")));
        return ApiResponse.success(data);
    }

    @PutMapping("/dicts/{id}")
    public ApiResponse<Map<String, Object>> updateDict(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Map<String, Object> data = adminDataService.updateDict(id, payload);
        adminHelper.recordAudit("UPDATE", "sys_dict", String.valueOf(id));
        return ApiResponse.success(data);
    }

    @DeleteMapping("/dicts/{id}")
    public ApiResponse<Map<String, Object>> deleteDict(@PathVariable Long id) {
        Map<String, Object> data = adminDataService.deleteDict(id);
        adminHelper.recordAudit("DELETE", "sys_dict", String.valueOf(id));
        return ApiResponse.success(data);
    }

    @Operation(summary = "MySQL健康检查")
    @GetMapping("/mysql-health")
    public ApiResponse<Map<String, Object>> mysqlHealth() {
        Map<String, Object> result = new LinkedHashMap<>();
        long start = System.currentTimeMillis();
        try {
            Integer dbOk = adminDataService.jdbc().queryForObject("SELECT 1", Integer.class);
            boolean ok = dbOk != null && dbOk == 1;
            result.put("status", ok ? "UP" : "DOWN");
            result.put("latencyMs", System.currentTimeMillis() - start);
            result.put("message", ok ? "MySQL 连接正常" : "MySQL 连接失败");
        } catch (Exception e) {
            result.put("status", "DOWN");
            result.put("latencyMs", System.currentTimeMillis() - start);
            result.put("message", "MySQL 连接失败: " + e.getMessage());
        }
        return ApiResponse.success(result);
    }

    @Operation(summary = "Redis健康检查")
    @GetMapping("/redis-health")
    public ApiResponse<Map<String, Object>> redisHealth() {
        Map<String, Object> result = new LinkedHashMap<>();
        long start = System.currentTimeMillis();
        try {
            if (cacheService != null && cacheService.isRedisAvailable()) {
                result.put("status", "UP");
                result.put("latencyMs", System.currentTimeMillis() - start);
                result.put("message", "Redis 连接正常");
            } else {
                result.put("status", "DOWN");
                result.put("latencyMs", System.currentTimeMillis() - start);
                result.put("message", "Redis 未配置或不可用");
            }
        } catch (Exception e) {
            result.put("status", "DOWN");
            result.put("latencyMs", System.currentTimeMillis() - start);
            result.put("message", "Redis 连接失败: " + e.getMessage());
        }
        return ApiResponse.success(result);
    }

    @Operation(summary = "Elasticsearch健康检查")
    @GetMapping("/es-health")
    public ApiResponse<Map<String, Object>> esHealth() {
        return ApiResponse.success(adminDataService.esHealth());
    }
}
