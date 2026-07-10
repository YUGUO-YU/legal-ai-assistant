package com.legalai.controller;

import com.legalai.dto.*;
import com.legalai.service.CacheService;
import com.legalai.service.CompanyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/company")
@CrossOrigin
@Tag(name = "企业查询", description = "工商信息查询、企业风险评估相关接口")
public class CompanyQueryController {

    private final CompanyService companyService;
    private final CacheService cacheService;

    public CompanyQueryController(CompanyService companyService, CacheService cacheService) {
        this.companyService = companyService;
        this.cacheService = cacheService;
    }

    @PostMapping("/query")
    @Operation(summary = "查询企业信息", description = "根据企业名称查询工商信息，支持缓存")
    public ApiResponse<CompanyQueryResponse> query(@RequestBody CompanyQueryRequest request) {
        String companyName = request.getCompanyName();
        Object cached = cacheService.getCachedCompanyInfo(companyName);
        if (cached != null) {
            return ApiResponse.success((CompanyQueryResponse) cached);
        }
        CompanyQueryResponse response = companyService.queryCompany(request);
        cacheService.cacheCompanyInfo(companyName, response);
        return ApiResponse.success(response);
    }

    @GetMapping("/queries/{uuid}")
    @Operation(summary = "获取查询记录", description = "根据UUID获取企业查询记录详情")
    public ApiResponse<CompanyQueryResponse> getQuery(@PathVariable String uuid) {
        CompanyQueryResponse response = companyService.getQuery(uuid);
        if (response == null) {
            return ApiResponse.error(404, "查询记录不存在或已过期");
        }
        return ApiResponse.success(response);
    }

    @GetMapping("/queries")
    @Operation(summary = "查询最近记录", description = "获取当前用户的最近企业查询记录")
    public ApiResponse<List<CompanyQueryResponse>> listRecent(@RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.success(companyService.listRecent(Math.max(1, Math.min(limit, 100))));
    }

    @GetMapping("/risk-levels")
    @Operation(summary = "获取风险等级", description = "获取企业风险等级分类标准")
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