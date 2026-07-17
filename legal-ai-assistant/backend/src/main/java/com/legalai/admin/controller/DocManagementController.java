package com.legalai.admin.controller;

import com.legalai.admin.service.AdminDataService;
import com.legalai.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@CrossOrigin
@Tag(name = "管理后台-文档管理", description = "文档模板和审查规则CRUD")
public class DocManagementController {
    @Autowired
    private AdminDataService adminDataService;

    @Autowired
    private AdminHelper adminHelper;

    @Operation(summary = "查询文档模板")
    @GetMapping("/doc_template/list")
    public ApiResponse<Map<String, Object>> listDocTemplates(@RequestParam(required = false) String category) {
        return ApiResponse.success(adminDataService.listDocTemplates(category));
    }

    @PostMapping("/doc_template/create")
    public ApiResponse<Void> createDocTemplate(@RequestBody Map<String, Object> data) {
        adminDataService.createDocTemplate(data);
        adminHelper.recordAudit("create", "doc_template", null);
        return ApiResponse.success(null);
    }

    @PostMapping("/doc_template/{id}/update")
    public ApiResponse<Void> updateDocTemplate(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        adminDataService.updateDocTemplate(id, data);
        adminHelper.recordAudit("update", "doc_template", String.valueOf(id));
        return ApiResponse.success(null);
    }

    @PostMapping("/doc_template/{id}/delete")
    public ApiResponse<Void> deleteDocTemplate(@PathVariable Long id) {
        adminDataService.deleteDocTemplate(id);
        adminHelper.recordAudit("delete", "doc_template", String.valueOf(id));
        return ApiResponse.success(null);
    }

    @PostMapping("/doc_template/{id}/toggle")
    public ApiResponse<Void> toggleDocTemplate(@PathVariable Long id, @RequestBody Map<String, Integer> data) {
        adminDataService.toggleDocTemplate(id, data.get("status"));
        adminHelper.recordAudit("toggle", "doc_template", String.valueOf(id));
        return ApiResponse.success(null);
    }

    @Operation(summary = "创建文档审查规则")
    @PostMapping("/doc_review_rule/create")
    public ApiResponse<Void> createDocReviewRule(@RequestBody Map<String, Object> data) {
        adminDataService.createDocReviewRule(data);
        adminHelper.recordAudit("create", "doc_review_rule", null);
        return ApiResponse.success(null);
    }

    @Operation(summary = "更新文档审查规则")
    @PostMapping("/doc_review_rule/{id}/update")
    public ApiResponse<Void> updateDocReviewRule(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        adminDataService.updateDocReviewRule(id, data);
        adminHelper.recordAudit("update", "doc_review_rule", String.valueOf(id));
        return ApiResponse.success(null);
    }

    @Operation(summary = "删除文档审查规则")
    @PostMapping("/doc_review_rule/{id}/delete")
    public ApiResponse<Void> deleteDocReviewRule(@PathVariable Long id) {
        adminDataService.deleteDocReviewRule(id);
        adminHelper.recordAudit("delete", "doc_review_rule", String.valueOf(id));
        return ApiResponse.success(null);
    }

    @Operation(summary = "切换文档审查规则状态")
    @PostMapping("/doc_review_rule/{id}/toggle")
    public ApiResponse<Void> toggleDocReviewRule(@PathVariable Long id, @RequestBody Map<String, Integer> data) {
        adminDataService.toggleDocReviewRule(id, data.get("status"));
        adminHelper.recordAudit("toggle", "doc_review_rule", String.valueOf(id));
        return ApiResponse.success(null);
    }
}
