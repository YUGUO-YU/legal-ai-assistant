package com.legalai.controller;

import com.legalai.dto.ApiResponse;
import com.legalai.dto.KnowledgeBaseListResponse;
import com.legalai.service.DocumentParserService;
import com.legalai.service.KnowledgeBaseService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/knowledge-base")
@CrossOrigin
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final DocumentParserService documentParserService;

    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService, DocumentParserService documentParserService) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.documentParserService = documentParserService;
    }

    @GetMapping("/list")
    public ApiResponse<KnowledgeBaseListResponse> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        KnowledgeBaseListResponse response = knowledgeBaseService.listKnowledgeBases(keyword, page, pageSize);
        return ApiResponse.success(response);
    }

    @PostMapping("/create")
    public ApiResponse<KnowledgeBaseListResponse.KnowledgeBase> create(@RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        Boolean isPublic = (Boolean) request.get("isPublic");

        if (name == null || name.isBlank()) {
            return ApiResponse.error(400, "知识库名称不能为空");
        }

        KnowledgeBaseListResponse.KnowledgeBase kb = knowledgeBaseService.createKnowledgeBase(
            name, description != null ? description : "", isPublic != null && isPublic
        );
        return ApiResponse.success(kb);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        boolean success = knowledgeBaseService.deleteKnowledgeBase(id);
        if (success) {
            return ApiResponse.success(null);
        } else {
            return ApiResponse.error(404, "知识库不存在");
        }
    }

    @PostMapping("/upload")
    public ApiResponse<String> upload(@RequestBody Map<String, Object> request) {
        Long kbId = ((Number) request.get("kbId")).longValue();
        String fileName = (String) request.get("fileName");
        String content = (String) request.getOrDefault("content", "");

        String result = knowledgeBaseService.uploadDocument(kbId, fileName, content);
        return ApiResponse.success(result);
    }

    @PostMapping("/upload/file")
    public ApiResponse<String> uploadFile(
            @RequestParam("kbId") Long kbId,
            @RequestParam("file") MultipartFile file) {
        try {
            String fileName = file.getOriginalFilename();
            String content = documentParserService.parseDocument(file);
            String result = knowledgeBaseService.uploadDocument(kbId, fileName, content);
            return ApiResponse.success(result);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(500, "文件解析失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<KnowledgeBaseListResponse.KnowledgeBase> detail(@PathVariable Long id) {
        KnowledgeBaseListResponse.KnowledgeBase kb = knowledgeBaseService.getKnowledgeBase(id);
        if (kb == null) {
            return ApiResponse.error(404, "知识库不存在");
        }
        return ApiResponse.success(kb);
    }

    @GetMapping("/{id}/chunks")
    public ApiResponse<java.util.List<com.legalai.service.KnowledgeBaseService.DocumentChunk>> chunks(@PathVariable Long id) {
        return ApiResponse.success(knowledgeBaseService.getDocumentChunks(id));
    }
}
