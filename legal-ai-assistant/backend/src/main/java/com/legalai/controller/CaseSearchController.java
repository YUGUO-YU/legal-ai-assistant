package com.legalai.controller;

import com.legalai.dto.*;
import com.legalai.service.CaseSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/case-search")
@CrossOrigin
@Tag(name = "案例查询", description = "司法判例检索相关接口")
public class CaseSearchController {

    private final CaseSearchService caseSearchService;

    public CaseSearchController(CaseSearchService caseSearchService) {
        this.caseSearchService = caseSearchService;
    }

    @PostMapping("/search")
    @Operation(summary = "案例多条件查询", description = "支持按案件类型、法院层级、审理程序等多维度过滤")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public com.legalai.dto.ApiResponse<CaseSearchResponse> search(@RequestBody CaseSearchRequest request) {
        CaseSearchResponse response = caseSearchService.searchCases(request);
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
}
