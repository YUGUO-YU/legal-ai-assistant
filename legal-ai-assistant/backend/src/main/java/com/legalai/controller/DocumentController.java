package com.legalai.controller;

import com.legalai.dto.*;
import com.legalai.service.DocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/document")
@CrossOrigin
public class DocumentController {
    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/draft")
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
    public ApiResponse<List<DocumentService.TemplateInfo>> getTemplates() {
        List<DocumentService.TemplateInfo> templates = documentService.getTemplates();
        return ApiResponse.success(templates);
    }

    @GetMapping("/templates/{templateCode}")
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
    public ApiResponse<ExtractedInfo> extractInfo(
            @RequestParam String text,
            @RequestParam String templateCode) {
        try {
            ExtractedInfo info = documentService.extractInfoFromText(text, templateCode);
            if (info.isSuccess()) {
                return ApiResponse.success(info);
            } else {
                return ApiResponse.error(400, info.getErrorMessage());
            }
        } catch (Exception e) {
            log.error("信息提取失败: {}", e.getMessage());
            return ApiResponse.error(500, "信息提取失败: " + e.getMessage());
        }
    }
}