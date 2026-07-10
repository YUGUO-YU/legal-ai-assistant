package com.legalai.controller;

import com.legalai.dto.*;
import com.legalai.service.CaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/case-similar")
@CrossOrigin
@Tag(name = "类案匹配", description = "基于案例相似度检索相关接口")
public class CaseSimilarController {

    private final CaseService caseService;

    public CaseSimilarController(CaseService caseService) {
        this.caseService = caseService;
    }

    @PostMapping("/search")
    @Operation(summary = "类案检索", description = "基于案例事实检索相似案例")
    public ApiResponse<CaseSimilarSearchResponse> search(@RequestBody CaseSimilarSearchRequest request) {
        CaseSimilarSearchResponse response = caseService.searchSimilarCases(request);
        return ApiResponse.success(response);
    }
}