package com.legalai.controller;

import com.legalai.dto.*;
import com.legalai.service.CacheService;
import com.legalai.service.CaseAnalysisService;
import com.legalai.service.CaseSearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/case-search")
@CrossOrigin
@Tag(name = "案例查询", description = "司法判例检索相关接口")
public class CaseSearchController {

    private final CaseSearchService caseSearchService;
    private final CaseAnalysisService caseAnalysisService;
    private final CacheService cacheService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CaseSearchController(CaseSearchService caseSearchService, CaseAnalysisService caseAnalysisService, CacheService cacheService) {
        this.caseSearchService = caseSearchService;
        this.caseAnalysisService = caseAnalysisService;
        this.cacheService = cacheService;
    }

    @PostMapping("/search")
    @Operation(summary = "案例多条件查询", description = "支持按案件类型、法院层级、审理程序等多维度过滤")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public com.legalai.dto.ApiResponse<CaseSearchResponse> search(@RequestBody CaseSearchRequest request) {
        String queryKey = request.toString();
        Object cached = cacheService.getCachedCaseResults(queryKey);
        if (cached != null) {
            return com.legalai.dto.ApiResponse.success((CaseSearchResponse) cached);
        }
        CaseSearchResponse response = caseSearchService.searchCases(request);
        cacheService.cacheCaseResults(queryKey, response);
        return com.legalai.dto.ApiResponse.success(response);
    }

    @GetMapping("/cases/{caseUuid}")
    @Operation(summary = "获取案例详情", description = "根据caseUuid获取案例详细信息")
    @ApiResponse(responseCode = "200", description = "获取成功")
    public com.legalai.dto.ApiResponse<CaseSearchResponse.CaseSearchItem> getCaseDetail(
            @Parameter(description = "案例UUID") @PathVariable String caseUuid) {
        CaseSearchResponse.CaseSearchItem item = caseSearchService.getCaseDetail(caseUuid);
        if (item == null) {
            return com.legalai.dto.ApiResponse.error(404, "案例不存在");
        }
        return com.legalai.dto.ApiResponse.success(item);
    }

    @GetMapping("/cases/{caseUuid}/analysis")
    @Operation(summary = "AI案情分析", description = "基于案例信息进行结构化AI案情分析")
    public com.legalai.dto.ApiResponse<CaseAnalysisResponse> analyzeCase(
            @Parameter(description = "案例UUID") @PathVariable String caseUuid) {
        try {
            CaseAnalysisResponse response = caseAnalysisService.getAnalysis(caseUuid);
            return com.legalai.dto.ApiResponse.success(response);
        } catch (IllegalArgumentException e) {
            return com.legalai.dto.ApiResponse.error(404, e.getMessage());
        } catch (Exception e) {
            return com.legalai.dto.ApiResponse.error(500, "AI案情分析失败: " + e.getMessage());
        }
    }

    @GetMapping(value = "/cases/{caseUuid}/analysis/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "AI案情分析流式", description = "通过SSE流式推送AI案情分析进度和结果")
    public Flux<ServerSentEvent<String>> analyzeCaseStream(
            @Parameter(description = "案例UUID") @PathVariable String caseUuid) {
        return caseAnalysisService.streamAnalysis(caseUuid)
            .map(event -> {
                try {
                    String data = objectMapper.writeValueAsString(event);
                    return ServerSentEvent.<String>builder().data(data).build();
                } catch (Exception e) {
                    String errJson = "{\"type\":\"error\",\"message\":\"" + escapeJson(e.getMessage()) + "\"}";
                    return ServerSentEvent.<String>builder().data(errJson).build();
                }
            });
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
