package com.legalai.service;

import com.legalai.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class CompanyService {
    private static final Logger log = LoggerFactory.getLogger(CompanyService.class);

    public CompanyQueryResponse queryCompany(CompanyQueryRequest request) {
        log.info("企业查询请求: companyName={}", request.getCompanyName());

        CompanyQueryResponse response = new CompanyQueryResponse();
        response.setCompanyName(request.getCompanyName() != null ? request.getCompanyName() : "示例科技有限公司");
        response.setUnifiedSocialCreditCode("91110000" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        response.setLegalRepresentative("张三");
        response.setRegisteredCapital(new BigDecimal("1000"));
        response.setBusinessStatus("存续");
        response.setRegistrationAuthority("北京市市场监督管理局");
        response.setEstablishDate("2020-01-15");
        response.setDataSource("企查查 | 查询时间：" + LocalDateTime.now());

        List<CompanyQueryResponse.ShareholderInfo> shareholders = new ArrayList<>();
        shareholders.add(createShareholder("李四", "500", "50%"));
        shareholders.add(createShareholder("王五", "300", "30%"));
        shareholders.add(createShareholder("赵六", "200", "20%"));
        response.setShareholders(shareholders);

        List<CompanyQueryResponse.RiskWarning> warnings = new ArrayList<>();
        warnings.add(createRiskWarning("LOW", "经营异常", "暂时性经营异常，已申请移出", "2024-06-01"));

        if (Boolean.TRUE.equals(request.getEnableRiskWarning())) {
            warnings.add(createRiskWarning("MEDIUM", "法律诉讼", "涉及3起合同纠纷案件", "2024-05-15"));
        }

        response.setRiskWarnings(warnings);
        return response;
    }

    private CompanyQueryResponse.ShareholderInfo createShareholder(String name, String capital, String ratio) {
        CompanyQueryResponse.ShareholderInfo info = new CompanyQueryResponse.ShareholderInfo();
        info.setName(name);
        info.setCapitalContribution(capital + "万元");
        info.setRatio(ratio);
        return info;
    }

    private CompanyQueryResponse.RiskWarning createRiskWarning(String level, String type, String desc, String date) {
        CompanyQueryResponse.RiskWarning warning = new CompanyQueryResponse.RiskWarning();
        warning.setLevel(level);
        warning.setType(type);
        warning.setDescription(desc);
        warning.setDate(date);
        return warning;
    }
}