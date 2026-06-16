package com.legalai.controller;

import com.legalai.dto.*;
import com.legalai.service.DocQaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

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
    @ApiResponse(responseCode = "200", description = "问答成功")
    public com.legalai.dto.ApiResponse<DocQaResponse> ask(@RequestBody DocQaRequest request) {
        DocQaResponse response = docQaService.answerQuestion(request);
        return com.legalai.dto.ApiResponse.success(response);
    }

    @GetMapping("/sessions/{sessionId}/history")
    @Operation(summary = "获取会话历史", description = "获取指定会话的历史问答记录")
    @ApiResponse(responseCode = "200", description = "获取成功")
    public com.legalai.dto.ApiResponse<Object> getSessionHistory(
            @Parameter(description = "会话ID") @PathVariable String sessionId) {
        return com.legalai.dto.ApiResponse.success(null);
    }
}
