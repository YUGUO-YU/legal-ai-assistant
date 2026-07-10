package com.legalai.controller;

import com.legalai.dto.*;
import com.legalai.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/document")
@CrossOrigin
@Tag(name = "文书起草", description = "法律文书智能起草相关接口")
public class DocumentController {
    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/draft")
    @Operation(summary = "起草文书", description = "根据模板和参数生成法律文书")
    public ApiResponse<DocumentDraftResponse> draft(@RequestBody DocumentDraftRequest request) {
        try {
            DocumentDraftResponse response = documentService.draftDocument(request);
            return ApiResponse.success(response);
        } catch (IllegalArgumentException e) {
            log.error("文书起草失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/templates")
    @Operation(summary = "获取文书模板", description = "获取所有可用的文书模板")
    public ApiResponse<List<DocumentService.TemplateInfo>> getTemplates() {
        List<DocumentService.TemplateInfo> templates = documentService.getTemplates();
        return ApiResponse.success(templates);
    }

    @GetMapping("/templates/{templateCode}")
    @Operation(summary = "获取模板详情", description = "获取指定文书模板的详细信息")
    public ApiResponse<DocumentService.TemplateInfo> getTemplate(@PathVariable String templateCode) {
        List<DocumentService.TemplateInfo> templates = documentService.getTemplates();
        DocumentService.TemplateInfo template = templates.stream()
            .filter(t -> t.getTemplateCode().equals(templateCode))
            .findFirst()
            .orElse(null);
        if (template == null) {
            return ApiResponse.error(404, "模板不存在");
        }
        return ApiResponse.success(template);
    }

    @PostMapping("/extract-info")
    @Operation(summary = "提取信息", description = "从文本中提取文书所需的关键信息")
    public ApiResponse<ExtractedInfo> extractInfo(@RequestBody ExtractInfoRequest request) {
        try {
            ExtractedInfo info = documentService.extractInfoFromText(request.getText(), request.getTemplateCode());
            return ApiResponse.success(info);
        } catch (Exception e) {
            log.error("信息提取失败: {}", e.getMessage());
            return ApiResponse.error(500, "信息提取失败: " + e.getMessage());
        }
    }
}