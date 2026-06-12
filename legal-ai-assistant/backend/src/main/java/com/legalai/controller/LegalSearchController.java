package com.legalai.controller;

import com.legalai.dto.*;
import com.legalai.service.LegalSearchService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/legal-search")
@CrossOrigin
public class LegalSearchController {

    private final LegalSearchService legalSearchService;

    public LegalSearchController(LegalSearchService legalSearchService) {
        this.legalSearchService = legalSearchService;
    }

    @PostMapping("/search")
    public ApiResponse<LegalSearchResponse> search(@RequestBody LegalSearchRequest request) {
        LegalSearchResponse response = legalSearchService.search(request);
        return ApiResponse.success(response);
    }

    @GetMapping("/articles/{articleId}")
    public ApiResponse<LegalSearchResponse> getArticle(@PathVariable String articleId) {
        LegalSearchRequest request = new LegalSearchRequest();
        request.setQuery(articleId);
        request.setPage(1);
        request.setPageSize(1);
        LegalSearchResponse response = legalSearchService.search(request);
        return ApiResponse.success(response);
    }

    @PostMapping("/feedback")
    public ApiResponse<Void> feedback(@RequestBody Object feedbackRequest) {
        return ApiResponse.success(null);
    }
}