package com.legalai.controller;

import com.legalai.dto.*;
import com.legalai.service.ContractService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/contract")
@CrossOrigin
public class ContractReviewController {

    private final ContractService contractService;

    public ContractReviewController(ContractService contractService) {
        this.contractService = contractService;
    }

    @PostMapping("/review")
    public ApiResponse<ContractReviewResponse> review(@RequestBody ContractReviewRequest request) {
        ContractReviewResponse response = contractService.reviewContract(request);
        return ApiResponse.success(response);
    }

    @GetMapping("/dimensions")
    public ApiResponse<List<DimensionInfo>> getDimensions() {
        return ApiResponse.success(List.of(
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