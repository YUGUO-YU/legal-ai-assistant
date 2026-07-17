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
import java.util.HashMap;
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
    private AdminHelper adminHelper;

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

    @Autowired
    private com.legalai.service.CsrfTokenService csrfTokenService;

    // ============================================================
    // CSRF Token
    // ============================================================

    @Operation(summary = "获取CSRF Token", description = "获取CSRF Token用于后续请求验证")
    @GetMapping("/csrf-token")
    public ApiResponse<Map<String, String>> getCsrfToken(HttpServletRequest request) {
        String sessionId = request.getHeader("X-Session-Id");
        if (sessionId == null) {
            sessionId = "default";
        }
        String token = csrfTokenService.generateToken(sessionId);
        Map<String, String> result = new HashMap<>();
        result.put("token", token);
        return ApiResponse.success(result);
    }

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

    @Operation(summary = "24小时访问趋势", description = "返回今日和昨日各小时登录次数")
    @GetMapping("/stats/hourly-access")
    public ApiResponse<Map<String, Object>> hourlyAccess() {
        return ApiResponse.success(adminDataService.getHourlyAccess());
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
        adminHelper.recordAudit("LIST", table, module);
        return ApiResponse.success(data);
    }

    @Operation(summary = "通用详情查询", description = "根据ID获取指定表的单条数据详情")
    @GetMapping("/{table}/{id}")
    public ApiResponse<Map<String, Object>> detail(
            @Parameter(description = "表名") @PathVariable String table,
            @Parameter(description = "数据ID") @PathVariable Long id) {
        Map<String, Object> data = adminDataService.detail(table, id);
        adminHelper.recordAudit("DETAIL", table, String.valueOf(id));
        return ApiResponse.success(data);
    }

    @Operation(summary = "通用数据创建", description = "向指定表插入一条新数据")
    @RateLimit(qps = 20, key = "admin_write")
    @PostMapping("/{table}/create")
    public ApiResponse<Map<String, Object>> create(
            @Parameter(description = "表名") @PathVariable String table,
            @RequestBody Map<String, Object> payload) {
        Map<String, Object> data = adminDataService.create(table, payload);
        adminHelper.recordAudit("CREATE", table, String.valueOf(data.get("id")));
        return ApiResponse.success(data);
    }

    @Operation(summary = "通用数据更新", description = "更新指定表的单条数据")
    @PostMapping("/{table}/{id}/update")
    public ApiResponse<Map<String, Object>> update(
            @Parameter(description = "表名") @PathVariable String table,
            @Parameter(description = "数据ID") @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {
        Map<String, Object> data = adminDataService.update(table, id, payload);
        adminHelper.recordAudit("UPDATE", table, String.valueOf(id));
        return ApiResponse.success(data);
    }

    @Operation(summary = "通用数据删除", description = "删除指定表的单条数据")
    @PostMapping("/{table}/{id}/delete")
    public ApiResponse<Map<String, Object>> delete(
            @Parameter(description = "表名") @PathVariable String table,
            @Parameter(description = "数据ID") @PathVariable Long id) {
        Map<String, Object> data = adminDataService.delete(table, id);
        adminHelper.recordAudit("DELETE", table, String.valueOf(id));
        return ApiResponse.success(data);
    }

    @Operation(summary = "通用状态切换", description = "切换指定记录的某个布尔字段状态")
    @PostMapping("/{table}/{id}/toggle")
    public ApiResponse<Map<String, Object>> toggle(
            @Parameter(description = "表名") @PathVariable String table,
            @Parameter(description = "数据ID") @PathVariable Long id,
            @Parameter(description = "字段名") @RequestParam String column) {
        Map<String, Object> data = adminDataService.toggle(table, id, column);
        adminHelper.recordAudit("TOGGLE", table, String.valueOf(id));
        return ApiResponse.success(data);
    }

    @Operation(summary = "批量删除", description = "批量删除指定表的多条数据")
    @PostMapping("/{table}/batch-delete")
    public ApiResponse<Map<String, Object>> batchDelete(
            @Parameter(description = "表名") @PathVariable String table,
            @RequestBody Map<String, Object> data) {
        @SuppressWarnings("unchecked")
        List<Long> ids = ((List<Number>) data.get("ids")).stream().map(Number::longValue).toList();
        adminHelper.recordAudit("BATCH_DELETE", table, String.join(",", ids.stream().map(String::valueOf).toList()));
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
        adminHelper.recordAudit("BATCH_TOGGLE", table, String.join(",", ids.stream().map(String::valueOf).toList()) + " -> " + status);
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
        adminHelper.recordAudit("PUBLISH", "prompt_template", String.valueOf(id));
        return ApiResponse.success(adminDataService.publishPrompt(id));
    }

    @Operation(summary = "灰度发布Prompt", description = "以灰度方式发布Prompt模板")
    @PostMapping("/ai/prompts/{id}/gray")
    public ApiResponse<Map<String, Object>> grayPrompt(
            @Parameter(description = "Prompt模板ID") @PathVariable Long id,
            @Parameter(description = "灰度比例(0-100)") @RequestParam int ratio,
            @Parameter(description = "指定团队ID列表，逗号分隔") @RequestParam(required = false) String teams) {
        adminHelper.recordAudit("GRAY", "prompt_template", String.valueOf(id));
        return ApiResponse.success(adminDataService.grayPrompt(id, ratio, teams));
    }

    @Operation(summary = "回滚Prompt", description = "将Prompt模板回滚到上一版本")
    @PostMapping("/ai/prompts/{id}/rollback")
    public ApiResponse<Map<String, Object>> rollbackPrompt(
            @Parameter(description = "Prompt模板ID") @PathVariable Long id,
            @Parameter(description = "回滚原因") @RequestParam(required = false) String reason) {
        adminHelper.recordAudit("ROLLBACK", "prompt_template", String.valueOf(id));
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
        adminHelper.recordAudit("create", "prompt_template", null);
        return ApiResponse.success(adminDataService.createPromptTemplate(data));
    }

    @Operation(summary = "更新Prompt模板", description = "更新指定Prompt模板")
    @PostMapping("/prompt_template/{id}/update")
    public ApiResponse<Map<String, Object>> updatePromptTemplate(
            @Parameter(description = "模板ID") @PathVariable Long id,
            @RequestBody Map<String, Object> data) {
        adminHelper.recordAudit("update", "prompt_template", String.valueOf(id));
        return ApiResponse.success(adminDataService.updatePromptTemplate(id, data));
    }

    @Operation(summary = "删除Prompt模板", description = "删除指定Prompt模板")
    @PostMapping("/prompt_template/{id}/delete")
    public ApiResponse<Map<String, Object>> deletePromptTemplate(
            @Parameter(description = "模板ID") @PathVariable Long id) {
        adminHelper.recordAudit("delete", "prompt_template", String.valueOf(id));
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

    // (Domain 02: MOD01-MOD10 - migrated to BusinessController)
    // (Domain 03: AI config - migrated to AiConfigController)
    // (Domain 04: Ops - migrated to OpsController)
    // (Domain 05: Monitor - migrated to MonitorController)
    // (Domain 06: System - migrated to SystemController)
    // (Law categories - migrated to LawCategoryController)
    // (Doc management - migrated to DocManagementController)

    // Helpers delegated to AdminHelper (see AdminHelper.java)
}