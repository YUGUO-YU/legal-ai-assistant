package com.legalai.controller;

import com.legalai.dto.*;
import com.legalai.service.ContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/contract")
@CrossOrigin
@Tag(name = "合同审查", description = "AI合同审查相关接口")
public class ContractReviewController {

    private final ContractService contractService;

    public ContractReviewController(ContractService contractService) {
        this.contractService = contractService;
    }

    @PostMapping("/review")
    @Operation(summary = "合同审查", description = "对合同文本进行多维度风险审查，输出风险点和修改建议")
    @ApiResponse(responseCode = "200", description = "审查成功")
    public com.legalai.dto.ApiResponse<ContractReviewResponse> review(@RequestBody ContractReviewRequest request) {
        ContractReviewResponse response = contractService.reviewContract(request);
        return com.legalai.dto.ApiResponse.success(response);
    }

    @GetMapping("/dimensions")
    @Operation(summary = "获取审查维度", description = "获取合同审查的风险维度及其权重")
    @ApiResponse(responseCode = "200", description = "获取成功")
    public com.legalai.dto.ApiResponse<List<DimensionInfo>> getDimensions() {
        return com.legalai.dto.ApiResponse.success(List.of(
            createDimension("SUBJECT_QUALIFICATION", "主体资格", 15),
            createDimension("CONTRACT_VALIDITY", "合同效力", 20),
            createDimension("RIGHTS_OBLIGATIONS", "权利义务", 15),
            createDimension("BREACH_RESPONSIBILITY", "违约责任", 15),
            createDimension("DISPUTE_RESOLUTION", "争议解决", 10),
            createDimension("EXEMPTION_CLAUSE", "免责条款", 10),
            createDimension("INTELLECTUAL_PROPERTY", "知识产权", 8),
            createDimension("PERSONAL_INFO", "个人信息", 7)
        ));
    }

    private DimensionInfo createDimension(String code, String name, int weight) {
        DimensionInfo info = new DimensionInfo();
        info.setDimensionCode(code);
        info.setDimensionName(name);
        info.setWeight(weight);
        return info;
    }

    @GetMapping("/reviews/{uuid}")
    @Operation(summary = "获取合同审查详情", description = "根据审查UUID获取历史审查结果详情")
    public com.legalai.dto.ApiResponse<ContractReviewResponse> getReview(@PathVariable String uuid) {
        ContractReviewResponse response = contractService.getReview(uuid);
        if (response == null) {
            return com.legalai.dto.ApiResponse.error(404, "审查记录不存在或已过期");
        }
        return com.legalai.dto.ApiResponse.success(response);
    }

    @GetMapping("/reviews")
    @Operation(summary = "获取最近审查记录", description = "获取最近的合同审查记录列表")
    public com.legalai.dto.ApiResponse<List<ContractReviewResponse>> listRecent(
            @RequestParam(defaultValue = "20") int limit) {
        return com.legalai.dto.ApiResponse.success(contractService.listRecent(Math.max(1, Math.min(limit, 100))));
    }

    static class DimensionInfo {
        private String dimensionCode;
        private String dimensionName;
        private int weight;

        public String getDimensionCode() { return dimensionCode; }
        public void setDimensionCode(String dimensionCode) { this.dimensionCode = dimensionCode; }
        public String getDimensionName() { return dimensionName; }
        public void setDimensionName(String dimensionName) { this.dimensionName = dimensionName; }
        public int getWeight() { return weight; }
        public void setWeight(int weight) { this.weight = weight; }
    }
}
