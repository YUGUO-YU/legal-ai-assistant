package com.legalai.controller;

import com.legalai.dto.*;
import com.legalai.service.CaseSearchService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/case-search")
@CrossOrigin
public class CaseSearchController {

    private final CaseSearchService caseSearchService;

    public CaseSearchController(CaseSearchService caseSearchService) {
        this.caseSearchService = caseSearchService;
    }

    @PostMapping("/search")
    public ApiResponse<CaseSearchResponse> search(@RequestBody CaseSearchRequest request) {
        CaseSearchResponse response = caseSearchService.searchCases(request);
        return ApiResponse.success(response);
    }
}