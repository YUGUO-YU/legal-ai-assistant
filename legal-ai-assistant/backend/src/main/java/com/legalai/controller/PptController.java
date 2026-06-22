package com.legalai.controller;

import com.legalai.dto.*;
import com.legalai.service.PptService;
import com.legalai.service.PptTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ppt")
public class PptController {

    @Autowired
    private PptService pptService;

    @Autowired
    private PptTemplateService templateService;

    @PostMapping("/generate")
    public ApiResponse<PptGenerateResponse> generate(@RequestBody PptGenerateRequest request) {
        PptGenerateResponse response = pptService.generatePpt(request);
        return ApiResponse.success(response);
    }

    @GetMapping("/{id}")
    public ApiResponse<PptDocumentDTO> getById(@PathVariable Long id) {
        PptDocumentDTO document = pptService.getById(id);
        return ApiResponse.success(document);
    }

    @GetMapping("/uuid/{uuid}")
    public ApiResponse<PptDocumentDTO> getByUuid(@PathVariable String uuid) {
        PptDocumentDTO document = pptService.getByUuid(uuid);
        return ApiResponse.success(document);
    }

    @PutMapping("/{id}")
    public ApiResponse<PptDocumentDTO> update(@PathVariable Long id, @RequestBody PptUpdateRequest request) {
        PptDocumentDTO document = pptService.update(id, request);
        return ApiResponse.success(document);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, Boolean>> delete(@PathVariable Long id) {
        boolean success = pptService.delete(id);
        return ApiResponse.success(Map.of("success", success));
    }

    @GetMapping("/list")
    public ApiResponse<List<PptDocumentDTO>> list(@RequestParam(required = false) String userId) {
        List<PptDocumentDTO> documents = pptService.list(userId);
        return ApiResponse.success(documents);
    }

    @GetMapping("/{id}/download")
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
    public ApiResponse<List<PptTemplateDTO>> getTemplates() {
        List<PptTemplateDTO> templates = templateService.getTemplates();
        return ApiResponse.success(templates);
    }

    @PostMapping("/templates/recommend")
    public ApiResponse<List<PptTemplateDTO>> recommendTemplates(@RequestBody Map<String, String> request) {
        String scenario = request.get("scenario");
        List<PptTemplateDTO> templates = templateService.getAiRecommendedTemplates(scenario);
        return ApiResponse.success(templates);
    }

    @PostMapping("/ai-enhance-slide")
    public ApiResponse<Map<String, Object>> enhanceSlide(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = pptService.enhanceSlide(request);
        return ApiResponse.success(result);
    }
}
