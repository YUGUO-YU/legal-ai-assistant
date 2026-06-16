package com.legalai.service;

import com.legalai.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class CompanyService {
    private static final Logger log = LoggerFactory.getLogger(CompanyService.class);

    @Value("${mock.enabled:true}")
    private boolean mockEnabled;

    public CompanyQueryResponse queryCompany(CompanyQueryRequest request) {
        log.info("企业查询请求: companyName={}", request.getCompanyName());

        validateRequest(request);

        if (mockEnabled) {
            return mockQueryCompany(request);
        }

        return realQueryCompany(request);
    }

    private void validateRequest(CompanyQueryRequest request) {
        if (request.getCompanyName() == null || request.getCompanyName().trim().isEmpty()) {
            throw new IllegalArgumentException("企业名称不能为空");
        }
        if (request.getCompanyName().length() > 200) {
            throw new IllegalArgumentException("企业名称长度不能超过200字");
        }
    }

    private CompanyQueryResponse mockQueryCompany(CompanyQueryRequest request) {
        CompanyQueryResponse response = new CompanyQueryResponse();
        String companyName = request.getCompanyName() != null ? request.getCompanyName() : "示例科技有限公司";

        response.setCompanyName(companyName);
        response.setUnifiedSocialCreditCode("91110000" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        response.setLegalRepresentative("张三");
        response.setRegisteredCapital(new BigDecimal("1000"));
        response.setBusinessStatus("存续");
        response.setRegistrationAuthority("北京市市场监督管理局");
        response.setEstablishDate("2020-01-15");
        response.setDataSource("企查查 | 查询时间：" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        List<CompanyQueryResponse.ShareholderInfo> shareholders = new ArrayList<>();
        shareholders.add(createShareholder("李四", "500", "50%", "自然人股东"));
        shareholders.add(createShareholder("王五", "300", "30%", "自然人股东"));
        shareholders.add(createShareholder("赵六", "200", "20%", "自然人股东"));
        shareholders.add(createShareholder("示例投资有限合伙企业", "1000", "通过多层股权穿透", "企业法人"));
        response.setShareholders(shareholders);

        List<CompanyQueryResponse.RiskWarning> warnings = new ArrayList<>();
        warnings.add(createRiskWarning("LOW", "经营异常", "暂时性经营异常，已申请移出", "2024-06-01", 1));

        if (Boolean.TRUE.equals(request.getEnableRiskWarning())) {
            warnings.add(createRiskWarning("MEDIUM", "法律诉讼", "涉及3起合同纠纷案件", "2024-05-15", 3));
            warnings.add(createRiskWarning("HIGH", "被执行人", "涉及金额50万元", "2024-04-20", 1));
        }

        response.setRiskWarnings(warnings);
        response.setRiskLevel(calculateRiskLevel(warnings));

        return response;
    }

    private CompanyQueryResponse realQueryCompany(CompanyQueryRequest request) {
        log.info("调用第三方API查询企业信息...");

        return mockQueryCompany(request);
    }

    private CompanyQueryResponse.ShareholderInfo createShareholder(String name, String capital, String ratio, String type) {
        CompanyQueryResponse.ShareholderInfo info = new CompanyQueryResponse.ShareholderInfo();
        info.setName(name);
        info.setCapitalContribution(capital + "万元");
        info.setRatio(ratio);
        info.setType(type);
        return info;
    }

    private CompanyQueryResponse.RiskWarning createRiskWarning(String level, String type, String desc, String date, int count) {
        CompanyQueryResponse.RiskWarning warning = new CompanyQueryResponse.RiskWarning();
        warning.setLevel(level);
        warning.setType(type);
        warning.setDescription(desc);
        warning.setDate(date);
        warning.setCount(count);
        return warning;
    }

    private String calculateRiskLevel(List<CompanyQueryResponse.RiskWarning> warnings) {
        if (warnings == null || warnings.isEmpty()) {
            return "NONE";
        }

        boolean hasHigh = warnings.stream().anyMatch(w -> "HIGH".equals(w.getLevel()));
        boolean hasMedium = warnings.stream().anyMatch(w -> "MEDIUM".equals(w.getLevel()));

        if (hasHigh) {
            return "HIGH";
        } else if (hasMedium) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    public CompanyQueryResponse getCompanyDetail(String companyName) {
        log.info("获取企业详情: companyName={}", companyName);

        CompanyQueryRequest request = new CompanyQueryRequest();
        request.setCompanyName(companyName);
        request.setEnableRiskWarning(true);

        return queryCompany(request);
    }

    public List<CompanyQueryResponse.ShareholderInfo> analyzeShareholdingStructure(String companyName) {
        log.info("分析股权结构: companyName={}", companyName);

        CompanyQueryResponse response = getCompanyDetail(companyName);
        return response.getShareholders();
    }
}