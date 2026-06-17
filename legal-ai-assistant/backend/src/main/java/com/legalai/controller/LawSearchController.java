package com.legalai.controller;

import com.legalai.dto.*;
import com.legalai.model.LawArticle;
import com.legalai.service.LawSearchService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    public ApiResponse<Map<String, Object>> getCategories() {
        Map<String, Object> categories = lawSearchService.getCategories();
        return ApiResponse.success(categories);
    }

    @GetMapping("/laws/{lawUuid}")
    public ApiResponse<LawSearchResponse.LawSearchItem> getLawDetail(@PathVariable String lawUuid) {
        LawSearchResponse.LawSearchItem item = lawSearchService.getLawDetail(lawUuid);
        if (item == null) {
            return ApiResponse.error(404, "法规不存在");
        }
        return ApiResponse.success(item);
    }

    @GetMapping("/laws/{lawUuid}/articles")
    public ApiResponse<List<LawArticle>> getLawArticles(@PathVariable String lawUuid) {
        List<LawArticle> articles = lawSearchService.getLawArticles(lawUuid);
        return ApiResponse.success(articles);
    }
}