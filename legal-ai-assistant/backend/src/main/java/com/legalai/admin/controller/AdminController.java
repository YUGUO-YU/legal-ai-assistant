package com.legalai.admin.controller;

import com.legalai.admin.service.AdminDataService;
import com.legalai.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
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

    // ============================================================
    // 通用：列表 / 详情 / 统计
    // ============================================================

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        return ApiResponse.success(adminDataService.stats());
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

    @GetMapping("/infra/audit-logs")
    public ApiResponse<Map<String, Object>> listAuditLogs(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) String module,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(adminDataService.audit(userId, operation, module, page, pageSize));
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
        return ApiResponse.success(adminDataService.list("law_revision", null, 1, 50, null));
    }

    @GetMapping("/biz/mod01/crawl-tasks")
    public ApiResponse<Map<String, Object>> mod01CrawlTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(adminDataService.list("crawl_task", null, page, pageSize, null));
    }

    @GetMapping("/biz/mod02/cases")
    public ApiResponse<Map<String, Object>> mod02Cases(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(adminDataService.list("tb_case", "MOD-02", page, pageSize, keyword));
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

    @GetMapping("/ai/token-usage")
    public ApiResponse<Map<String, Object>> tokenUsage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(adminDataService.list("llm_token_usage", null, page, pageSize, null));
    }

    // ============================================================
    // 域 04 运营分析：反馈
    // ============================================================

    @GetMapping("/ops/user-feedback")
    public ApiResponse<Map<String, Object>> userFeedback(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(adminDataService.list("user_feedback", null, page, pageSize, null));
    }

    @GetMapping("/ops/search-logs")
    public ApiResponse<Map<String, Object>> searchLogs() {
        return ApiResponse.success(adminDataService.list("search_log", null, 1, 50, null));
    }

    // ============================================================
    // 域 05 监控告警
    // ============================================================

    @GetMapping("/monitor/alert-rules")
    public ApiResponse<Map<String, Object>> alertRules() {
        return ApiResponse.success(adminDataService.list("alert_rule", null, 1, 100, null));
    }

    @GetMapping("/monitor/alert-history")
    public ApiResponse<Map<String, Object>> alertHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(adminDataService.list("alert_history", null, page, pageSize, null));
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

    // ============================================================
    // 健康检查
    // ============================================================

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "UP");
        result.put("scope", "admin");
        result.put("timestamp", System.currentTimeMillis());
        result.put("domains", java.util.Arrays.asList(
                "infra", "biz(mod01-mod10)", "ai", "ops", "monitor", "sys"));
        return ApiResponse.success(result);
    }

    // ============================================================
    // helpers
    // ============================================================

    private void recordAudit(String operation, String bizType, String bizId) {
        try {
            adminDataService.recordAudit(null, "system", operation, "ADMIN",
                    bizType, bizId, "/api/v1/admin", "GET", null, "ok", null, 0, true, null);
        } catch (Exception ignore) {
        }
    }
}