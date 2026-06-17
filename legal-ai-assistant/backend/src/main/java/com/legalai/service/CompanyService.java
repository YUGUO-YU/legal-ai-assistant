package com.legalai.service;

import com.legalai.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class CompanyService {
    private static final Logger log = LoggerFactory.getLogger(CompanyService.class);

    @Value("${mock.enabled:true}")
    private boolean mockEnabled;

    @Autowired
    private AIService aiService;

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
        response.setLegalRepresentative("模拟数据-张三");
        response.setRegisteredCapital(new BigDecimal("1000"));
        response.setBusinessStatus("存续（模拟数据）");
        response.setRegistrationAuthority("北京市市场监督管理局（模拟）");
        response.setEstablishDate("2020-01-15");
        response.setDataSource("Mock数据 | 仅供参考");

        List<CompanyQueryResponse.ShareholderInfo> shareholders = new ArrayList<>();
        shareholders.add(createShareholder("李四（模拟）", "500", "50%", "自然人股东"));
        shareholders.add(createShareholder("王五（模拟）", "300", "30%", "自然人股东"));
        shareholders.add(createShareholder("赵六（模拟）", "200", "20%", "自然人股东"));
        response.setShareholders(shareholders);

        List<CompanyQueryResponse.RiskWarning> warnings = new ArrayList<>();
        warnings.add(createRiskWarning("LOW", "经营异常（模拟）", "暂无异常", "", 0));

        if (Boolean.TRUE.equals(request.getEnableRiskWarning())) {
            warnings.add(createRiskWarning("MEDIUM", "法律诉讼（模拟）", "涉及合同纠纷", "", 0));
        }

        response.setRiskWarnings(warnings);
        response.setRiskLevel(calculateRiskLevel(warnings));

        return response;
    }

    private CompanyQueryResponse realQueryCompany(CompanyQueryRequest request) {
        log.info("调用MiniMax AI查询企业信息...");

        String prompt = buildCompanyQueryPrompt(request);

        try {
            String aiResponse = aiService.chat(prompt);
            return parseAIResponse(aiResponse, request);
        } catch (IOException e) {
            log.error("AI企业查询失败: {}", e.getMessage());
            return mockQueryCompany(request);
        }
    }

    private String buildCompanyQueryPrompt(CompanyQueryRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位专业的企业信息查询助手。请使用你的网络搜索能力，查找并核实用户输入企业的真实公开信息。\n\n");
        sb.append("【查询企业】").append(request.getCompanyName()).append("\n\n");
        sb.append("请执行以下搜索查询来获取真实信息：\n");
        sb.append("1. 搜索 \"").append(request.getCompanyName()).append(" 企业信息 统一社会信用代码\"\n");
        sb.append("2. 搜索 \"").append(request.getCompanyName()).append(" 法定代表人 注册资本\"\n");
        sb.append("3. 搜索 \"").append(request.getCompanyName()).append(" 股东信息\"\n\n");
        sb.append("根据搜索结果，提取以下信息（以JSON格式输出）：\n\n");

        sb.append("1. 企业基本信息：\n");
        sb.append("   - 统一社会信用代码（18位标准格式）\n");
        sb.append("   - 法定代表人\n");
        sb.append("   - 注册资本及实缴资本\n");
        sb.append("   - 经营状态（存续/吊销/注销等）\n");
        sb.append("   - 登记机关\n");
        sb.append("   - 成立日期\n");
        sb.append("   - 注册地址\n\n");

        sb.append("2. 股东信息（主要股东及持股比例）\n\n");

        if (Boolean.TRUE.equals(request.getEnableRiskWarning())) {
            sb.append("3. 风险提示（如有，搜索相关诉讼、被执行人、失信等信息）：\n");
            sb.append("   - 经营异常信息\n");
            sb.append("   - 法律诉讼情况\n");
            sb.append("   - 被执行人信息\n");
            sb.append("   - 失信被执行人信息\n");
            sb.append("   - 风险等级评估：LOW/MEDIUM/HIGH\n\n");
        }

        sb.append("请输出JSON格式（如果某项信息确实无法查到，请填\"未知\"）：\n");
        sb.append("{\n");
        sb.append("  \"companyName\": \"企业名称（以搜索到的为准）\",\n");
        sb.append("  \"unifiedSocialCreditCode\": \"统一社会信用代码\",\n");
        sb.append("  \"legalRepresentative\": \"法定代表人\",\n");
        sb.append("  \"registeredCapital\": 注册资本数值（单位万元，纯数字）,\n");
        sb.append("  \"businessStatus\": \"存续/吊销/注销等\",\n");
        sb.append("  \"registrationAuthority\": \"市场监督管理局名称\",\n");
        sb.append("  \"establishDate\": \"成立日期如2020-01-15\",\n");
        sb.append("  \"shareholders\": [\n");
        sb.append("    {\"name\": \"股东名称\", \"capitalContribution\": \"出资金额万元\", \"ratio\": \"持股比例\", \"type\": \"自然人股东/企业法人\"}\n");
        sb.append("  ],\n");
        sb.append("  \"riskWarnings\": [\n");
        sb.append("    {\"level\": \"LOW/MEDIUM/HIGH\", \"type\": \"风险类型\", \"description\": \"简要描述\", \"date\": \"日期\", \"count\": 数量}\n");
        sb.append("  ],\n");
        sb.append("  \"riskLevel\": \"NONE/LOW/MEDIUM/HIGH\",\n");
        sb.append("  \"dataSource\": \"AI网络搜索核实\"\n");
        sb.append("}\n\n");
        sb.append("重要：只输出JSON，不要有任何解释性文字。如果企业不存在或无法查到，请返回空数据并标注dataSource为\"未找到相关信息\"。");

        return sb.toString();
    }

    private CompanyQueryResponse parseAIResponse(String aiResponse, CompanyQueryRequest request) {
        String jsonContent = extractJsonFromResponse(aiResponse);
        if (jsonContent == null || jsonContent.isEmpty()) {
            log.error("无法从AI响应中提取JSON内容");
            return mockQueryCompany(request);
        }

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(jsonContent);

            CompanyQueryResponse response = new CompanyQueryResponse();
            response.setCompanyName(node.has("companyName") ? node.get("companyName").asText() : request.getCompanyName());
            response.setUnifiedSocialCreditCode(node.has("unifiedSocialCreditCode") ? node.get("unifiedSocialCreditCode").asText() : "91110000XXXXXXXX");
            response.setLegalRepresentative(node.has("legalRepresentative") ? node.get("legalRepresentative").asText() : "未知");
            response.setRegisteredCapital(node.has("registeredCapital") ? new BigDecimal(node.get("registeredCapital").asText()) : new BigDecimal("0"));
            response.setBusinessStatus(node.has("businessStatus") ? node.get("businessStatus").asText() : "存续");
            response.setRegistrationAuthority(node.has("registrationAuthority") ? node.get("registrationAuthority").asText() : "未知");
            response.setEstablishDate(node.has("establishDate") ? node.get("establishDate").asText() : "2020-01-01");
            response.setDataSource(node.has("dataSource") ? node.get("dataSource").asText() : "AI分析生成");

            java.util.List<CompanyQueryResponse.ShareholderInfo> shareholders = new java.util.ArrayList<>();
            if (node.has("shareholders")) {
                for (com.fasterxml.jackson.databind.JsonNode sh : node.get("shareholders")) {
                    CompanyQueryResponse.ShareholderInfo info = new CompanyQueryResponse.ShareholderInfo();
                    info.setName(sh.has("name") ? sh.get("name").asText() : "未知");
                    info.setCapitalContribution(sh.has("capitalContribution") ? sh.get("capitalContribution").asText() : "0万元");
                    info.setRatio(sh.has("ratio") ? sh.get("ratio").asText() : "0%");
                    info.setType(sh.has("type") ? sh.get("type").asText() : "未知");
                    shareholders.add(info);
                }
            }
            response.setShareholders(shareholders);

            if (Boolean.TRUE.equals(request.getEnableRiskWarning())) {
                java.util.List<CompanyQueryResponse.RiskWarning> warnings = new java.util.ArrayList<>();
                if (node.has("riskWarnings")) {
                    for (com.fasterxml.jackson.databind.JsonNode w : node.get("riskWarnings")) {
                        CompanyQueryResponse.RiskWarning warning = new CompanyQueryResponse.RiskWarning();
                        warning.setLevel(w.has("level") ? w.get("level").asText() : "LOW");
                        warning.setType(w.has("type") ? w.get("type").asText() : "其他");
                        warning.setDescription(w.has("description") ? w.get("description").asText() : "");
                        warning.setDate(w.has("date") ? w.get("date").asText() : "");
                        warning.setCount(w.has("count") ? w.get("count").asInt() : 0);
                        warnings.add(warning);
                    }
                }
                response.setRiskWarnings(warnings);
                response.setRiskLevel(node.has("riskLevel") ? node.get("riskLevel").asText() : "LOW");
            } else {
                response.setRiskWarnings(new java.util.ArrayList<>());
                response.setRiskLevel("NONE");
            }

            return response;

        } catch (Exception e) {
            log.error("解析AI企业查询响应失败: {}", e.getMessage());
            return mockQueryCompany(request);
        }
    }

    private String extractJsonFromResponse(String response) {
        if (response == null || response.isEmpty()) {
            return null;
        }

        String trimmed = response.trim();

        int jsonStart = trimmed.indexOf("[");
        int jsonEnd = trimmed.lastIndexOf("]");
        if (jsonStart == -1 || jsonEnd == -1 || jsonStart > jsonEnd) {
            jsonStart = trimmed.indexOf("{");
            jsonEnd = trimmed.lastIndexOf("}");
        }

        if (jsonStart != -1 && jsonEnd != -1 && jsonStart < jsonEnd) {
            String json = trimmed.substring(jsonStart, jsonEnd + 1);
            json = json.replaceAll("```json\\s*", "").replaceAll("```\\s*", "");
            return json;
        }

        return trimmed;
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