package com.legalai.controller;

import com.legalai.dto.*;
import com.legalai.service.PptService;
import com.legalai.service.PptTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ppt")
@Tag(name = "PPT生成", description = "法律PPT智能生成相关接口")
public class PptController {

    @Autowired
    private PptService pptService;

    @Autowired
    private PptTemplateService templateService;

    @PostMapping("/generate")
    @Operation(summary = "生成PPT", description = "基于法律检索结果生成PPT文档")
    public ApiResponse<PptGenerateResponse> generate(@RequestBody PptGenerateRequest request) {
        PptGenerateResponse response = pptService.generatePpt(request);
        return ApiResponse.success(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取PPT详情", description = "根据ID获取PPT文档详情")
    public ApiResponse<PptDocumentDTO> getById(@PathVariable Long id) {
        PptDocumentDTO document = pptService.getById(id);
        return ApiResponse.success(document);
    }

    @GetMapping("/uuid/{uuid}")
    @Operation(summary = "获取PPT详情", description = "根据UUID获取PPT文档详情")
    public ApiResponse<PptDocumentDTO> getByUuid(@PathVariable String uuid) {
        PptDocumentDTO document = pptService.getByUuid(uuid);
        return ApiResponse.success(document);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新PPT", description = "更新PPT文档内容")
    public ApiResponse<PptDocumentDTO> update(@PathVariable Long id, @RequestBody PptUpdateRequest request) {
        PptDocumentDTO document = pptService.update(id, request);
        return ApiResponse.success(document);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除PPT", description = "删除指定的PPT文档")
    public ApiResponse<Map<String, Boolean>> delete(@PathVariable Long id) {
        boolean success = pptService.delete(id);
        return ApiResponse.success(Map.of("success", success));
    }

    @GetMapping("/list")
    @Operation(summary = "获取PPT列表", description = "获取用户的PPT文档列表")
    public ApiResponse<List<PptDocumentDTO>> list(@RequestParam(required = false) String userId) {
        List<PptDocumentDTO> documents = pptService.list(userId);
        return ApiResponse.success(documents);
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "下载PPT", description = "下载PPT文档文件")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        byte[] fileContent = pptService.generatePptx(id);
        String filename = pptService.getFilename(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.presentationml.presentation"));
        headers.setContentDispositionFormData("attachment", filename);

        return ResponseEntity.ok()
                .headers(headers)
                .body(fileContent);
    }

    @GetMapping("/templates")
    @Operation(summary = "获取PPT模板", description = "获取所有可用的PPT模板")
    public ApiResponse<List<PptTemplateDTO>> getTemplates() {
        List<PptTemplateDTO> templates = templateService.getTemplates();
        return ApiResponse.success(templates);
    }

    @PostMapping("/templates/recommend")
    @Operation(summary = "推荐PPT模板", description = "根据场景推荐合适的PPT模板")
    public ApiResponse<List<PptTemplateDTO>> recommendTemplates(@RequestBody Map<String, String> request) {
        String scenario = request.get("scenario");
        List<PptTemplateDTO> templates = templateService.getAiRecommendedTemplates(scenario);
        return ApiResponse.success(templates);
    }

    @PostMapping("/ai-enhance-slide")
    @Operation(summary = "AI增强幻灯片", description = "使用AI增强单张幻灯片的内容")
    public ApiResponse<Map<String, Object>> enhanceSlide(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = pptService.enhanceSlide(request);
        return ApiResponse.success(result);
    }
}
