package com.legalai.controller;

import com.legalai.admin.annotation.RateLimit;
import com.legalai.dto.*;
import com.legalai.service.LegalSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/legal-search")
@CrossOrigin
@Tag(name = "法规检索", description = "法规检索与溯源相关接口")
public class LegalSearchController {

    private final LegalSearchService legalSearchService;

    public LegalSearchController(LegalSearchService legalSearchService) {
        this.legalSearchService = legalSearchService;
    }

    @RateLimit(qps = 30, key = "search")
    @PostMapping("/search")
    @Operation(summary = "法规混合检索", description = "支持ES全文检索和Milvus向量检索的混合检索")
    @ApiResponse(responseCode = "200", description = "检索成功")
    public com.legalai.dto.ApiResponse<LegalSearchResponse> search(@RequestBody LegalSearchRequest request) {
        LegalSearchResponse response = legalSearchService.search(request);
        return com.legalai.dto.ApiResponse.success(response);
    }

    @GetMapping("/articles/{articleId}")
    @Operation(summary = "获取法规详情", description = "根据articleId获取法规条文详情")
    @ApiResponse(responseCode = "200", description = "获取成功")
    public com.legalai.dto.ApiResponse<LegalSearchResponse.SearchResultItem> getArticle(
            @Parameter(description = "法规ID") @PathVariable String articleId) {
        LegalSearchResponse.SearchResultItem item = legalSearchService.getArticleDetail(articleId);
        if (item == null) {
            return com.legalai.dto.ApiResponse.error(404, "Article not found");
        }
        return com.legalai.dto.ApiResponse.success(item);
    }

    @PostMapping("/feedback")
    @Operation(summary = "提交检索反馈", description = "用户对检索结果进行评分反馈")
    @ApiResponse(responseCode = "200", description = "提交成功")
    public com.legalai.dto.ApiResponse<Void> feedback(@RequestBody SearchFeedbackRequest feedbackRequest) {
        legalSearchService.submitFeedback(feedbackRequest);
        return com.legalai.dto.ApiResponse.success(null);
    }

    @GetMapping("/suggested-queries")
    @Operation(summary = "获取追问建议", description = "基于用户查询生成3-5个追问建议")
    @ApiResponse(responseCode = "200", description = "获取成功")
    public com.legalai.dto.ApiResponse<List<String>> getSuggestedQueries(
            @Parameter(description = "用户查询") @RequestParam String query) {
        List<String> suggestions = legalSearchService.generateSuggestedQueries(query);
        return com.legalai.dto.ApiResponse.success(suggestions);
    }
}
