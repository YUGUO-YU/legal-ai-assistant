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
    public ResponseEntity<PptGenerateResponse> generate(@RequestBody PptGenerateRequest request) {
        PptGenerateResponse response = pptService.generatePpt(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PptDocumentDTO> getById(@PathVariable Long id) {
        PptDocumentDTO document = pptService.getById(id);
        return ResponseEntity.ok(document);
    }

    @GetMapping("/{id}/uuid/{uuid}")
    public ResponseEntity<PptDocumentDTO> getByUuid(@PathVariable Long id, @PathVariable String uuid) {
        PptDocumentDTO document = pptService.getByUuid(uuid);
        return ResponseEntity.ok(document);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PptDocumentDTO> update(@PathVariable Long id, @RequestBody PptUpdateRequest request) {
        PptDocumentDTO document = pptService.update(id, request);
        return ResponseEntity.ok(document);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Boolean>> delete(@PathVariable Long id) {
        boolean success = pptService.delete(id);
        return ResponseEntity.ok(Map.of("success", success));
    }

    @GetMapping("/list")
    public ResponseEntity<List<PptDocumentDTO>> list(@RequestParam(required = false) String userId) {
        List<PptDocumentDTO> documents = pptService.list(userId);
        return ResponseEntity.ok(documents);
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
    public ResponseEntity<List<PptTemplateDTO>> getTemplates() {
        List<PptTemplateDTO> templates = templateService.getTemplates();
        return ResponseEntity.ok(templates);
    }

    @PostMapping("/templates/recommend")
    public ResponseEntity<List<PptTemplateDTO>> recommendTemplates(@RequestBody Map<String, String> request) {
        String scenario = request.get("scenario");
        List<PptTemplateDTO> templates = templateService.getAiRecommendedTemplates(scenario);
        return ResponseEntity.ok(templates);
    }

    @PostMapping("/ai-enhance-slide")
    public ResponseEntity<Map<String, Object>> enhanceSlide(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = pptService.enhanceSlide(request);
        return ResponseEntity.ok(result);
    }
}
