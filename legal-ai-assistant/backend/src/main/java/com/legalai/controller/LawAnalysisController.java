package com.legalai.controller;

import com.legalai.dto.ApiResponse;
import com.legalai.service.LawAnalysisService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/law-analysis")
@CrossOrigin
public class LawAnalysisController {

    private final LawAnalysisService lawAnalysisService;

    public LawAnalysisController(LawAnalysisService lawAnalysisService) {
        this.lawAnalysisService = lawAnalysisService;
    }

    @PostMapping("/analyze")
    public ApiResponse<Map<String, Object>> analyzeLaw(@RequestBody Map<String, Object> request) {
        String lawUuid = (String) request.get("lawUuid");
        String lawTitle = (String) request.get("lawTitle");
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> articles = (java.util.List<Map<String, Object>>) request.get("articles");

        if (lawUuid == null || lawUuid.isBlank()) {
            return ApiResponse.error(400, "lawUuid 不能为空");
        }

        Map<String, Object> analysis = lawAnalysisService.analyzeLaw(lawUuid, lawTitle, articles);
        return ApiResponse.success(analysis);
    }
}
