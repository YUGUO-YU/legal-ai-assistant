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
    public ApiResponse<LegalSearchResponse.SearchResultItem> getArticle(@PathVariable String articleId) {
        LegalSearchResponse.SearchResultItem item = legalSearchService.getArticleDetail(articleId);
        if (item == null) {
            return ApiResponse.error(404, "Article not found");
        }
        return ApiResponse.success(item);
    }

    @PostMapping("/feedback")
    public ApiResponse<Void> feedback(@RequestBody SearchFeedbackRequest feedbackRequest) {
        legalSearchService.submitFeedback(feedbackRequest);
        return ApiResponse.success(null);
    }

    @GetMapping("/suggested-queries")
    public ApiResponse<java.util.List<String>> getSuggestedQueries(@RequestParam String query) {
        java.util.List<String> suggestions = legalSearchService.generateSuggestedQueries(query);
        return ApiResponse.success(suggestions);
    }
}