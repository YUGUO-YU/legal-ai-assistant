package com.legalai.controller;

import com.legalai.dto.ApiResponse;
import com.legalai.dto.DocQaRequest;
import com.legalai.dto.DocQaResponse;
import com.legalai.service.DocQaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/doc-qa")
@CrossOrigin
@Tag(name = "文档问答", description = "基于上传文档的智能问答接口")
public class DocQaController {

    private final DocQaService docQaService;

    public DocQaController(DocQaService docQaService) {
        this.docQaService = docQaService;
    }

    @PostMapping("/ask")
    @Operation(summary = "文档问答", description = "基于知识库中的文档进行智能问答，支持多轮对话")
    public ApiResponse<DocQaResponse> ask(@RequestBody DocQaRequest request) {
        DocQaResponse response = docQaService.answerQuestion(request);
        return ApiResponse.success(response);
    }

    @PostMapping(value = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "文档问答(流式)", description = "基于知识库中的文档进行智能问答，流式返回")
    public Flux<String> askStream(@RequestBody DocQaRequest request) {
        return docQaService.answerQuestionStream(request);
    }

    @GetMapping("/sessions/{sessionId}/history")
    @Operation(summary = "获取会话历史", description = "获取指定会话的历史问答记录")
    public ApiResponse<List<Map<String, Object>>> getSessionHistory(
            @Parameter(description = "会话ID") @PathVariable String sessionId) {
        var history = docQaService.getSessionHistory(sessionId);
        List<Map<String, Object>> result = history.stream()
            .map(msg -> {
                Map<String, Object> map = new HashMap<>();
                map.put("role", msg.getRole());
                map.put("content", msg.getContent());
                map.put("createdAt", msg.getCreatedAt() != null ? msg.getCreatedAt().toString() : "");
                return map;
            })
            .collect(Collectors.toList());
        return ApiResponse.success(result);
    }

    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "清除会话历史", description = "清除指定会话的所有历史记录")
    public ApiResponse<Void> clearSession(
            @Parameter(description = "会话ID") @PathVariable String sessionId) {
        docQaService.clearSessionHistory(sessionId);
        return ApiResponse.success(null);
    }

    @GetMapping("/sessions")
    @Operation(summary = "获取会话列表", description = "获取用户的所有会话列表")
    public ApiResponse<List<Map<String, Object>>> getSessionList(
            @Parameter(description = "用户ID") @RequestParam(required = false, defaultValue = "default") String userId) {
        var sessions = docQaService.getSessionList(userId);
        return ApiResponse.success(sessions);
    }

    @PostMapping("/sessions")
    @Operation(summary = "创建会话", description = "创建一个新的问答会话")
    public ApiResponse<Map<String, Object>> createSession(
            @Parameter(description = "用户ID") @RequestParam(required = false, defaultValue = "default") String userId) {
        Map<String, Object> result = docQaService.createSession(userId);
        return ApiResponse.success(result);
    }
}
