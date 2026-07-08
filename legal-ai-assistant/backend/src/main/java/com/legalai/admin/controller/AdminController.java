package com.legalai.admin.controller;

import com.legalai.admin.annotation.RateLimit;
import com.legalai.admin.enums.DataScope;
import com.legalai.admin.service.AdminDataService;
import com.legalai.admin.service.AlertMonitorService;
import com.legalai.admin.service.LawCategoryService;
import com.legalai.dto.ApiResponse;
import com.legalai.model.LawCategory;
import com.legalai.model.LawCategoryType;
import com.legalai.service.CacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 后台管理统一 API 入口。
 * 6 大域全部收口到 /admin/* 路径下，便于前端按需调用。
 */
@RestController
@RequestMapping("/api/v1/admin")
@CrossOrigin
@Tag(name = "管理后台", description = "后台管理统一API入口，包含基础设施、数据资产、AI能力、运营分析、监控告警、系统配置等模块")
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

    @Autowired
    private com.legalai.admin.service.ConfigRefreshService configRefreshService;

    @Autowired
    private com.legalai.admin.service.AppLogService appLogService;

    // ============================================================
    // 通用：列表 / 详情 / 统计
    // ============================================================

    @Operation(summary = "获取统计概览", description = "获取系统关键统计数据")
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        return ApiResponse.success(adminDataService.stats());
    }

    @Operation(summary = "用户活动统计", description = "按时间段统计用户活动情况")
    @GetMapping("/stats/user-activity")
    public ApiResponse<Map<String, Object>> userActivityStats(
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate) {
        return ApiResponse.success(adminDataService.userActivityStats(startDate, endDate));
    }

    @Operation(summary = "法规使用统计", description = "统计法规的查询和使用频次")
    @GetMapping("/stats/law-usage")
    public ApiResponse<Map<String, Object>> lawUsageStats(
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate,
            @Parameter(description = "返回前N条") @RequestParam(defaultValue = "10") int topN) {
        return ApiResponse.success(adminDataService.lawUsageStats(startDate, endDate, topN));
    }

    @Operation(summary = "数据库健康检查", description = "检查MySQL数据库连接状态")
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

    @Operation(summary = "查询数据库表", description = "获取MySQL数据库所有表列表")
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

    @Operation(summary = "通用列表查询", description = "分页查询指定表的数据列表")
    @RateLimit(qps = 100, key = "admin_list")
    @GetMapping("/{table}/list")
    public ApiResponse<Map<String, Object>> list(
            @Parameter(description = "表名") @PathVariable String table,
            @Parameter(description = "模块标识") @RequestParam(required = false) String module,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(description = "关键词搜索") @RequestParam(required = false) String keyword) {
        Map<String, Object> data = adminDataService.list(table, module, page, pageSize, keyword);
        recordAudit("LIST", table, module);
        return ApiResponse.success(data);
    }

    @Operation(summary = "通用详情查询", description = "根据ID获取指定表的单条数据详情")
    @GetMapping("/{table}/{id}")
    public ApiResponse<Map<String, Object>> detail(
            @Parameter(description = "表名") @PathVariable String table,
            @Parameter(description = "数据ID") @PathVariable Long id) {
        Map<String, Object> data = adminDataService.detail(table, id);
        recordAudit("DETAIL", table, String.valueOf(id));
        return ApiResponse.success(data);
    }

    @Operation(summary = "通用数据创建", description = "向指定表插入一条新数据")
    @RateLimit(qps = 20, key = "admin_write")
    @PostMapping("/{table}/create")
    public ApiResponse<Map<String, Object>> create(
            @Parameter(description = "表名") @PathVariable String table,
            @RequestBody Map<String, Object> payload) {
        Map<String, Object> data = adminDataService.create(table, payload);
        recordAudit("CREATE", table, String.valueOf(data.get("id")));
        return ApiResponse.success(data);
    }

    @Operation(summary = "通用数据更新", description = "更新指定表的单条数据")
    @PostMapping("/{table}/{id}/update")
    public ApiResponse<Map<String, Object>> update(
            @Parameter(description = "表名") @PathVariable String table,
            @Parameter(description = "数据ID") @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {
        Map<String, Object> data = adminDataService.update(table, id, payload);
        recordAudit("UPDATE", table, String.valueOf(id));
        return ApiResponse.success(data);
    }

    @Operation(summary = "通用数据删除", description = "删除指定表的单条数据")
    @PostMapping("/{table}/{id}/delete")
    public ApiResponse<Map<String, Object>> delete(
            @Parameter(description = "表名") @PathVariable String table,
            @Parameter(description = "数据ID") @PathVariable Long id) {
        Map<String, Object> data = adminDataService.delete(table, id);
        recordAudit("DELETE", table, String.valueOf(id));
        return ApiResponse.success(data);
    }

    @Operation(summary = "通用状态切换", description = "切换指定记录的某个布尔字段状态")
    @PostMapping("/{table}/{id}/toggle")
    public ApiResponse<Map<String, Object>> toggle(
            @Parameter(description = "表名") @PathVariable String table,
            @Parameter(description = "数据ID") @PathVariable Long id,
            @Parameter(description = "字段名") @RequestParam String column) {
        Map<String, Object> data = adminDataService.toggle(table, id, column);
        recordAudit("TOGGLE", table, String.valueOf(id));
        return ApiResponse.success(data);
    }

    @Operation(summary = "批量删除", description = "批量删除指定表的多条数据")
    @PostMapping("/{table}/batch-delete")
    public ApiResponse<Map<String, Object>> batchDelete(
            @Parameter(description = "表名") @PathVariable String table,
            @RequestBody Map<String, Object> data) {
        @SuppressWarnings("unchecked")
        List<Long> ids = ((List<Number>) data.get("ids")).stream().map(Number::longValue).toList();
        recordAudit("BATCH_DELETE", table, String.join(",", ids.stream().map(String::valueOf).toList()));
        return ApiResponse.success(adminDataService.batchDelete(table, ids));
    }

    @Operation(summary = "批量状态切换", description = "批量切换多条记录的启用状态")
    @PostMapping("/{table}/batch-toggle")
    public ApiResponse<Map<String, Object>> batchToggle(
            @Parameter(description = "表名") @PathVariable String table,
            @RequestBody Map<String, Object> data) {
        @SuppressWarnings("unchecked")
        List<Long> ids = ((List<Number>) data.get("ids")).stream().map(Number::longValue).toList();
        Integer status = ((Number) data.get("status")).intValue();
        recordAudit("BATCH_TOGGLE", table, String.join(",", ids.stream().map(String::valueOf).toList()) + " -> " + status);
        return ApiResponse.success(adminDataService.batchToggle(table, ids, status));
    }

    // ============================================================
    // 工作流：审核/告警/Prompt
    // ============================================================

    @Operation(summary = "法规审核", description = "对法规文档进行审核操作")
    @PostMapping("/biz/mod01/laws/{id}/audit")
    public ApiResponse<Map<String, Object>> auditLaw(
            @Parameter(description = "法规ID") @PathVariable Long id,
            @Parameter(description = "操作：1通过 2拒绝") @RequestParam int action,
            @Parameter(description = "审核人ID") @RequestParam(required = false) Long auditorId) {
        return ApiResponse.success(adminDataService.auditLaw(id, action, auditorId));
    }

    @Operation(summary = "文档审核", description = "对文档草稿进行审核操作")
    @PostMapping("/biz/mod03/drafts/{id}/review")
    public ApiResponse<Map<String, Object>> reviewDraft(
            @Parameter(description = "草稿ID") @PathVariable Long id,
            @Parameter(description = "操作：1通过 2拒绝") @RequestParam int action,
            @Parameter(description = "审核人ID") @RequestParam(required = false) Long reviewerId,
            @RequestBody(required = false) Map<String, Object> body) {
        String note = body == null ? null : String.valueOf(body.getOrDefault("note", null));
        return ApiResponse.success(adminDataService.reviewDraft(id, action, reviewerId, note));
    }

    @Operation(summary = "告警确认", description = "确认处理指定告警")
    @PostMapping("/monitor/alert-history/{id}/ack")
    public ApiResponse<Map<String, Object>> ackAlert(
            @Parameter(description = "告警ID") @PathVariable Long id,
            @Parameter(description = "处理人ID") @RequestParam(required = false) Long handlerId) {
        return ApiResponse.success(adminDataService.ackAlert(id, handlerId));
    }

    @Operation(summary = "告警解决", description = "标记指定告警为已解决")
    @PostMapping("/monitor/alert-history/{id}/resolve")
    public ApiResponse<Map<String, Object>> resolveAlert(
            @Parameter(description = "告警ID") @PathVariable Long id) {
        return ApiResponse.success(adminDataService.resolveAlert(id));
    }

    @Operation(summary = "发布Prompt模板", description = "将Prompt模板发布为正式版本")
    @PostMapping("/ai/prompts/{id}/publish")
    public ApiResponse<Map<String, Object>> publishPrompt(
            @Parameter(description = "Prompt模板ID") @PathVariable Long id) {
        recordAudit("PUBLISH", "prompt_template", String.valueOf(id));
        return ApiResponse.success(adminDataService.publishPrompt(id));
    }

    @Operation(summary = "灰度发布Prompt", description = "以灰度方式发布Prompt模板")
    @PostMapping("/ai/prompts/{id}/gray")
    public ApiResponse<Map<String, Object>> grayPrompt(
            @Parameter(description = "Prompt模板ID") @PathVariable Long id,
            @Parameter(description = "灰度比例(0-100)") @RequestParam int ratio,
            @Parameter(description = "指定团队ID列表，逗号分隔") @RequestParam(required = false) String teams) {
        recordAudit("GRAY", "prompt_template", String.valueOf(id));
        return ApiResponse.success(adminDataService.grayPrompt(id, ratio, teams));
    }

    @Operation(summary = "回滚Prompt", description = "将Prompt模板回滚到上一版本")
    @PostMapping("/ai/prompts/{id}/rollback")
    public ApiResponse<Map<String, Object>> rollbackPrompt(
            @Parameter(description = "Prompt模板ID") @PathVariable Long id,
            @Parameter(description = "回滚原因") @RequestParam(required = false) String reason) {
        recordAudit("ROLLBACK", "prompt_template", String.valueOf(id));
        return ApiResponse.success(adminDataService.rollbackPrompt(id, reason));
    }

    // ============================================================
    // PROMPT_TEMPLATE CRUD (alias endpoints for frontend compatibility)
    // ============================================================

    @Operation(summary = "查询Prompt模板列表", description = "分页查询Prompt模板")
    @GetMapping("/prompt_template/list")
    public ApiResponse<Map<String, Object>> listPromptTemplates(
            @Parameter(description = "模块标识") @RequestParam(required = false) String module,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "50") int pageSize,
            @Parameter(description = "关键词搜索") @RequestParam(required = false) String keyword) {
        return ApiResponse.success(adminDataService.list("prompt_template", module, page, pageSize, keyword));
    }

    @Operation(summary = "Prompt模板详情", description = "获取指定Prompt模板的详细信息")
    @GetMapping("/prompt_template/{id}")
    public ApiResponse<Map<String, Object>> promptTemplateDetail(
            @Parameter(description = "模板ID") @PathVariable Long id) {
        return ApiResponse.success(adminDataService.detail("prompt_template", id));
    }

    @Operation(summary = "创建Prompt模板", description = "创建新的Prompt模板")
    @PostMapping("/prompt_template/create")
    public ApiResponse<Map<String, Object>> createPromptTemplate(@RequestBody Map<String, Object> data) {
        recordAudit("create", "prompt_template", null);
        return ApiResponse.success(adminDataService.createPromptTemplate(data));
    }

    @Operation(summary = "更新Prompt模板", description = "更新指定Prompt模板")
    @PostMapping("/prompt_template/{id}/update")
    public ApiResponse<Map<String, Object>> updatePromptTemplate(
            @Parameter(description = "模板ID") @PathVariable Long id,
            @RequestBody Map<String, Object> data) {
        recordAudit("update", "prompt_template", String.valueOf(id));
        return ApiResponse.success(adminDataService.updatePromptTemplate(id, data));
    }

    @Operation(summary = "删除Prompt模板", description = "删除指定Prompt模板")
    @PostMapping("/prompt_template/{id}/delete")
    public ApiResponse<Map<String, Object>> deletePromptTemplate(
            @Parameter(description = "模板ID") @PathVariable Long id) {
        recordAudit("delete", "prompt_template", String.valueOf(id));
        return ApiResponse.success(adminDataService.deletePromptTemplate(id));
    }

    // ============================================================
    // 监控概览 + LLM 健康 + Milvus 状态
    // ============================================================

    @Operation(summary = "监控概览", description = "获取系统监控各项指标概览")
    @GetMapping("/monitor/overview")
    public ApiResponse<Map<String, Object>> monitorOverview() {
        return ApiResponse.success(adminDataService.monitorOverview());
    }

    @Operation(summary = "LLM健康检查", description = "检查所有LLM模型的健康状态")
    @PostMapping("/ai/llm-models/health-check")
    public ApiResponse<Map<String, Object>> llmHealthCheck() {
        return ApiResponse.success(adminDataService.llmHealthCheck());
    }

    @Operation(summary = "Milvus集合列表", description = "获取Milvus向量数据库中的所有集合")
    @GetMapping("/ai/milvus/collections")
    public ApiResponse<Map<String, Object>> milvusCollections() {
        return ApiResponse.success(adminDataService.milvusCollections());
    }

    // ============================================================
    // 域 01 基础设施：用户/角色/菜单/审计
    // ============================================================

    @Operation(summary = "查询管理员用户", description = "分页查询后台管理员用户列表")
    @GetMapping("/infra/users")
    public ApiResponse<Map<String, Object>> listUsers(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(description = "关键词搜索") @RequestParam(required = false) String keyword) {
        return ApiResponse.success(adminDataService.list("admin_user", null, page, pageSize, keyword));
    }

    @Operation(summary = "查询角色列表", description = "分页查询系统角色")
    @GetMapping("/infra/roles")
    public ApiResponse<Map<String, Object>> listRoles(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(adminDataService.list("admin_role", null, page, pageSize, null));
    }

    @Operation(summary = "查询菜单列表", description = "获取系统所有菜单树")
    @GetMapping("/infra/menus")
    public ApiResponse<Map<String, Object>> listMenus() {
        return ApiResponse.success(adminDataService.list("admin_menu", null, 1, 200, null));
    }

    @Operation(summary = "获取角色菜单", description = "获取指定角色关联的菜单ID列表")
    @GetMapping("/infra/roles/{roleId}/menus")
    public ApiResponse<Map<String, Object>> getRoleMenus(
            @Parameter(description = "角色ID") @PathVariable Long roleId) {
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

    @Operation(summary = "保存角色菜单", description = "为指定角色分配菜单权限")
    @PostMapping("/infra/roles/{roleId}/menus")
    public ApiResponse<Map<String, Object>> saveRoleMenus(
            @Parameter(description = "角色ID") @PathVariable Long roleId,
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

    @Operation(summary = "设置用户角色", description = "为指定用户分配角色")
    @PostMapping("/infra/users/{userId}/roles")
    public ApiResponse<Map<String, Object>> setUserRoles(
            @Parameter(description = "用户ID") @PathVariable Long userId,
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

    @Operation(summary = "获取用户角色", description = "获取指定用户拥有的角色ID列表")
    @GetMapping("/infra/users/{userId}/roles")
    public ApiResponse<Map<String, Object>> getUserRoles(
            @Parameter(description = "用户ID") @PathVariable Long userId) {
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

    @Operation(summary = "查询审计日志", description = "分页查询系统审计日志")
    @GetMapping("/infra/audit-logs")
    public ApiResponse<Map<String, Object>> listAuditLogs(
            @Parameter(description = "用户ID") @RequestParam(required = false) String userId,
            @Parameter(description = "操作类型") @RequestParam(required = false) String operation,
            @Parameter(description = "模块") @RequestParam(required = false) String module,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request) {
        Map<String, Object> adminInfo = getCurrentAdminUser(request);
        Long adminUserId = (Long) adminInfo.get("userId");
        DataScope dataScope = (DataScope) adminInfo.get("dataScope");
        return ApiResponse.success(adminDataService.audit(userId, operation, module, page, pageSize, dataScope, adminUserId));
    }

    @Operation(summary = "查询检索反馈", description = "分页查询用户对检索结果的评分反馈")
    @GetMapping("/infra/search-feedback")
    public ApiResponse<Map<String, Object>> listSearchFeedback(
            @Parameter(description = "法规ID") @RequestParam(required = false) Long articleId,
            @Parameter(description = "是否有帮助") @RequestParam(required = false) Integer isHelpful,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(adminDataService.listSearchFeedback(articleId, isHelpful, startDate, endDate, page, pageSize));
    }

    @Operation(summary = "检索反馈统计", description = "统计检索反馈的整体情况")
    @GetMapping("/infra/search-feedback/stats")
    public ApiResponse<Map<String, Object>> searchFeedbackStats(
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate) {
        return ApiResponse.success(adminDataService.searchFeedbackStats(startDate, endDate));
    }

    @Operation(summary = "查询法律收藏", description = "分页查询用户的法规收藏记录")
    @GetMapping("/infra/law-favorites")
    public ApiResponse<Map<String, Object>> listLawFavorites(
            @Parameter(description = "用户ID") @RequestParam(required = false) Long userId,
            @Parameter(description = "用户名") @RequestParam(required = false) String username,
            @Parameter(description = "法规ID") @RequestParam(required = false) Long articleId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(adminDataService.listLawFavorites(userId, username, articleId, page, pageSize));
    }

    @Operation(summary = "删除法律收藏", description = "删除指定的法律收藏记录")
    @DeleteMapping("/infra/law-favorites/{id}")
    public ApiResponse<Map<String, Object>> deleteLawFavorite(
            @Parameter(description = "收藏ID") @PathVariable Long id) {
        return ApiResponse.success(adminDataService.deleteLawFavorite(id));
    }

    @GetMapping("/infra/law-favorites/stats")
    public ApiResponse<Map<String, Object>> lawFavoriteStats() {
        return ApiResponse.success(adminDataService.lawFavoriteStats());
    }

    @Operation(summary = "导出审计日志", description = "将审计日志导出为CSV文件")
    @RateLimit(qps = 10, key = "admin_export")
    @GetMapping("/infra/audit-logs/export")
    public void exportAuditLogs(
            @Parameter(description = "用户ID") @RequestParam(required = false) String userId,
            @Parameter(description = "操作类型") @RequestParam(required = false) String operation,
            @Parameter(description = "模块") @RequestParam(required = false) String module,
            HttpServletResponse response) throws IOException {
        recordAudit("EXPORT", "audit_log", null);

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=audit-logs.csv");

        OutputStream os = response.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));

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

            if (rows == null || rows.isEmpty()) {
                break;
            }

            for (Map<String, Object> row : rows) {
                writer.write(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                        row.get("id"),
                        escapeCsv(row.get("username")),
                        escapeCsv(row.get("operation")),
                        escapeCsv(row.get("biz_module")),
                        escapeCsv(row.get("biz_type")),
                        escapeCsv(row.get("biz_id")),
                        escapeCsv(row.get("request_url")),
                        row.get("request_method"),
                        escapeCsv(row.get("ip")),
                        row.get("duration_ms"),
                        row.get("status"),
                        row.get("created_at")
                ));
                writer.newLine();
                totalWritten++;
            }

            page++;

            if (rows.size() < pageSize) {
                break;
            }
        }

        writer.flush();
    }

    private String escapeCsv(Object value) {
        if (value == null) {
            return "";
        }
        String str = value.toString();
        if (str.contains(",") || str.contains("\"") || str.contains("\n")) {
            return "\"" + str.replace("\"", "\"\"") + "\"";
        }
        return str;
    }

    @Operation(summary = "导出告警规则", description = "将告警规则导出为CSV文件")
    @GetMapping(value = "/monitor/alert-rules/export", produces = "text/csv")
    public String exportAlertRules() {
        recordAudit("EXPORT", "alert_rule", null);
        return adminDataService.exportAlertRulesCsv();
    }

    @Operation(summary = "导出告警历史", description = "将告警历史导出为CSV文件")
    @GetMapping(value = "/monitor/alert-history/export", produces = "text/csv")
    public String exportAlertHistory() {
        recordAudit("EXPORT", "alert_history", null);
        return adminDataService.exportAlertHistoryCsv();
    }

    @Operation(summary = "导出搜索日志", description = "将搜索日志导出为CSV文件")
    @GetMapping(value = "/ops/search-logs/export", produces = "text/csv")
    public String exportSearchLogs() {
        recordAudit("EXPORT", "search_log", null);
        return adminDataService.exportSearchLogsCsv();
    }

    @Operation(summary = "导出用户反馈", description = "将用户反馈导出为CSV文件")
    @GetMapping(value = "/ops/user-feedback/export", produces = "text/csv")
    public String exportUserFeedback() {
        recordAudit("EXPORT", "user_feedback", null);
        return adminDataService.exportUserFeedbackCsv();
    }

    @Operation(summary = "查询前端用户", description = "分页查询注册的前端用户")
    @GetMapping("/infra/frontend-users")
    public ApiResponse<Map<String, Object>> listFrontendUsers(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(description = "关键词搜索") @RequestParam(required = false) String keyword,
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

    @Operation(summary = "查询公告列表", description = "分页查询系统公告")
    @GetMapping("/infra/announcements")
    public ApiResponse<Map<String, Object>> listAnnouncements(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(description = "关键词搜索") @RequestParam(required = false) String keyword) {
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

    @Operation(summary = "查询字典列表", description = "根据类型查询系统字典")
    @GetMapping("/infra/dicts/list")
    public ApiResponse<Map<String, Object>> listDicts(
            @Parameter(description = "字典类型") @RequestParam(required = false) String dict_type) {
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

    @PostMapping("/biz/contract-reviews/draft")
    public ApiResponse<Map<String, Object>> saveContractDraft(@RequestBody Map<String, Object> data) {
        return ApiResponse.success(adminDataService.saveContractDraft(data));
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
            configRefreshService.notifyAll();
            result.put("configRefreshed", true);
        } catch (Exception e) {
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return ApiResponse.success(result);
    }

    @PostMapping("/sys/configs/refresh")
    public ApiResponse<Map<String, Object>> refreshSysConfigs() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            configRefreshService.notifyChange("sys_config");
            result.put("ok", true);
            result.put("message", "配置已热更新");
        } catch (Exception e) {
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return ApiResponse.success(result);
    }

    // ============================================================
    // 健康检查
    // ============================================================

    @Operation(summary = "MySQL健康检查", description = "检查MySQL数据库连接状态")
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

    @Operation(summary = "Redis健康检查", description = "检查Redis缓存服务连接状态")
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

    @Operation(summary = "Elasticsearch健康检查", description = "检查Elasticsearch服务状态")
    @GetMapping("/infra/es-health")
    public ApiResponse<Map<String, Object>> esHealth() {
        return ApiResponse.success(adminDataService.esHealth());
    }

    @Operation(summary = "管理后台健康检查", description = "检查管理后台服务状态")
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

    @Operation(summary = "查询分类类型列表", description = "获取所有法规分类类型")
    @GetMapping("/law/category-types")
    public ApiResponse<List<Map<String, Object>>> listCategoryTypes() {
        return ApiResponse.success(lawCategoryService.listTypes());
    }

    @Operation(summary = "获取分类类型详情", description = "获取指定分类类型的详细信息")
    @GetMapping("/law/category-types/{id}")
    public ApiResponse<LawCategoryType> getCategoryType(
            @Parameter(description = "类型ID") @PathVariable Long id) {
        return ApiResponse.<LawCategoryType>success(lawCategoryService.getType(id));
    }

    @Operation(summary = "创建分类类型", description = "创建新的法规分类类型")
    @PostMapping("/law/category-types")
    public ApiResponse<Void> createCategoryType(@RequestBody LawCategoryType type) {
        lawCategoryService.createType(type);
        return ApiResponse.success(null);
    }

    @Operation(summary = "更新分类类型", description = "更新指定分类类型")
    @PutMapping("/law/category-types/{id}")
    public ApiResponse<Void> updateCategoryType(
            @Parameter(description = "类型ID") @PathVariable Long id,
            @RequestBody LawCategoryType type) {
        lawCategoryService.updateType(id, type);
        return ApiResponse.success(null);
    }

    @Operation(summary = "删除分类类型", description = "删除指定的分类类型")
    @DeleteMapping("/law/category-types/{id}")
    public ApiResponse<Void> deleteCategoryType(
            @Parameter(description = "类型ID") @PathVariable Long id) {
        lawCategoryService.deleteType(id);
        return ApiResponse.success(null);
    }

    @Operation(summary = "查询分类列表", description = "获取指定类型的法规分类列表")
    @GetMapping("/law/categories")
    public ApiResponse<List<Map<String, Object>>> listCategories(
            @Parameter(description = "类型ID") @RequestParam(required = false) Long typeId) {
        return ApiResponse.success(lawCategoryService.listCategories(typeId));
    }

    @Operation(summary = "获取分类详情", description = "获取指定分类的详细信息")
    @GetMapping("/law/categories/{id}")
    public ApiResponse<Map<String, Object>> getCategory(
            @Parameter(description = "分类ID") @PathVariable Long id) {
        return ApiResponse.<Map<String, Object>>success(lawCategoryService.getCategory(id));
    }

    @Operation(summary = "创建分类", description = "创建新的法规分类")
    @PostMapping("/law/categories")
    public ApiResponse<Void> createCategory(@RequestBody LawCategory category) {
        lawCategoryService.createCategory(category);
        return ApiResponse.success(null);
    }

    @Operation(summary = "更新分类", description = "更新指定分类")
    @PutMapping("/law/categories/{id}")
    public ApiResponse<Void> updateCategory(
            @Parameter(description = "分类ID") @PathVariable Long id,
            @RequestBody LawCategory category) {
        lawCategoryService.updateCategory(id, category);
        return ApiResponse.success(null);
    }

    @Operation(summary = "删除分类", description = "删除指定的分类")
    @DeleteMapping("/law/categories/{id}")
    public ApiResponse<Void> deleteCategory(
            @Parameter(description = "分类ID") @PathVariable Long id) {
        lawCategoryService.deleteCategory(id);
        return ApiResponse.success(null);
    }

    @Operation(summary = "获取文档分类", description = "获取指定法规文档关联的分类")
    @GetMapping("/law/document-categories/{lawId}")
    public ApiResponse<List<Map<String, Object>>> getDocumentCategories(
            @Parameter(description = "法规文档ID") @PathVariable Long lawId) {
        return ApiResponse.success(lawCategoryService.getDocumentCategories(lawId));
    }

    @Operation(summary = "设置文档分类", description = "为法规文档设置分类关联")
    @PostMapping("/law/document-categories/{lawId}")
    public ApiResponse<Void> setDocumentCategories(
            @Parameter(description = "法规文档ID") @PathVariable Long lawId,
            @RequestBody Map<String, List<Long>> body) {
        lawCategoryService.setDocumentCategories(lawId, body.get("categoryIds"));
        return ApiResponse.success(null);
    }

    // ============================================================
    // DOC_TEMPLATE (Mod03) CRUD
    // ============================================================

    @Operation(summary = "查询文档模板", description = "查询文档模板列表")
    @GetMapping("/doc_template/list")
    public ApiResponse<Map<String, Object>> listDocTemplates(
            @Parameter(description = "分类") @RequestParam(required = false) String category) {
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

    @Operation(summary = "创建文档审查规则", description = "创建新的文档审查规则")
    @PostMapping("/doc_review_rule/create")
    public ApiResponse<Void> createDocReviewRule(@RequestBody Map<String, Object> data) {
        adminDataService.createDocReviewRule(data);
        recordAudit("create", "doc_review_rule", null);
        return ApiResponse.success(null);
    }

    @Operation(summary = "更新文档审查规则", description = "更新指定的文档审查规则")
    @PostMapping("/doc_review_rule/{id}/update")
    public ApiResponse<Void> updateDocReviewRule(
            @Parameter(description = "规则ID") @PathVariable Long id,
            @RequestBody Map<String, Object> data) {
        adminDataService.updateDocReviewRule(id, data);
        recordAudit("update", "doc_review_rule", String.valueOf(id));
        return ApiResponse.success(null);
    }

    @Operation(summary = "删除文档审查规则", description = "删除指定的文档审查规则")
    @PostMapping("/doc_review_rule/{id}/delete")
    public ApiResponse<Void> deleteDocReviewRule(
            @Parameter(description = "规则ID") @PathVariable Long id) {
        adminDataService.deleteDocReviewRule(id);
        recordAudit("delete", "doc_review_rule", String.valueOf(id));
        return ApiResponse.success(null);
    }

    @Operation(summary = "切换文档审查规则状态", description = "启用或禁用指定的文档审查规则")
    @PostMapping("/doc_review_rule/{id}/toggle")
    public ApiResponse<Void> toggleDocReviewRule(
            @Parameter(description = "规则ID") @PathVariable Long id,
            @RequestBody Map<String, Integer> data) {
        adminDataService.toggleDocReviewRule(id, data.get("status"));
        recordAudit("toggle", "doc_review_rule", String.valueOf(id));
        return ApiResponse.success(null);
    }

    // ============================================================
    // 应用日志
    // ============================================================

    @Operation(summary = "查询应用日志", description = "分页查询应用系统日志")
    @GetMapping("/ops/app-logs")
    public ApiResponse<Map<String, Object>> getAppLogs(
            @Parameter(description = "日志级别") @RequestParam(defaultValue = "INFO") String level,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "100") int pageSize) {
        return ApiResponse.success(appLogService.getLogs(level, page, pageSize));
    }

    @Operation(summary = "下载应用日志", description = "下载应用日志文件")
    @GetMapping("/ops/app-logs/download")
    public void downloadAppLogs(HttpServletResponse response) throws IOException {
        File logFile = new File("/var/log/legal-ai-assistant/app.log");
        if (logFile.exists()) {
            response.setContentType("text/plain");
            response.setHeader("Content-Disposition", "attachment; filename=app.log");
            Files.copy(logFile.toPath(), response.getOutputStream());
        }
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