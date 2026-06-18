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

    @Autowired
    private CompanyQueryStore queryStore;

    public CompanyQueryResponse queryCompany(CompanyQueryRequest request) {
        log.info("企业查询请求: companyName={}", request.getCompanyName());

        validateRequest(request);

        CompanyQueryResponse response;
        if (mockEnabled) {
            response = mockQueryCompany(request);
        } else {
            response = realQueryCompany(request);
        }
        queryStore.save(response);
        return response;
    }

    public CompanyQueryResponse getQuery(String uuid) {
        return queryStore.get(uuid);
    }

    public List<CompanyQueryResponse> listRecent(int limit) {
        return queryStore.listRecent(limit);
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
        response.setSearchSources(Collections.singletonList("模拟数据源"));

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
        log.info("两阶段查询: 1.联网搜索 + 2.AI结构化整理...");

        try {
            String searchPrompt = buildSearchPrompt(request);
            String searchResult = aiService.searchWeb(searchPrompt);
            log.info("联网搜索完成，结果长度={}", searchResult != null ? searchResult.length() : 0);

            String structurePrompt = buildStructurePrompt(request, searchResult);
            String aiResponse = aiService.chat(structurePrompt);

            return parseAIResponse(aiResponse, request);
        } catch (IOException e) {
            log.error("AI企业查询失败: {}", e.getMessage());
            return mockQueryCompany(request);
        }
    }

    private String buildSearchPrompt(CompanyQueryRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("请帮我搜索以下企业的公开信息：\n");
        sb.append("1. 搜索 \"").append(request.getCompanyName()).append(" 企业基本信息 统一社会信用代码\"\n");
        sb.append("2. 搜索 \"").append(request.getCompanyName()).append(" 法定代表人 注册资本 经营范围\"\n");
        sb.append("3. 搜索 \"").append(request.getCompanyName()).append(" 股东信息 股权结构\"\n");
        if (Boolean.TRUE.equals(request.getEnableRiskWarning())) {
            sb.append("4. 搜索 \"").append(request.getCompanyName()).append(" 经营异常 法律诉讼 被执行人 失信\"\n");
        }
        sb.append("\n请尽可能多地收集上述信息，包括企业基础信息、股东、风险提示等。");
        return sb.toString();
    }

    private String buildStructurePrompt(CompanyQueryRequest request, String searchResult) {
        StringBuilder sb = new StringBuilder();
        sb.append("根据以下网络搜索结果，提取并整理 ").append(request.getCompanyName()).append(" 的企业信息。\n\n");
        sb.append("【搜索结果】\n");
        sb.append(searchResult != null ? searchResult : "无搜索结果").append("\n\n");
        sb.append("请提取以下信息并以JSON格式输出：\n\n");

        sb.append("1. 企业基本信息：统一社会信用代码、法定代表人、注册资本(万元，纯数字)、经营状态、登记机关、成立日期、注册地址\n");
        sb.append("2. 股东信息（主要股东及持股比例）\n\n");

        if (Boolean.TRUE.equals(request.getEnableRiskWarning())) {
            sb.append("3. 风险提示：经营异常、法律诉讼、被执行人、失信被执行人，风险等级(LOW/MEDIUM/HIGH)\n\n");
        }

        sb.append("输出JSON格式（查不到的信息填\"未知\"）：\n");
        sb.append("{\n");
        sb.append("  \"companyName\": \"企业名称\",\n");
        sb.append("  \"unifiedSocialCreditCode\": \"统一社会信用代码\",\n");
        sb.append("  \"legalRepresentative\": \"法定代表人\",\n");
        sb.append("  \"registeredCapital\": 数字,\n");
        sb.append("  \"businessStatus\": \"存续/吊销/注销等\",\n");
        sb.append("  \"registrationAuthority\": \"登记机关\",\n");
        sb.append("  \"establishDate\": \"成立日期如2020-01-15\",\n");
        sb.append("  \"shareholders\": [{\"name\":\"\", \"capitalContribution\":\"\", \"ratio\":\"\", \"type\":\"\"}],\n");
        sb.append("  \"riskWarnings\": [{\"level\":\"LOW/MEDIUM/HIGH\", \"type\":\"\", \"description\":\"\", \"date\":\"\", \"count\":0}],\n");
        sb.append("  \"riskLevel\": \"NONE/LOW/MEDIUM/HIGH\",\n");
        sb.append("  \"dataSource\": \"网络搜索+AI整理\"\n");
        sb.append("}\n\n");
        sb.append("只输出JSON，不要任何解释。");

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
            response.setDataSource(node.has("dataSource") ? node.get("dataSource").asText() : "网络搜索+AI整理");

            if (node.has("searchSources") && node.get("searchSources").isArray()) {
                List<String> sources = new ArrayList<>();
                for (JsonNode s : node.get("searchSources")) {
                    sources.add(s.asText());
                }
                response.setSearchSources(sources);
            } else {
                response.setSearchSources(Collections.singletonList("通过互联网搜索引擎获取的公开企业信息"));
            }

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