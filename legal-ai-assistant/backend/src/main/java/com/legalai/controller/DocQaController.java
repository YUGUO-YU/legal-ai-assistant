package com.legalai.controller;

import com.legalai.dto.*;
import com.legalai.service.DocQaService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/doc-qa")
@CrossOrigin
public class DocQaController {

    private final DocQaService docQaService;

    public DocQaController(DocQaService docQaService) {
        this.docQaService = docQaService;
    }

    @PostMapping("/ask")
    public ApiResponse<DocQaResponse> ask(@RequestBody DocQaRequest request) {
        DocQaResponse response = docQaService.answerQuestion(request);
        return ApiResponse.success(response);
    }

    @GetMapping("/sessions/{sessionId}/history")
    public ApiResponse<Object> getSessionHistory(@PathVariable String sessionId) {
        return ApiResponse.success(null);
    }
}