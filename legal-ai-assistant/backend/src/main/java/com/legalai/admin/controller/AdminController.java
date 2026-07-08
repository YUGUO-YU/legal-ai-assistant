package com.legalai.admin.controller;

import com.legalai.admin.enums.DataScope;
import com.legalai.admin.service.AdminDataService;
import com.legalai.admin.service.AlertMonitorService;
import com.legalai.admin.service.LawCategoryService;
import com.legalai.dto.ApiResponse;
import com.legalai.model.LawCategory;
import com.legalai.model.LawCategoryType;
import com.legalai.service.CacheService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台管理统一 API 入口。
 * 6 大域全部收口到 /admin/* 路径下，便于前端按需调用。
 */
@RestController
@RequestMapping("/api/v1/admin")
@CrossOrigin
public class AdminController {
    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private AdminDataService adminDataService;

    @Autowired
    private LawCategoryService lawCategoryService;

    @Autowired
    private AlertMonitorService alertMonitorService;

    @Autowired(required = false)
    private CacheService cacheService;

    // ============================================================
    // 通用：列表 / 详情 / 统计
    // ============================================================

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        return ApiResponse.success(adminDataService.stats());
    }

    @GetMapping("/stats/user-activity")
    public ApiResponse<Map<String, Object>> userActivityStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ApiResponse.success(adminDataService.userActivityStats(startDate, endDate));
    }

    @GetMapping("/stats/law-usage")
    public ApiResponse<Map<String, Object>> lawUsageStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "10") int topN) {
        return ApiResponse.success(adminDataService.lawUsageStats(startDate, endDate, topN));
    }

    @GetMapping("/db/health")
    public ApiResponse<Map<String, Object>> dbHealth() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            Long count = adminDataService.jdbc().queryForObject("SELECT 1", Long.class);
            result.put("status", "ok");
            result.put("connected", true);
            result.put("message", "数据库连接正常");
        } catch (Exception e) {
            result.put("status", "error");
            result.put("connected", false);
            result.put("message", "数据库连接失败: " + e.getMessage());
        }
        return ApiResponse.success(result);
    }

    @GetMapping("/db/tables")
    public ApiResponse<Map<String, Object>> dbTables() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            var rows = adminDataService.jdbc().queryForList("SHOW TABLES");
            result.put("tables", rows);
            result.put("count", rows.size());
            result.put("status", "ok");
        } catch (Exception e) {
            result.put("status", "error");
            result.put("error", e.getMessage());
        }
        return ApiResponse.success(result);
    }

    @GetMapping("/{table}/list")
    public ApiResponse<Map<String, Object>> list(
            @PathVariable String table,
            @RequestParam(required = false) String module,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword) {
        Map<String, Object> data = adminDataService.list(table, module, page, pageSize, keyword);
        recordAudit("LIST", table, module);
        return ApiResponse.success(data);
    }

    @GetMapping("/{table}/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable String table, @PathVariable Long id) {
        Map<String, Object> data = adminDataService.detail(table, id);
        recordAudit("DETAIL", table, String.valueOf(id));
        return ApiResponse.success(data);
    }

    @PostMapping("/{table}/create")
    public ApiResponse<Map<String, Object>> create(@PathVariable String table, @RequestBody Map<String, Object> payload) {
        Map<String, Object> data = adminDataService.create(table, payload);
        recordAudit("CREATE", table, String.valueOf(data.get("id")));
        return ApiResponse.success(data);
    }

    @PostMapping("/{table}/{id}/update")
    public ApiResponse<Map<String, Object>> update(@PathVariable String table, @PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Map<String, Object> data = adminDataService.update(table, id, payload);
        recordAudit("UPDATE", table, String.valueOf(id));
        return ApiResponse.success(data);
    }

    @PostMapping("/{table}/{id}/delete")
    public ApiResponse<Map<String, Object>> delete(@PathVariable String table, @PathVariable Long id) {
        Map<String, Object> data = adminDataService.delete(table, id);
        recordAudit("DELETE", table, String.valueOf(id));
        return ApiResponse.success(data);
    }

    @PostMapping("/{table}/{id}/toggle")
    public ApiResponse<Map<String, Object>> toggle(@PathVariable String table, @PathVariable Long id, @RequestParam String column) {
        Map<String, Object> data = adminDataService.toggle(table, id, column);
        recordAudit("TOGGLE", table, String.valueOf(id));
        return ApiResponse.success(data);
    }

    @PostMapping("/{table}/batch-delete")
    public ApiResponse<Map<String, Object>> batchDelete(@PathVariable String table, @RequestBody Map<String, Object> data) {
        @SuppressWarnings("unchecked")
        List<Long> ids = ((List<Number>) data.get("ids")).stream().map(Number::longValue).toList();
        recordAudit("BATCH_DELETE", table, String.join(",", ids.stream().map(String::valueOf).toList()));
        return ApiResponse.success(adminDataService.batchDelete(table, ids));
    }

    @PostMapping("/{table}/batch-toggle")
    public ApiResponse<Map<String, Object>> batchToggle(@PathVariable String table, @RequestBody Map<String, Object> data) {
        @SuppressWarnings("unchecked")
        List<Long> ids = ((List<Number>) data.get("ids")).stream().map(Number::longValue).toList();
        Integer status = ((Number) data.get("status")).intValue();
        recordAudit("BATCH_TOGGLE", table, String.join(",", ids.stream().map(String::valueOf).toList()) + " -> " + status);
        return ApiResponse.success(adminDataService.batchToggle(table, ids, status));
    }

    // ============================================================
    // 工作流：审核/告警/Prompt
    // ============================================================

    @PostMapping("/biz/mod01/laws/{id}/audit")
    public ApiResponse<Map<String, Object>> auditLaw(@PathVariable Long id, @RequestParam int action, @RequestParam(required = false) Long auditorId) {
        return ApiResponse.success(adminDataService.auditLaw(id, action, auditorId));
    }

    @PostMapping("/biz/mod03/drafts/{id}/review")
    public ApiResponse<Map<String, Object>> reviewDraft(@PathVariable Long id, @RequestParam int action, @RequestParam(required = false) Long reviewerId, @RequestBody(required = false) Map<String, Object> body) {
        String note = body == null ? null : String.valueOf(body.getOrDefault("note", null));
        return ApiResponse.success(adminDataService.reviewDraft(id, action, reviewerId, note));
    }

    @PostMapping("/monitor/alert-history/{id}/ack")
    public ApiResponse<Map<String, Object>> ackAlert(@PathVariable Long id, @RequestParam(required = false) Long handlerId) {
        return ApiResponse.success(adminDataService.ackAlert(id, handlerId));
    }

    @PostMapping("/monitor/alert-history/{id}/resolve")
    public ApiResponse<Map<String, Object>> resolveAlert(@PathVariable Long id) {
        return ApiResponse.success(adminDataService.resolveAlert(id));
    }

    @PostMapping("/ai/prompts/{id}/publish")
    public ApiResponse<Map<String, Object>> publishPrompt(@PathVariable Long id) {
        recordAudit("PUBLISH", "prompt_template", String.valueOf(id));
        return ApiResponse.success(adminDataService.publishPrompt(id));
    }

    @PostMapping("/ai/prompts/{id}/gray")
    public ApiResponse<Map<String, Object>> grayPrompt(@PathVariable Long id, @RequestParam int ratio, @RequestParam(required = false) String teams) {
        recordAudit("GRAY", "prompt_template", String.valueOf(id));
        return ApiResponse.success(adminDataService.grayPrompt(id, ratio, teams));
    }

    @PostMapping("/ai/prompts/{id}/rollback")
    public ApiResponse<Map<String, Object>> rollbackPrompt(@PathVariable Long id, @RequestParam(required = false) String reason) {
        recordAudit("ROLLBACK", "prompt_template", String.valueOf(id));
        return ApiResponse.success(adminDataService.rollbackPrompt(id, reason));
    }

    // ============================================================
    // PROMPT_TEMPLATE CRUD (alias endpoints for frontend compatibility)
    // ============================================================

    @GetMapping("/prompt_template/list")
    public ApiResponse<Map<String, Object>> listPromptTemplates(
            @RequestParam(required = false) String module,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(adminDataService.list("prompt_template", module, page, pageSize, keyword));
    }

    @GetMapping("/prompt_template/{id}")
    public ApiResponse<Map<String, Object>> promptTemplateDetail(@PathVariable Long id) {
        return ApiResponse.success(adminDataService.detail("prompt_template", id));
    }

    @PostMapping("/prompt_template/create")
    public ApiResponse<Map<String, Object>> createPromptTemplate(@RequestBody Map<String, Object> data) {
        recordAudit("create", "prompt_template", null);
        return ApiResponse.success(adminDataService.createPromptTemplate(data));
    }

    @PostMapping("/prompt_template/{id}/update")
    public ApiResponse<Map<String, Object>> updatePromptTemplate(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        recordAudit("update", "prompt_template", String.valueOf(id));
        return ApiResponse.success(adminDataService.updatePromptTemplate(id, data));
    }

    @PostMapping("/prompt_template/{id}/delete")
    public ApiResponse<Map<String, Object>> deletePromptTemplate(@PathVariable Long id) {
        recordAudit("delete", "prompt_template", String.valueOf(id));
        return ApiResponse.success(adminDataService.deletePromptTemplate(id));
    }

    // ============================================================
    // 监控概览 + LLM 健康 + Milvus 状态
    // ============================================================

    @GetMapping("/monitor/overview")
    public ApiResponse<Map<String, Object>> monitorOverview() {
        return ApiResponse.success(adminDataService.monitorOverview());
    }

    @PostMapping("/ai/llm-models/health-check")
    public ApiResponse<Map<String, Object>> llmHealthCheck() {
        return ApiResponse.success(adminDataService.llmHealthCheck());
    }

    @GetMapping("/ai/milvus/collections")
    public ApiResponse<Map<String, Object>> milvusCollections() {
        return ApiResponse.success(adminDataService.milvusCollections());
    }

    // ============================================================
    // 域 01 基础设施：用户/角色/菜单/审计
    // ============================================================

    @GetMapping("/infra/users")
    public ApiResponse<Map<String, Object>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(adminDataService.list("admin_user", null, page, pageSize, keyword));
    }

    @GetMapping("/infra/roles")
    public ApiResponse<Map<String, Object>> listRoles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(adminDataService.list("admin_role", null, page, pageSize, null));
    }

    @GetMapping("/infra/menus")
    public ApiResponse<Map<String, Object>> listMenus() {
        return ApiResponse.success(adminDataService.list("admin_menu", null, 1, 200, null));
    }

    @GetMapping("/infra/roles/{roleId}/menus")
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

    @PostMapping("/infra/roles/{roleId}/menus")
    public ApiResponse<Map<String, Object>> saveRoleMenus(
            @PathVariable Long roleId,
            @RequestBody Map<String, Object> payload) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            @SuppressWarnings("unchecked")
            var menuIds = (java.util.List<Integer>) payload.get("menu_ids");
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

    @PostMapping("/infra/users/{userId}/roles")
    public ApiResponse<Map<String, Object>> setUserRoles(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> payload) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            @SuppressWarnings("unchecked")
            var roleIds = (java.util.List<Integer>) payload.get("role_ids");
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

    @GetMapping("/infra/users/{userId}/roles")
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

    @GetMapping("/infra/audit-logs")
    public ApiResponse<Map<String, Object>> listAuditLogs(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) String module,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request) {
        Map<String, Object> adminInfo = getCurrentAdminUser(request);
        Long adminUserId = (Long) adminInfo.get("userId");
        DataScope dataScope = (DataScope) adminInfo.get("dataScope");
        return ApiResponse.success(adminDataService.audit(userId, operation, module, page, pageSize, dataScope, adminUserId));
    }

    @GetMapping("/infra/search-feedback")
    public ApiResponse<Map<String, Object>> listSearchFeedback(
            @RequestParam(required = false) Long articleId,
            @RequestParam(required = false) Integer isHelpful,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(adminDataService.listSearchFeedback(articleId, isHelpful, startDate, endDate, page, pageSize));
    }

    @GetMapping("/infra/search-feedback/stats")
    public ApiResponse<Map<String, Object>> searchFeedbackStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ApiResponse.success(adminDataService.searchFeedbackStats(startDate, endDate));
    }

    @GetMapping("/infra/law-favorites")
    public ApiResponse<Map<String, Object>> listLawFavorites(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Long articleId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(adminDataService.listLawFavorites(userId, username, articleId, page, pageSize));
    }

    @DeleteMapping("/infra/law-favorites/{id}")
    public ApiResponse<Map<String, Object>> deleteLawFavorite(@PathVariable Long id) {
        return ApiResponse.success(adminDataService.deleteLawFavorite(id));
    }

    @GetMapping("/infra/law-favorites/stats")
    public ApiResponse<Map<String, Object>> lawFavoriteStats() {
        return ApiResponse.success(adminDataService.lawFavoriteStats());
    }

    @GetMapping(value = "/infra/audit-logs/export", produces = "text/csv")
    public String exportAuditLogs(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) String module) {
        recordAudit("EXPORT", "audit_log", null);
        return adminDataService.exportAuditLogsCsv(userId, operation, module);
    }

    @GetMapping(value = "/monitor/alert-rules/export", produces = "text/csv")
    public String exportAlertRules() {
        recordAudit("EXPORT", "alert_rule", null);
        return adminDataService.exportAlertRulesCsv();
    }

    @GetMapping(value = "/monitor/alert-history/export", produces = "text/csv")
    public String exportAlertHistory() {
        recordAudit("EXPORT", "alert_history", null);
        return adminDataService.exportAlertHistoryCsv();
    }

    @GetMapping(value = "/ops/search-logs/export", produces = "text/csv")
    public String exportSearchLogs() {
        recordAudit("EXPORT", "search_log", null);
        return adminDataService.exportSearchLogsCsv();
    }

    @GetMapping(value = "/ops/user-feedback/export", produces = "text/csv")
    public String exportUserFeedback() {
        recordAudit("EXPORT", "user_feedback", null);
        return adminDataService.exportUserFeedbackCsv();
    }

    @GetMapping("/infra/frontend-users")
    public ApiResponse<Map<String, Object>> listFrontendUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        Map<String, Object> adminInfo = getCurrentAdminUser(request);
        Long adminUserId = (Long) adminInfo.get("userId");
        DataScope dataScope = (DataScope) adminInfo.get("dataScope");
        return ApiResponse.success(adminDataService.listFrontendUsers(adminUserId, dataScope, page, pageSize, keyword));
    }

    @PostMapping("/infra/frontend-users")
    public ApiResponse<Map<String, Object>> createFrontendUser(@RequestBody Map<String, Object> payload) {
        Map<String, Object> data = adminDataService.createFrontendUser(payload);
        recordAudit("CREATE", "frontend_user", String.valueOf(data.get("id")));
        return ApiResponse.success(data);
    }

    @PutMapping("/infra/frontend-users/{id}")
    public ApiResponse<Map<String, Object>> updateFrontendUser(@PathVariable String id, @RequestBody Map<String, Object> payload) {
        Map<String, Object> data = adminDataService.updateFrontendUser(id, payload);
        recordAudit("UPDATE", "frontend_user", id);
        return ApiResponse.success(data);
    }

    @DeleteMapping("/infra/frontend-users/{id}")
    public ApiResponse<Map<String, Object>> deleteFrontendUser(@PathVariable String id) {
        Map<String, Object> data = adminDataService.deleteFrontendUser(id);
        recordAudit("DELETE", "frontend_user", id);
        return ApiResponse.success(data);
    }

    @PostMapping("/infra/frontend-users/{id}/toggle")
    public ApiResponse<Map<String, Object>> toggleFrontendUserStatus(@PathVariable String id) {
        Map<String, Object> data = adminDataService.toggleFrontendUserStatus(id);
        recordAudit("TOGGLE", "frontend_user", id);
        return ApiResponse.success(data);
    }

    @GetMapping("/infra/frontend-users/pending")
    public ApiResponse<Map<String, Object>> listPendingApprovals() {
        return ApiResponse.success(adminDataService.listPendingApprovals());
    }

    @PostMapping("/infra/frontend-users/{id}/approve")
    public ApiResponse<Map<String, Object>> approveFrontendUser(@PathVariable String id) {
        Map<String, Object> data = adminDataService.approveFrontendUser(id);
        recordAudit("APPROVE", "frontend_user", id);
        return ApiResponse.success(data);
    }

    @PostMapping("/infra/frontend-users/{id}/reject")
    public ApiResponse<Map<String, Object>> rejectFrontendUser(@PathVariable String id) {
        Map<String, Object> data = adminDataService.rejectFrontendUser(id);
        recordAudit("REJECT", "frontend_user", id);
        return ApiResponse.success(data);
    }

    @GetMapping("/infra/announcements")
    public ApiResponse<Map<String, Object>> listAnnouncements(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(adminDataService.listAnnouncements(page, pageSize, keyword));
    }

    @PostMapping("/infra/announcements")
    public ApiResponse<Map<String, Object>> createAnnouncement(@RequestBody Map<String, Object> payload) {
        Map<String, Object> data = adminDataService.createAnnouncement(payload);
        recordAudit("CREATE", "announcement", String.valueOf(data.get("id")));
        return ApiResponse.success(data);
    }

    @PutMapping("/infra/announcements/{id}")
    public ApiResponse<Map<String, Object>> updateAnnouncement(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Map<String, Object> data = adminDataService.updateAnnouncement(id, payload);
        recordAudit("UPDATE", "announcement", String.valueOf(id));
        return ApiResponse.success(data);
    }

    @DeleteMapping("/infra/announcements/{id}")
    public ApiResponse<Map<String, Object>> deleteAnnouncement(@PathVariable Long id) {
        Map<String, Object> data = adminDataService.deleteAnnouncement(id);
        recordAudit("DELETE", "announcement", String.valueOf(id));
        return ApiResponse.success(data);
    }

    @GetMapping("/announcements/active")
    public ApiResponse<Map<String, Object>> listActiveAnnouncements() {
        return ApiResponse.success(adminDataService.listActiveAnnouncements());
    }

    @GetMapping("/infra/dicts/list")
    public ApiResponse<Map<String, Object>> listDicts(@RequestParam(required = false) String dict_type) {
        return ApiResponse.success(adminDataService.listDicts(dict_type));
    }

    @PostMapping("/infra/dicts")
    public ApiResponse<Map<String, Object>> createDict(@RequestBody Map<String, Object> payload) {
        Map<String, Object> data = adminDataService.createDict(payload);
        recordAudit("CREATE", "sys_dict", String.valueOf(data.get("id")));
        return ApiResponse.success(data);
    }

    @PutMapping("/infra/dicts/{id}")
    public ApiResponse<Map<String, Object>> updateDict(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Map<String, Object> data = adminDataService.updateDict(id, payload);
        recordAudit("UPDATE", "sys_dict", String.valueOf(id));
        return ApiResponse.success(data);
    }

    @DeleteMapping("/infra/dicts/{id}")
    public ApiResponse<Map<String, Object>> deleteDict(@PathVariable Long id) {
        Map<String, Object> data = adminDataService.deleteDict(id);
        recordAudit("DELETE", "sys_dict", String.valueOf(id));
        return ApiResponse.success(data);
    }

    // ============================================================
    // 域 02 数据资产：MOD-01..MOD-10 各模块数据
    // ============================================================

    @GetMapping("/biz/mod01/laws")
    public ApiResponse<Map<String, Object>> mod01Laws(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(adminDataService.list("law_document", "MOD-01", page, pageSize, keyword));
    }

    @GetMapping("/biz/mod01/laws/{id}/revisions")
    public ApiResponse<Map<String, Object>> mod01LawRevisions(@PathVariable Long id) {
        return ApiResponse.success(adminDataService.listRevisionsByLawId(id));
    }

    @GetMapping("/biz/mod01/law-relations")
    public ApiResponse<Map<String, Object>> listLawRelations(
            @RequestParam(required = false) Long sourceArticleId,
            @RequestParam(required = false) Long targetArticleId,
            @RequestParam(required = false) String relationType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(adminDataService.listLawRelations(sourceArticleId, targetArticleId, relationType, page, pageSize));
    }

    @PostMapping("/biz/mod01/law-relations")
    public ApiResponse<Map<String, Object>> createLawRelation(@RequestBody Map<String, Object> data) {
        recordAudit("CREATE", "law_relation", null);
        return ApiResponse.success(adminDataService.createLawRelation(data));
    }

    @PutMapping("/biz/mod01/law-relations/{id}")
    public ApiResponse<Map<String, Object>> updateLawRelation(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        recordAudit("UPDATE", "law_relation", String.valueOf(id));
        return ApiResponse.success(adminDataService.updateLawRelation(id, data));
    }

    @DeleteMapping("/biz/mod01/law-relations/{id}")
    public ApiResponse<Map<String, Object>> deleteLawRelation(@PathVariable Long id) {
        recordAudit("DELETE", "law_relation", String.valueOf(id));
        return ApiResponse.success(adminDataService.deleteLawRelation(id));
    }

    @GetMapping("/biz/mod01/crawl-tasks")
    public ApiResponse<Map<String, Object>> mod01CrawlTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(adminDataService.list("crawl_task", null, page, pageSize, null));
    }

    @GetMapping("/biz/mod01/crawl-logs")
    public ApiResponse<Map<String, Object>> mod01CrawlLogs(
            @RequestParam Long taskId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(adminDataService.listCrawlLogs(taskId, page, pageSize));
    }

    @GetMapping("/biz/mod02/cases")
    public ApiResponse<Map<String, Object>> mod02Cases(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String cause,
            @RequestParam(required = false) Integer caseType,
            @RequestParam(required = false) Integer judgment) {
        return ApiResponse.success(adminDataService.listMod02Cases(page, pageSize, cause, caseType, judgment));
    }

    @GetMapping("/biz/mod02/case-elements")
    public ApiResponse<Map<String, Object>> mod02CaseElements() {
        return ApiResponse.success(adminDataService.list("case_element_dict", null, 1, 100, null));
    }

    @GetMapping("/biz/mod03/templates")
    public ApiResponse<Map<String, Object>> mod03Templates(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(adminDataService.list("doc_template", "MOD-03", page, pageSize, keyword));
    }

    @GetMapping("/biz/mod03/drafts")
    public ApiResponse<Map<String, Object>> mod03Drafts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(adminDataService.list("doc_draft", "MOD-03", page, pageSize, null));
    }

    @GetMapping("/biz/mod03/review-rules")
    public ApiResponse<Map<String, Object>> mod03ReviewRules() {
        return ApiResponse.success(adminDataService.list("doc_review_rule", null, 1, 100, null));
    }

    @GetMapping("/biz/mod04/research-tasks")
    public ApiResponse<Map<String, Object>> mod04Tasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(adminDataService.list("legal_research_task", "MOD-04", page, pageSize, null));
    }

    @GetMapping("/biz/mod04/research-tasks/{id}")
    public ApiResponse<Map<String, Object>> mod04TaskDetail(@PathVariable Long id) {
        return ApiResponse.success(adminDataService.getResearchTask(id));
    }

    @GetMapping("/biz/mod05/company-apis")
    public ApiResponse<Map<String, Object>> mod05CompanyApis() {
        return ApiResponse.success(adminDataService.list("company_api_config", null, 1, 100, null));
    }

    @GetMapping("/biz/mod06/case-search-logs")
    public ApiResponse<Map<String, Object>> mod06Logs() {
        return ApiResponse.success(adminDataService.list("search_log", "MOD-06", 1, 20, null));
    }

    @GetMapping("/biz/mod07/laws")
    public ApiResponse<Map<String, Object>> mod07Laws() {
        return ApiResponse.success(adminDataService.list("law_document", "MOD-07", 1, 20, null));
    }

    @GetMapping("/biz/mod08/contract-rules")
    public ApiResponse<Map<String, Object>> mod08ContractRules() {
        return ApiResponse.success(adminDataService.list("contract_review_rule", null, 1, 100, null));
    }

    @GetMapping("/biz/contract-reviews")
    public ApiResponse<Map<String, Object>> listContractReviews(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(adminDataService.listContractReviews(userId, riskLevel, page, pageSize));
    }

    @GetMapping("/biz/contract-reviews/{id}")
    public ApiResponse<Map<String, Object>> getContractReview(@PathVariable Long id) {
        return ApiResponse.success(adminDataService.getContractReview(id));
    }

    @GetMapping("/biz/mod09/kb-bases")
    public ApiResponse<Map<String, Object>> mod09KbBases() {
        return ApiResponse.success(adminDataService.list("kb_knowledge_base", "MOD-09", 1, 100, null));
    }

    @GetMapping("/biz/mod09/kb-strategies")
    public ApiResponse<Map<String, Object>> mod09Strategies() {
        return ApiResponse.success(adminDataService.list("kb_chunk_strategy", null, 1, 100, null));
    }

    @GetMapping("/biz/mod10/qa-sessions")
    public ApiResponse<Map<String, Object>> mod10QaSessions() {
        return ApiResponse.success(adminDataService.list("qa_session", "MOD-10", 1, 20, null));
    }

    // ============================================================
    // 域 03 AI 能力：Prompt / 模型 / Token 用量
    // ============================================================

    @GetMapping("/ai/prompts")
    public ApiResponse<Map<String, Object>> listPrompts(
            @RequestParam(required = false) String module,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(adminDataService.list("prompt_template", module, page, pageSize, keyword));
    }

    @GetMapping("/ai/prompts/{id}")
    public ApiResponse<Map<String, Object>> promptDetail(@PathVariable Long id) {
        return ApiResponse.success(adminDataService.detail("prompt_template", id));
    }

    @GetMapping("/ai/prompts/gray-releases")
    public ApiResponse<Map<String, Object>> grayReleases() {
        return ApiResponse.success(adminDataService.list("prompt_gray_release", null, 1, 50, null));
    }

    @GetMapping("/ai/llm-models")
    public ApiResponse<Map<String, Object>> llmModels() {
        return ApiResponse.success(adminDataService.list("llm_model_config", null, 1, 50, null));
    }

    @GetMapping("/ai/llm-models/summary")
    public ApiResponse<Map<String, Object>> llmModelsSummary() {
        return ApiResponse.success(adminDataService.llmModelsSummary());
    }

    @PostMapping("/ai/llm-models/{id}/set-active")
    public ApiResponse<Map<String, Object>> setActiveModel(@PathVariable Long id) {
        boolean ok = adminDataService.setActiveModel(id);
        if (ok) {
            return ApiResponse.success(Map.of("ok", true, "message", "已将当前模型切换到 id=" + id));
        }
        return ApiResponse.error(500, "设置失败");
    }

    @PostMapping("/ai/llm-models/{id}/update-key")
    public ApiResponse<Map<String, Object>> updateModelApiKey(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String apiKey = body.get("apiKey");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return ApiResponse.error(400, "API密钥不能为空");
        }
        boolean ok = adminDataService.updateModelApiKey(id, apiKey);
        if (ok) {
            return ApiResponse.success(Map.of("ok", true, "message", "API密钥已更新"));
        }
        return ApiResponse.error(500, "更新失败");
    }

    @PostMapping("/ai/llm-models")
    public ApiResponse<Map<String, Object>> createModelConfig(@RequestBody Map<String, Object> payload) {
        Map<String, Object> data = adminDataService.createModelConfig(payload);
        recordAudit("CREATE", "llm_model_config", String.valueOf(data.get("id")));
        return ApiResponse.success(data);
    }

    @PutMapping("/ai/llm-models/{id}")
    public ApiResponse<Map<String, Object>> updateModelConfig(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Map<String, Object> data = adminDataService.updateModelConfig(id, payload);
        recordAudit("UPDATE", "llm_model_config", String.valueOf(id));
        return ApiResponse.success(data);
    }

    @DeleteMapping("/ai/llm-models/{id}")
    public ApiResponse<Map<String, Object>> deleteModelConfig(@PathVariable Long id) {
        Map<String, Object> data = adminDataService.deleteModelConfig(id);
        recordAudit("DELETE", "llm_model_config", String.valueOf(id));
        return ApiResponse.success(data);
    }

    @GetMapping("/ai/token-usage")
    public ApiResponse<Map<String, Object>> tokenUsage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(adminDataService.list("llm_token_usage", null, page, pageSize, null));
    }

    @GetMapping("/ai/kb-chunks")
    public ApiResponse<Map<String, Object>> listKbChunks(
            @RequestParam(required = false) Long kbId,
            @RequestParam(required = false) String fileName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(adminDataService.listKbChunks(kbId, fileName, page, pageSize));
    }

    @GetMapping("/ai/kb-chunks/stats")
    public ApiResponse<Map<String, Object>> kbChunksStats() {
        return ApiResponse.success(adminDataService.kbChunksStats());
    }

    // ============================================================
    // 域 04 运营分析：反馈
    // ============================================================

    @GetMapping("/ops/user-feedback")
    public ApiResponse<Map<String, Object>> userFeedback(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request) {
        Map<String, Object> adminInfo = getCurrentAdminUser(request);
        Long adminUserId = (Long) adminInfo.get("userId");
        DataScope dataScope = (DataScope) adminInfo.get("dataScope");
        return ApiResponse.success(adminDataService.listUserFeedback(adminUserId, dataScope, page, pageSize));
    }

    @PostMapping("/ops/user-feedback/{id}/update")
    public ApiResponse<Map<String, Object>> updateUserFeedback(
            @PathVariable Long id,
            @RequestBody Map<String, Object> data) {
        recordAudit("UPDATE", "user_feedback", String.valueOf(id));
        return ApiResponse.success(adminDataService.updateUserFeedback(id, data));
    }

    @GetMapping("/ops/search-logs")
    public ApiResponse<Map<String, Object>> searchLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize,
            HttpServletRequest request) {
        Map<String, Object> adminInfo = getCurrentAdminUser(request);
        Long adminUserId = (Long) adminInfo.get("userId");
        DataScope dataScope = (DataScope) adminInfo.get("dataScope");
        return ApiResponse.success(adminDataService.listSearchLogs(adminUserId, dataScope, page, pageSize));
    }

    // ============================================================
    // 域 05 监控告警
    // ============================================================

    @GetMapping("/monitor/alert-rules")
    public ApiResponse<Map<String, Object>> alertRules() {
        return ApiResponse.success(adminDataService.list("alert_rule", null, 1, 100, null));
    }

    @PostMapping("/monitor/alert-rules")
    public ApiResponse<Map<String, Object>> createAlertRule(@RequestBody Map<String, Object> data) {
        recordAudit("CREATE", "alert_rule", null);
        return ApiResponse.success(adminDataService.createAlertRule(data));
    }

    @PutMapping("/monitor/alert-rules/{id}")
    public ApiResponse<Map<String, Object>> updateAlertRule(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        recordAudit("UPDATE", "alert_rule", String.valueOf(id));
        return ApiResponse.success(adminDataService.updateAlertRule(id, data));
    }

    @DeleteMapping("/monitor/alert-rules/{id}")
    public ApiResponse<Map<String, Object>> deleteAlertRule(@PathVariable Long id) {
        recordAudit("DELETE", "alert_rule", String.valueOf(id));
        return ApiResponse.success(adminDataService.deleteAlertRule(id));
    }

    @PostMapping("/monitor/alert-rules/{id}/toggle")
    public ApiResponse<Map<String, Object>> toggleAlertRule(@PathVariable Long id) {
        recordAudit("TOGGLE", "alert_rule", String.valueOf(id));
        return ApiResponse.success(adminDataService.toggleAlertRule(id));
    }

    @GetMapping("/monitor/alert-history")
    public ApiResponse<Map<String, Object>> alertHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(adminDataService.list("alert_history", null, page, pageSize, null));
    }

    @GetMapping("/alert_history/list")
    public ApiResponse<Map<String, Object>> alertHistoryList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(adminDataService.list("alert_history", null, page, pageSize, null));
    }

    @PostMapping("/monitor/alert-check")
    public ApiResponse<Map<String, Object>> triggerAlertCheck() {
        return ApiResponse.success(alertMonitorService.checkAlerts());
    }

    // ============================================================
    // 域 06 系统配置
    // ============================================================

    @GetMapping("/sys/configs")
    public ApiResponse<Map<String, Object>> sysConfigs(
            @RequestParam(required = false) String group,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize) {
        return ApiResponse.success(adminDataService.list("sys_config", group, page, pageSize, null));
    }

    @GetMapping("/sys/dicts")
    public ApiResponse<Map<String, Object>> sysDicts(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int pageSize) {
        return ApiResponse.success(adminDataService.list("sys_dict", type, page, pageSize, null));
    }

    @PostMapping("/sys/cache/refresh")
    public ApiResponse<Map<String, Object>> refreshCache() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            if (cacheService != null) {
                cacheService.invalidateSearchCache();
                result.put("ok", true);
                result.put("message", "缓存已刷新");
            } else {
                result.put("ok", true);
                result.put("message", "缓存服务不可用（Redis未配置）");
            }
        } catch (Exception e) {
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return ApiResponse.success(result);
    }

    // ============================================================
    // 健康检查
    // ============================================================

    @GetMapping("/infra/mysql-health")
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

    @GetMapping("/infra/redis-health")
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

    @GetMapping("/infra/es-health")
    public ApiResponse<Map<String, Object>> esHealth() {
        return ApiResponse.success(adminDataService.esHealth());
    }

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scope", "admin");
        result.put("timestamp", System.currentTimeMillis());
        result.put("domains", java.util.Arrays.asList(
                "infra", "biz(mod01-mod10)", "ai", "ops", "monitor", "sys"));
        try {
            Integer dbOk = adminDataService.jdbc().queryForObject("SELECT 1", Integer.class);
            result.put("status", dbOk != null && dbOk == 1 ? "UP" : "DOWN");
            result.put("database", "UP");
        } catch (Exception e) {
            result.put("status", "DOWN");
            result.put("database", "DOWN");
            result.put("db_error", e.getMessage());
        }
        return ApiResponse.success(result);
    }

    // ============================================================
    // 分类管理：类型 / 类别 / 文档分类关联
    // ============================================================

    @GetMapping("/law/category-types")
    public ApiResponse<List<Map<String, Object>>> listCategoryTypes() {
        return ApiResponse.success(lawCategoryService.listTypes());
    }

    @GetMapping("/law/category-types/{id}")
    public ApiResponse<LawCategoryType> getCategoryType(@PathVariable Long id) {
        return ApiResponse.<LawCategoryType>success(lawCategoryService.getType(id));
    }

    @PostMapping("/law/category-types")
    public ApiResponse<Void> createCategoryType(@RequestBody LawCategoryType type) {
        lawCategoryService.createType(type);
        return ApiResponse.success(null);
    }

    @PutMapping("/law/category-types/{id}")
    public ApiResponse<Void> updateCategoryType(@PathVariable Long id, @RequestBody LawCategoryType type) {
        lawCategoryService.updateType(id, type);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/law/category-types/{id}")
    public ApiResponse<Void> deleteCategoryType(@PathVariable Long id) {
        lawCategoryService.deleteType(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/law/categories")
    public ApiResponse<List<Map<String, Object>>> listCategories(@RequestParam(required = false) Long typeId) {
        return ApiResponse.success(lawCategoryService.listCategories(typeId));
    }

    @GetMapping("/law/categories/{id}")
    public ApiResponse<Map<String, Object>> getCategory(@PathVariable Long id) {
        return ApiResponse.<Map<String, Object>>success(lawCategoryService.getCategory(id));
    }

    @PostMapping("/law/categories")
    public ApiResponse<Void> createCategory(@RequestBody LawCategory category) {
        lawCategoryService.createCategory(category);
        return ApiResponse.success(null);
    }

    @PutMapping("/law/categories/{id}")
    public ApiResponse<Void> updateCategory(@PathVariable Long id, @RequestBody LawCategory category) {
        lawCategoryService.updateCategory(id, category);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/law/categories/{id}")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
        lawCategoryService.deleteCategory(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/law/document-categories/{lawId}")
    public ApiResponse<List<Map<String, Object>>> getDocumentCategories(@PathVariable Long lawId) {
        return ApiResponse.success(lawCategoryService.getDocumentCategories(lawId));
    }

    @PostMapping("/law/document-categories/{lawId}")
    public ApiResponse<Void> setDocumentCategories(@PathVariable Long lawId, @RequestBody Map<String, List<Long>> body) {
        lawCategoryService.setDocumentCategories(lawId, body.get("categoryIds"));
        return ApiResponse.success(null);
    }

    // ============================================================
    // DOC_TEMPLATE (Mod03) CRUD
    // ============================================================

    @GetMapping("/doc_template/list")
    public ApiResponse<Map<String, Object>> listDocTemplates(
            @RequestParam(required = false) String category) {
        return ApiResponse.success(adminDataService.listDocTemplates(category));
    }

    @PostMapping("/doc_template/create")
    public ApiResponse<Void> createDocTemplate(@RequestBody Map<String, Object> data) {
        adminDataService.createDocTemplate(data);
        recordAudit("create", "doc_template", null);
        return ApiResponse.success(null);
    }

    @PostMapping("/doc_template/{id}/update")
    public ApiResponse<Void> updateDocTemplate(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        adminDataService.updateDocTemplate(id, data);
        recordAudit("update", "doc_template", String.valueOf(id));
        return ApiResponse.success(null);
    }

    @PostMapping("/doc_template/{id}/delete")
    public ApiResponse<Void> deleteDocTemplate(@PathVariable Long id) {
        adminDataService.deleteDocTemplate(id);
        recordAudit("delete", "doc_template", String.valueOf(id));
        return ApiResponse.success(null);
    }

    @PostMapping("/doc_template/{id}/toggle")
    public ApiResponse<Void> toggleDocTemplate(@PathVariable Long id, @RequestBody Map<String, Integer> data) {
        adminDataService.toggleDocTemplate(id, data.get("status"));
        recordAudit("toggle", "doc_template", String.valueOf(id));
        return ApiResponse.success(null);
    }

    // ============================================================
    // DOC_REVIEW_RULE CRUD
    // ============================================================

    @PostMapping("/doc_review_rule/create")
    public ApiResponse<Void> createDocReviewRule(@RequestBody Map<String, Object> data) {
        adminDataService.createDocReviewRule(data);
        recordAudit("create", "doc_review_rule", null);
        return ApiResponse.success(null);
    }

    @PostMapping("/doc_review_rule/{id}/update")
    public ApiResponse<Void> updateDocReviewRule(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        adminDataService.updateDocReviewRule(id, data);
        recordAudit("update", "doc_review_rule", String.valueOf(id));
        return ApiResponse.success(null);
    }

    @PostMapping("/doc_review_rule/{id}/delete")
    public ApiResponse<Void> deleteDocReviewRule(@PathVariable Long id) {
        adminDataService.deleteDocReviewRule(id);
        recordAudit("delete", "doc_review_rule", String.valueOf(id));
        return ApiResponse.success(null);
    }

    @PostMapping("/doc_review_rule/{id}/toggle")
    public ApiResponse<Void> toggleDocReviewRule(@PathVariable Long id, @RequestBody Map<String, Integer> data) {
        adminDataService.toggleDocReviewRule(id, data.get("status"));
        recordAudit("toggle", "doc_review_rule", String.valueOf(id));
        return ApiResponse.success(null);
    }

    // ============================================================
    // helpers
    // ============================================================

    private Map<String, Object> getCurrentAdminUser(HttpServletRequest request) {
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

    private void recordAudit(String operation, String bizType, String bizId) {
        try {
            adminDataService.recordAudit(null, "system", operation, "ADMIN",
                    bizType, bizId, "/api/v1/admin", "GET", null, "ok", null, 0, true, null);
        } catch (Exception ignore) {
        }
    }
}