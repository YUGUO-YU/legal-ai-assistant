package com.legalai.controller;

import com.legalai.dto.*;
import com.legalai.service.LegalResearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/legal-research")
@CrossOrigin
@Tag(name = "法律研究", description = "结构化法律研究相关接口")
public class LegalResearchController {

    private final LegalResearchService legalResearchService;

    public LegalResearchController(LegalResearchService legalResearchService) {
        this.legalResearchService = legalResearchService;
    }

    @PostMapping("/tasks")
    @Operation(summary = "创建研究任务", description = "创建异步法律研究任务，返回任务ID")
    @ApiResponse(responseCode = "200", description = "创建成功")
    public com.legalai.dto.ApiResponse<String> createTask(@RequestBody LegalResearchRequest request) {
        String taskId = legalResearchService.createResearchTask(request);
        return com.legalai.dto.ApiResponse.success(taskId);
    }

    @GetMapping(value = "/tasks/{taskId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "SSE流式进度", description = "通过SSE流式推送研究进度")
    @ApiResponse(responseCode = "200", description = "流式输出中")
    public Flux<ServerSentEvent<String>> streamProgress(
            @Parameter(description = "任务ID") @PathVariable String taskId) {
        List<Map<String, Object>> events = legalResearchService.getResearchProgress(taskId);

        return Flux.fromIterable(events).map(event -> {
            String data = event.get("type").equals("report_complete")
                ? "{\"reportId\":" + event.get("reportId") + "}"
                : "{\"phase\":\"" + event.get("phase") + "\",\"progress\":" + event.get("progress") + ",\"message\":\"" + event.get("message") + "\"}";
            return ServerSentEvent.<String>builder()
                .data(data)
                .build();
        });
    }

    @GetMapping("/tasks/{taskId}/report")
    @Operation(summary = "获取研究报告", description = "根据任务ID获取生成的研究报告")
    @ApiResponse(responseCode = "200", description = "获取成功")
    public com.legalai.dto.ApiResponse<LegalResearchResponse> getReport(
            @Parameter(description = "任务ID") @PathVariable String taskId) {
        LegalResearchResponse response = legalResearchService.getResearchReport(taskId);
        return com.legalai.dto.ApiResponse.success(response);
    }

    @PostMapping("/generate")
    @Operation(summary = "同步生成报告", description = "同步方式生成法律研究报告")
    @ApiResponse(responseCode = "200", description = "生成成功")
    public com.legalai.dto.ApiResponse<LegalResearchResponse> generateReport(@RequestBody LegalResearchRequest request) {
        LegalResearchResponse response = legalResearchService.generateResearchReport(request);
        return com.legalai.dto.ApiResponse.success(response);
    }
}
