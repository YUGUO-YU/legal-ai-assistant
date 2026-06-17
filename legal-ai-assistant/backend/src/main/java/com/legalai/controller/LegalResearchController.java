package com.legalai.controller;

import com.legalai.dto.ApiResponse;
import com.legalai.dto.LegalResearchRequest;
import com.legalai.dto.LegalResearchResponse;
import com.legalai.service.LegalResearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

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
    public ApiResponse<String> createTask(@RequestBody LegalResearchRequest request) {
        String taskId = legalResearchService.createResearchTask(request);
        return ApiResponse.success(taskId);
    }

    @GetMapping(value = "/tasks/{taskId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "SSE流式进度", description = "通过SSE流式推送研究进度")
    public Flux<ServerSentEvent<String>> streamProgress(
            @Parameter(description = "任务ID") @PathVariable String taskId) {
        return Flux.create(sink -> {
            var events = legalResearchService.getResearchProgress(taskId);
            for (Map<String, Object> event : events) {
                String data;
                if ("report_complete".equals(event.get("type"))) {
                    data = "{\"type\":\"report_complete\",\"reportId\":\"" + event.get("reportId") + "\"}";
                } else {
                    data = "{\"type\":\"" + event.get("type") + "\",\"phase\":\"" + event.get("phase") + "\",\"progress\":" + event.get("progress") + ",\"message\":\"" + event.get("message") + "\"}";
                }
                sink.next(ServerSentEvent.<String>builder().data(data).build());
            }
            sink.complete();
        });
    }

    @GetMapping("/tasks/{taskId}/report")
    @Operation(summary = "获取研究报告", description = "根据任务ID获取生成的研究报告")
    public ApiResponse<LegalResearchResponse> getReport(
            @Parameter(description = "任务ID") @PathVariable String taskId) {
        LegalResearchResponse response = legalResearchService.getResearchReport(taskId);
        return ApiResponse.success(response);
    }

    @PostMapping("/generate")
    @Operation(summary = "同步生成报告", description = "同步方式生成法律研究报告")
    public ApiResponse<LegalResearchResponse> generateReport(@RequestBody LegalResearchRequest request) {
        LegalResearchResponse response = legalResearchService.generateResearchReport(request);
        return ApiResponse.success(response);
    }

    @PostMapping(value = "/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式生成报告", description = "通过SSE流式输出研究报告生成进度和内容")
    public Flux<ServerSentEvent<String>> generateReportStream(@RequestBody LegalResearchRequest request) {
        return legalResearchService.generateReportStream(request)
            .map(event -> {
                String data;
                if ("report".equals(event.get("type"))) {
                    String content = (String) event.get("content");
                    data = "{\"type\":\"report\",\"content\":" + escapeJson(content) + "}";
                } else if ("error".equals(event.get("type"))) {
                    data = "{\"type\":\"error\",\"message\":\"" + escapeJson((String) event.get("message")) + "\"}";
                } else {
                    data = "{\"type\":\"" + event.get("type") + "\",\"phase\":\"" + event.get("phase") + "\",\"progress\":" + event.get("progress") + ",\"message\":\"" + escapeJson((String) event.get("message")) + "\"}";
                }
                return ServerSentEvent.<String>builder().data(data).build();
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
