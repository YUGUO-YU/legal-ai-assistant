package com.legalai.admin.controller;

import com.legalai.admin.service.AdminDataService;
import com.legalai.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/biz")
@CrossOrigin
@Tag(name = "管理后台-数据资产", description = "MOD-01至MOD-10各业务模块数据管理")
public class BusinessController {
    @Autowired
    private AdminDataService adminDataService;

    @Autowired
    private AdminHelper adminHelper;

    @Operation(summary = "MOD-01 法规数据")
    @GetMapping("/mod01/laws")
    public ApiResponse<Map<String, Object>> mod01Laws(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(adminDataService.list("law_document", "MOD-01", page, pageSize, keyword));
    }

    @GetMapping("/mod01/laws/{id}/revisions")
    public ApiResponse<Map<String, Object>> mod01LawRevisions(@PathVariable Long id) {
        return ApiResponse.success(adminDataService.listRevisionsByLawId(id));
    }

    @GetMapping("/mod01/law-relations")
    public ApiResponse<Map<String, Object>> listLawRelations(
            @RequestParam(required = false) Long sourceArticleId,
            @RequestParam(required = false) Long targetArticleId,
            @RequestParam(required = false) String relationType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(adminDataService.listLawRelations(sourceArticleId, targetArticleId, relationType, page, pageSize));
    }

    @PostMapping("/mod01/law-relations")
    public ApiResponse<Map<String, Object>> createLawRelation(@RequestBody Map<String, Object> data) {
        adminHelper.recordAudit("CREATE", "law_relation", null);
        return ApiResponse.success(adminDataService.createLawRelation(data));
    }

    @PutMapping("/mod01/law-relations/{id}")
    public ApiResponse<Map<String, Object>> updateLawRelation(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        adminHelper.recordAudit("UPDATE", "law_relation", String.valueOf(id));
        return ApiResponse.success(adminDataService.updateLawRelation(id, data));
    }

    @DeleteMapping("/mod01/law-relations/{id}")
    public ApiResponse<Map<String, Object>> deleteLawRelation(@PathVariable Long id) {
        adminHelper.recordAudit("DELETE", "law_relation", String.valueOf(id));
        return ApiResponse.success(adminDataService.deleteLawRelation(id));
    }

    @GetMapping("/mod01/crawl-tasks")
    public ApiResponse<Map<String, Object>> mod01CrawlTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(adminDataService.list("crawl_task", null, page, pageSize, null));
    }

    @GetMapping("/mod01/crawl-logs")
    public ApiResponse<Map<String, Object>> mod01CrawlLogs(
            @RequestParam Long taskId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(adminDataService.listCrawlLogs(taskId, page, pageSize));
    }

    @Operation(summary = "MOD-02 案例数据")
    @GetMapping("/mod02/cases")
    public ApiResponse<Map<String, Object>> mod02Cases(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String cause,
            @RequestParam(required = false) Integer caseType,
            @RequestParam(required = false) Integer judgment) {
        return ApiResponse.success(adminDataService.listMod02Cases(page, pageSize, cause, caseType, judgment));
    }

    @GetMapping("/mod02/case-elements")
    public ApiResponse<Map<String, Object>> mod02CaseElements() {
        return ApiResponse.success(adminDataService.list("case_element_dict", null, 1, 100, null));
    }

    @Operation(summary = "MOD-03 文档模板")
    @GetMapping("/mod03/templates")
    public ApiResponse<Map<String, Object>> mod03Templates(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(adminDataService.list("doc_template", "MOD-03", page, pageSize, keyword));
    }

    @GetMapping("/mod03/drafts")
    public ApiResponse<Map<String, Object>> mod03Drafts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(adminDataService.list("doc_draft", "MOD-03", page, pageSize, null));
    }

    @GetMapping("/mod03/review-rules")
    public ApiResponse<Map<String, Object>> mod03ReviewRules() {
        return ApiResponse.success(adminDataService.list("doc_review_rule", null, 1, 100, null));
    }

    @Operation(summary = "MOD-04 研究任务")
    @GetMapping("/mod04/research-tasks")
    public ApiResponse<Map<String, Object>> mod04Tasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(adminDataService.list("legal_research_task", "MOD-04", page, pageSize, null));
    }

    @GetMapping("/mod04/research-tasks/{id}")
    public ApiResponse<Map<String, Object>> mod04TaskDetail(@PathVariable Long id) {
        return ApiResponse.success(adminDataService.getResearchTask(id));
    }

    @Operation(summary = "MOD-05 企业API配置")
    @GetMapping("/mod05/company-apis")
    public ApiResponse<Map<String, Object>> mod05CompanyApis() {
        return ApiResponse.success(adminDataService.list("company_api_config", null, 1, 100, null));
    }

    @Operation(summary = "MOD-06 案例查询日志")
    @GetMapping("/mod06/case-search-logs")
    public ApiResponse<Map<String, Object>> mod06Logs() {
        return ApiResponse.success(adminDataService.list("search_log", "MOD-06", 1, 20, null));
    }

    @Operation(summary = "MOD-07 法规查询")
    @GetMapping("/mod07/laws")
    public ApiResponse<Map<String, Object>> mod07Laws() {
        return ApiResponse.success(adminDataService.list("law_document", "MOD-07", 1, 20, null));
    }

    @Operation(summary = "MOD-08 合同审查规则")
    @GetMapping("/mod08/contract-rules")
    public ApiResponse<Map<String, Object>> mod08ContractRules() {
        return ApiResponse.success(adminDataService.list("contract_review_rule", null, 1, 100, null));
    }

    @Operation(summary = "合同审查列表")
    @GetMapping("/contract-reviews")
    public ApiResponse<Map<String, Object>> listContractReviews(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(adminDataService.listContractReviews(userId, riskLevel, page, pageSize));
    }

    @GetMapping("/contract-reviews/{id}")
    public ApiResponse<Map<String, Object>> getContractReview(@PathVariable Long id) {
        return ApiResponse.success(adminDataService.getContractReview(id));
    }

    @PostMapping("/contract-reviews/draft")
    public ApiResponse<Map<String, Object>> saveContractDraft(@RequestBody Map<String, Object> data) {
        return ApiResponse.success(adminDataService.saveContractDraft(data));
    }

    @Operation(summary = "MOD-09 知识库")
    @GetMapping("/mod09/kb-bases")
    public ApiResponse<Map<String, Object>> mod09KbBases() {
        return ApiResponse.success(adminDataService.list("kb_knowledge_base", "MOD-09", 1, 100, null));
    }

    @GetMapping("/mod09/kb-strategies")
    public ApiResponse<Map<String, Object>> mod09Strategies() {
        return ApiResponse.success(adminDataService.list("kb_chunk_strategy", null, 1, 100, null));
    }
}
