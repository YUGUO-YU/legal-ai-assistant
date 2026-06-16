package com.legalai.controller;

import com.legalai.dto.*;
import com.legalai.service.LegalResearchService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/legal-research")
@CrossOrigin
public class LegalResearchController {

    private final LegalResearchService legalResearchService;

    public LegalResearchController(LegalResearchService legalResearchService) {
        this.legalResearchService = legalResearchService;
    }

    @PostMapping("/tasks")
    public ApiResponse<String> createTask(@RequestBody LegalResearchRequest request) {
        String taskId = legalResearchService.createResearchTask(request);
        return ApiResponse.success(taskId);
    }

    @GetMapping(value = "/tasks/{taskId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamProgress(@PathVariable String taskId) {
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
    public ApiResponse<LegalResearchResponse> getReport(@PathVariable String taskId) {
        LegalResearchResponse response = legalResearchService.getResearchReport(taskId);
        return ApiResponse.success(response);
    }

    @PostMapping("/generate")
    public ApiResponse<LegalResearchResponse> generateReport(@RequestBody LegalResearchRequest request) {
        LegalResearchResponse response = legalResearchService.generateResearchReport(request);
        return ApiResponse.success(response);
    }
}