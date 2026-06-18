package com.legalai.controller;

import com.legalai.dto.*;
import com.legalai.service.CompanyService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/company")
@CrossOrigin
public class CompanyQueryController {

    private final CompanyService companyService;

    public CompanyQueryController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @PostMapping("/query")
    public ApiResponse<CompanyQueryResponse> query(@RequestBody CompanyQueryRequest request) {
        CompanyQueryResponse response = companyService.queryCompany(request);
        return ApiResponse.success(response);
    }

    @GetMapping("/queries/{uuid}")
    public ApiResponse<CompanyQueryResponse> getQuery(@PathVariable String uuid) {
        CompanyQueryResponse response = companyService.getQuery(uuid);
        if (response == null) {
            return ApiResponse.error(404, "查询记录不存在或已过期");
        }
        return ApiResponse.success(response);
    }

    @GetMapping("/queries")
    public ApiResponse<List<CompanyQueryResponse>> listRecent(@RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.success(companyService.listRecent(Math.max(1, Math.min(limit, 100))));
    }

    @GetMapping("/risk-levels")
    public ApiResponse<List<RiskLevelInfo>> getRiskLevels() {
        return ApiResponse.success(List.of(
            createRiskLevel("HIGH", "高风险", "立即通知", "red"),
            createRiskLevel("MEDIUM", "中风险", "24小时内", "yellow"),
            createRiskLevel("LOW", "低风险", "定期汇总", "blue")
        ));
    }

    private RiskLevelInfo createRiskLevel(String code, String name, String responseTime, String color) {
        RiskLevelInfo info = new RiskLevelInfo();
        info.setLevelCode(code);
        info.setLevelName(name);
        info.setResponseTime(responseTime);
        info.setColor(color);
        return info;
    }

    static class RiskLevelInfo {
        private String levelCode;
        private String levelName;
        private String responseTime;
        private String color;

        public String getLevelCode() { return levelCode; }
        public void setLevelCode(String levelCode) { this.levelCode = levelCode; }
        public String getLevelName() { return levelName; }
        public void setLevelName(String levelName) { this.levelName = levelName; }
        public String getResponseTime() { return responseTime; }
        public void setResponseTime(String responseTime) { this.responseTime = responseTime; }
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
    }
}