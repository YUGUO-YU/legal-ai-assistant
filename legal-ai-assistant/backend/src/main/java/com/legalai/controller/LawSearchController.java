package com.legalai.controller;

import com.legalai.dto.*;
import com.legalai.service.LawSearchService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/law-search")
@CrossOrigin
public class LawSearchController {

    private final LawSearchService lawSearchService;

    public LawSearchController(LawSearchService lawSearchService) {
        this.lawSearchService = lawSearchService;
    }

    @PostMapping("/search")
    public ApiResponse<LawSearchResponse> search(@RequestBody LawSearchRequest request) {
        LawSearchResponse response = lawSearchService.searchLaws(request);
        return ApiResponse.success(response);
    }

    @GetMapping("/categories")
    public ApiResponse<Object> getCategories() {
        return ApiResponse.success(java.util.Map.of(
            "categoryL1", java.util.List.of(
                java.util.Map.of("code", "法律", "name", "法律"),
                java.util.Map.of("code", "行政法规", "name", "行政法规"),
                java.util.Map.of("code", "部门规章", "name", "部门规章"),
                java.util.Map.of("code", "地方性法规", "name", "地方性法规"),
                java.util.Map.of("code", "司法解释", "name", "司法解释")
            ),
            "statusOptions", java.util.List.of(
                java.util.Map.of("value", 1, "label", "现行有效"),
                java.util.Map.of("value", 2, "label", "已废止"),
                java.util.Map.of("value", 3, "label", "修订中"),
                java.util.Map.of("value", 4, "label", "尚未生效"),
                java.util.Map.of("value", 5, "label", "部分失效")
            )
        ));
    }
}