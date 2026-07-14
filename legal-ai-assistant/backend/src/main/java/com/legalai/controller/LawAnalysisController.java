package com.legalai.controller;

import com.legalai.dto.ApiResponse;
import com.legalai.service.LawAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/law-analysis")
@CrossOrigin
@Tag(name = "法规分析", description = "法规智能分析相关接口")
public class LawAnalysisController {

    private final LawAnalysisService lawAnalysisService;

    public LawAnalysisController(LawAnalysisService lawAnalysisService) {
        this.lawAnalysisService = lawAnalysisService;
    }

    @PostMapping("/analyze")
    @Operation(summary = "分析法规", description = "对指定法规进行智能分析")
    public ApiResponse<Map<String, Object>> analyzeLaw(@RequestBody Map<String, Object> request) {
        String lawUuid = (String) request.get("lawUuid");
        String lawTitle = (String) request.get("lawTitle");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> articles = (List<Map<String, Object>>) request.get("articles");

        if (lawUuid == null || lawUuid.isBlank()) {
            return ApiResponse.error(400, "lawUuid 不能为空");
        }

        Map<String, Object> analysis = lawAnalysisService.analyzeLaw(lawUuid, lawTitle, articles);
        return ApiResponse.success(analysis);
    }
}
