package com.legalai.controller;

import com.legalai.dto.*;
import com.legalai.service.CaseService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/case-similar")
@CrossOrigin
public class CaseSimilarController {

    private final CaseService caseService;

    public CaseSimilarController(CaseService caseService) {
        this.caseService = caseService;
    }

    @PostMapping("/search")
    public ApiResponse<CaseSimilarSearchResponse> search(@RequestBody CaseSimilarSearchRequest request) {
        CaseSimilarSearchResponse response = caseService.searchSimilarCases(request);
        return ApiResponse.success(response);
    }
}