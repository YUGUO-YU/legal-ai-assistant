package com.legalai.controller;

import com.legalai.dto.ApiResponse;
import com.legalai.dto.KnowledgeBaseListResponse;
import com.legalai.service.KnowledgeBaseService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/knowledge-base")
@CrossOrigin
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
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

        String result = knowledgeBaseService.uploadDocument(kbId, fileName);
        return ApiResponse.success(result);
    }
}
