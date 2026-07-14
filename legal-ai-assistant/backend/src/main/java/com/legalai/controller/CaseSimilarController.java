package com.legalai.controller;

import com.legalai.dto.*;
import com.legalai.service.CaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.*;

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

    @GetMapping("/{id}")
    @Operation(summary = "案例详情", description = "根据案例ID获取详细信息")
    public ApiResponse<CaseSimilarSearchResponse.SimilarCaseItem> getCaseDetail(@PathVariable String id) {
        CaseSimilarSearchResponse.SimilarCaseItem detail = caseService.getCaseDetail(id);
        if (detail == null) {
            return ApiResponse.error(404, "案例不存在");
        }
        return ApiResponse.success(detail);
    }

    @GetMapping("/{id}/elements")
    @Operation(summary = "案例要素", description = "获取指定案例的要素信息")
    public ApiResponse<List<Map<String, Object>>> getCaseElements(@PathVariable String id) {
        List<Map<String, Object>> elements = caseService.getCaseElements(id);
        return ApiResponse.success(elements);
    }

    @GetMapping("/{id}/similar")
    @Operation(summary = "案例相似列表", description = "获取与指定案例相似的案例列表")
    public ApiResponse<List<Map<String, Object>>> getSimilarCases(
            @PathVariable String id,
            @RequestParam(defaultValue = "10") int topK) {
        List<Map<String, Object>> similarCases = caseService.getSimilarCases(id, topK);
        return ApiResponse.success(similarCases);
    }

    @GetMapping("/by-element")
    @Operation(summary = "按要素检索", description = "根据案件要素类型和值检索案例")
    public ApiResponse<List<Map<String, Object>>> searchByElement(
            @RequestParam String elementType,
            @RequestParam String elementValue,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        List<Map<String, Object>> cases = caseService.searchCasesByElement(elementType, elementValue, page, pageSize);
        return ApiResponse.success(cases);
    }

    @GetMapping("/element-stats")
    @Operation(summary = "要素统计", description = "获取案例要素的统计信息")
    public ApiResponse<Map<String, Object>> getElementStats() {
        Map<String, Object> stats = caseService.getElementStats();
        return ApiResponse.success(stats);
    }
}
