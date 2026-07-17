package com.legalai.service;

import com.legalai.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class CompanyService {
    private static final Logger log = LoggerFactory.getLogger(CompanyService.class);

    @Autowired
    private AIService aiService;

    @Autowired
    private CompanyQueryStore queryStore;

    public CompanyQueryResponse queryCompany(CompanyQueryRequest request) {
        log.info("企业查询请求: companyName={}", request.getCompanyName());

        validateRequest(request);

        CompanyQueryResponse response = realQueryCompany(request);
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

    private CompanyQueryResponse realQueryCompany(CompanyQueryRequest request) {
        log.info("两阶段查询: 1.联网搜索 + 2.AI结构化整理...");

        String searchResult = null;
        try {
            String searchPrompt = buildSearchPrompt(request);
            searchResult = aiService.searchWeb(searchPrompt);
            log.info("联网搜索完成，结果长度={}", searchResult != null ? searchResult.length() : 0);
        } catch (IOException e) {
            log.warn("MiniMax联网搜索失败，尝试直接HTTP搜索: {}", e.getMessage());
            searchResult = directWebSearch(buildSearchPrompt(request));
            log.info("直接HTTP搜索完成，结果长度={}", searchResult != null ? searchResult.length() : 0);
        }

        if (searchResult == null || searchResult.contains("未获取到搜索结果") || searchResult.contains("搜索API调用失败") || searchResult.contains("网络搜索失败")) {
            log.warn("MiniMax搜索返回无效结果，尝试直接HTTP搜索...");
            searchResult = directWebSearch(buildSearchPrompt(request));
            log.info("直接HTTP搜索完成，结果长度={}", searchResult != null ? searchResult.length() : 0);
        }

        if (searchResult == null || searchResult.contains("未获取到") || searchResult.contains("搜索失败") || searchResult.isEmpty()) {
            throw new IllegalStateException("企业信息查询失败：无法从网络获取有效数据，请稍后重试");
        }

        try {
            String structurePrompt = buildStructurePrompt(request, searchResult);
            String aiResponse = aiService.chat(structurePrompt);
            return parseAIResponse(aiResponse, request);
        } catch (IOException e) {
            log.error("AI结构化失败: {}", e.getMessage());
            throw new IllegalStateException("企业信息查询失败：AI结构化处理异常，请稍后重试");
        }
    }

    private String directWebSearch(String query) {
        // Strategy 1: DuckDuckGo Instant Answer API
        String result = ddgInstantAnswer(query);
        if (result != null && !result.contains("未返回有效结果") && !result.contains("网络搜索失败")) {
            return result;
        }

        // Strategy 2: DuckDuckGo HTML scrape (more comprehensive)
        result = ddgHtmlSearch(query);
        if (result != null && !result.isEmpty()) {
            return result;
        }

        return "网络搜索未返回有效结果";
    }

    private String ddgInstantAnswer(String query) {
        try {
            String encodedQuery = java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
            String url = "https://api.duckduckgo.com/?q=" + encodedQuery + "&format=json&no_html=1&skip_disambig=1&hl=zh-cn";
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build();
            Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (compatible; LegalAI/1.0)")
                    .get()
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.warn("DuckDuckGo即时搜索失败: code={}", response.code());
                    return null;
                }
                String body = response.body() != null ? response.body().string() : "";
                if (body.isEmpty()) {
                    return null;
                }
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(body);
                StringBuilder sb = new StringBuilder();
                if (node.has("AbstractText") && !node.get("AbstractText").isNull()) {
                    sb.append(node.get("AbstractText").asText());
                }
                if (node.has("RelatedTopics")) {
                    for (JsonNode topic : node.get("RelatedTopics")) {
                        if (topic.has("Text")) {
                            sb.append("\n").append(topic.get("Text").asText());
                        }
                        if (sb.length() > 3000) break;
                    }
                }
                String result = sb.toString();
                log.info("DuckDuckGo即时搜索返回 {} 字符", result.length());
                return result.isEmpty() ? null : result;
            }
        } catch (Exception e) {
            log.warn("DuckDuckGo即时搜索异常: {}", e.getMessage());
            return null;
        }
    }

    private String ddgHtmlSearch(String query) {
        try {
            String encodedQuery = java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
            String url = "https://html.duckduckgo.com/html/?q=" + encodedQuery + "&kl=zh-cn";
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .build();
            Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (compatible; LegalAI/1.0)")
                    .get()
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.warn("DuckDuckGo HTML搜索失败: code={}", response.code());
                    return null;
                }
                String html = response.body() != null ? response.body().string() : "";
                if (html.isEmpty()) {
                    return null;
                }
                return parseDdgHtml(html);
            }
        } catch (Exception e) {
            log.warn("DuckDuckGo HTML搜索异常: {}", e.getMessage());
            return null;
        }
    }

    private String parseDdgHtml(String html) {
        StringBuilder sb = new StringBuilder();
        java.util.regex.Pattern linkPattern = java.util.regex.Pattern.compile(
                "<a class=\"result__a\"[^>]*href=\"([^\"]*)\"[^>]*>(.*?)</a>",
                java.util.regex.Pattern.DOTALL);
        java.util.regex.Pattern snippetPattern = java.util.regex.Pattern.compile(
                "<a class=\"result__snippet\"[^>]*>(.*?)</a>",
                java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher linkMatcher = linkPattern.matcher(html);
        java.util.regex.Matcher snippetMatcher = snippetPattern.matcher(html);
        java.util.List<String> links = new java.util.ArrayList<>();
        java.util.List<String> titles = new java.util.ArrayList<>();
        while (linkMatcher.find()) {
            String href = linkMatcher.group(1);
            String title = linkMatcher.group(2).replaceAll("<[^>]+>", "").trim();
            if (!title.isEmpty()) {
                links.add(href);
                titles.add(title);
            }
            if (links.size() >= 10) break;
        }
        int snippetCount = 0;
        while (snippetMatcher.find()) {
            String snippet = snippetMatcher.group(1).replaceAll("<[^>]+>", "").trim();
            if (snippetCount < links.size()) {
                sb.append("【").append(titles.get(snippetCount)).append("】")
                  .append(snippet).append("\n来源: ").append(links.get(snippetCount)).append("\n\n");
            }
            snippetCount++;
            if (sb.length() > 4000) break;
        }
        if (sb.length() > 0) {
            log.info("DuckDuckGo HTML解析提取 {} 条结果", snippetCount);
        }
        return sb.toString();
    }

    private String buildSearchPrompt(CompanyQueryRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("请帮我搜索以下企业的公开信息：\n");
        sb.append("1. 搜索 \"").append(request.getCompanyName()).append(" 企业基本信息 统一社会信用代码\"\n");
        sb.append("2. 搜索 \"").append(request.getCompanyName()).append(" 法定代表人 注册资本 经营范围\"\n");
        sb.append("3. 搜索 \"").append(request.getCompanyName()).append(" 股东信息 股权结构\"\n");
        sb.append("4. 搜索 \"").append(request.getCompanyName()).append(" 关联企业 对外投资 分支机构\"\n");
        sb.append("5. 搜索 \"").append(request.getCompanyName()).append(" 实际控制人 受益所有人\"\n");
        sb.append("6. 搜索 \"").append(request.getCompanyName()).append(" 经营状况 员工人数 知识产权 专利商标\"\n");
        if (Boolean.TRUE.equals(request.getEnableRiskWarning())) {
            sb.append("7. 搜索 \"").append(request.getCompanyName()).append(" 经营异常 法律诉讼 被执行人 失信\"\n");
        }
        sb.append("\n请尽可能多地收集上述信息，包括企业基础信息、股东、风险提示、关联企业、经营分析等。");
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

        sb.append("输出JSON格式（查不到的信息填\"未知\"或空数组）：\n");
        sb.append("{\n");
        sb.append("  \"companyName\": \"企业名称\",\n");
        sb.append("  \"unifiedSocialCreditCode\": \"统一社会信用代码\",\n");
        sb.append("  \"legalRepresentative\": \"法定代表人\",\n");
        sb.append("  \"registeredCapital\": 数字,\n");
        sb.append("  \"businessStatus\": \"存续/吊销/注销等\",\n");
        sb.append("  \"registrationAuthority\": \"登记机关\",\n");
        sb.append("  \"establishDate\": \"成立日期如2020-01-15\",\n");
        sb.append("  \"shareholders\": [{\"name\":\"\", \"capitalContribution\":\"\", \"ratio\":\"\", \"type\":\"\"}],\n");
        sb.append("  \"equityChain\": [{\"name\":\"\", \"ratio\":\"\", \"level\":1, \"type\":\"自然人/企业\"}],\n");
        sb.append("  \"relatedCompanies\": [{\"name\":\"\", \"relation\":\"子公司/对外投资/分支机构\", \"ratio\":\"\"}],\n");
        sb.append("  \"beneficialOwner\": {\"name\":\"\", \"finalRatio\":\"\", \"controlPath\":\"\"},\n");
        sb.append("  \"businessAnalysis\": {\"employeeCount\":0, \"employeeTrend\":\"\", \"paidInCapital\":0, \"industry\":\"\", \"industryAvgRatio\":\"\", \"patentCount\":0, \"trademarkCount\":0, \"copyrightCount\":0, \"businessScope\":\"\", \"mainBusiness\":\"\", \"yearlyData\":[{\"year\":2024, \"revenue\":0, \"employeeCount\":0, \"trend\":\"\"}]},\n");
        if (Boolean.TRUE.equals(request.getEnableRiskWarning())) {
            sb.append("  \"riskWarnings\": [{\"level\":\"LOW/MEDIUM/HIGH\", \"type\":\"\", \"description\":\"\", \"date\":\"\", \"count\":0}],\n");
            sb.append("  \"riskLevel\": \"NONE/LOW/MEDIUM/HIGH\",\n");
        }
        sb.append("  \"dataSource\": \"网络搜索+AI整理\"\n");
        sb.append("}\n\n");
        sb.append("只输出JSON，不要任何解释。");

        return sb.toString();
    }

    private CompanyQueryResponse parseAIResponse(String aiResponse, CompanyQueryRequest request) {
        String jsonContent = extractJsonFromResponse(aiResponse);
        if (jsonContent == null || jsonContent.isEmpty()) {
            log.error("无法从AI响应中提取JSON内容");
            throw new IllegalStateException("企业信息查询失败：AI返回格式异常，请稍后重试");
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(jsonContent);

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

            List<CompanyQueryResponse.ShareholderInfo> shareholders = new ArrayList<>();
            if (node.has("shareholders")) {
                for (JsonNode sh : node.get("shareholders")) {
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
                List<CompanyQueryResponse.RiskWarning> warnings = new ArrayList<>();
                if (node.has("riskWarnings")) {
                    for (JsonNode w : node.get("riskWarnings")) {
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
                response.setRiskWarnings(new ArrayList<>());
                response.setRiskLevel("NONE");
            }

            response.setEquityChain(parseEquityChain(node, response.getCompanyName()));
            response.setRelatedCompanies(parseRelatedCompanies(node, response.getCompanyName()));
            response.setBeneficialOwner(parseBeneficialOwner(node));
            response.setBusinessAnalysis(parseBusinessAnalysis(node));
            response.setSubscribed(false);

            return response;

        } catch (Exception e) {
            log.error("解析AI企业查询响应失败: {}", e.getMessage());
            throw new IllegalStateException("企业信息查询失败：解析AI响应异常，请稍后重试");
        }
    }

    private List<CompanyQueryResponse.EquityChain> parseEquityChain(JsonNode node, String companyName) {
        List<CompanyQueryResponse.EquityChain> chain = new ArrayList<>();
        try {
            if (node.has("equityChain") && node.get("equityChain").isArray()) {
                for (JsonNode item : node.get("equityChain")) {
                    CompanyQueryResponse.EquityChain ec = new CompanyQueryResponse.EquityChain();
                    ec.setLevel(item.has("level") ? item.get("level").asInt() : 1);
                    ec.setCompanyName(item.has("name") ? item.get("name").asText() : "未知");
                    ec.setRatio(item.has("ratio") ? item.get("ratio").asText() : "0%");
                    ec.setType(item.has("type") ? item.get("type").asText() : "自然人");
                    chain.add(ec);
                }
            }
        } catch (Exception e) {
            log.debug("解析股权链失败: {}", e.getMessage());
        }
        return chain;
    }

    private List<CompanyQueryResponse.RelatedCompany> parseRelatedCompanies(JsonNode node, String companyName) {
        List<CompanyQueryResponse.RelatedCompany> related = new ArrayList<>();
        try {
            if (node.has("relatedCompanies") && node.get("relatedCompanies").isArray()) {
                for (JsonNode item : node.get("relatedCompanies")) {
                    CompanyQueryResponse.RelatedCompany rc = new CompanyQueryResponse.RelatedCompany();
                    rc.setName(item.has("name") ? item.get("name").asText() : "未知");
                    rc.setRelation(item.has("relation") ? item.get("relation").asText() : "关联企业");
                    rc.setUnifiedSocialCreditCode(item.has("unifiedSocialCreditCode") ? item.get("unifiedSocialCreditCode").asText() : null);
                    rc.setBusinessStatus(item.has("businessStatus") ? item.get("businessStatus").asText() : null);
                    rc.setLegalRepresentative(item.has("legalRepresentative") ? item.get("legalRepresentative").asText() : null);
                    related.add(rc);
                }
            }
        } catch (Exception e) {
            log.debug("解析关联企业失败: {}", e.getMessage());
        }
        return related;
    }

    private CompanyQueryResponse.BeneficialOwner parseBeneficialOwner(JsonNode node) {
        try {
            if (node.has("beneficialOwner") && !node.get("beneficialOwner").isNull()) {
                JsonNode bo = node.get("beneficialOwner");
                CompanyQueryResponse.BeneficialOwner owner = new CompanyQueryResponse.BeneficialOwner();
                owner.setName(bo.has("name") ? bo.get("name").asText() : "未知");
                owner.setType(bo.has("type") ? bo.get("type").asText() : "自然人");
                if (bo.has("actualRatio")) {
                    owner.setActualRatio(bo.get("actualRatio").asDouble());
                }
                return owner;
            }
        } catch (Exception e) {
            log.debug("解析受益所有人失败: {}", e.getMessage());
        }
        CompanyQueryResponse.BeneficialOwner owner = new CompanyQueryResponse.BeneficialOwner();
        owner.setName("未知");
        owner.setType("自然人");
        return owner;
    }

    private CompanyQueryResponse.BusinessAnalysis parseBusinessAnalysis(JsonNode node) {
        try {
            if (node.has("businessAnalysis") && !node.get("businessAnalysis").isNull()) {
                JsonNode ba = node.get("businessAnalysis");
                CompanyQueryResponse.BusinessAnalysis analysis = new CompanyQueryResponse.BusinessAnalysis();
                analysis.setEmployeeCount(ba.has("employeeCount") ? ba.get("employeeCount").asInt() : 0);
                analysis.setEmployeeTrend(ba.has("employeeTrend") ? ba.get("employeeTrend").asText() : "稳定");
                analysis.setPaidInCapital(ba.has("paidInCapital") ? new BigDecimal(ba.get("paidInCapital").asText()) : BigDecimal.ZERO);
                analysis.setIndustry(ba.has("industry") ? ba.get("industry").asText() : "未知");
                analysis.setIndustryAvgRatio(ba.has("industryAvgRatio") ? ba.get("industryAvgRatio").asText() : "未知");
                analysis.setPatentCount(ba.has("patentCount") ? ba.get("patentCount").asInt() : 0);
                analysis.setTrademarkCount(ba.has("trademarkCount") ? ba.get("trademarkCount").asInt() : 0);
                analysis.setCopyrightCount(ba.has("copyrightCount") ? ba.get("copyrightCount").asInt() : 0);
                analysis.setBusinessScope(ba.has("businessScope") ? ba.get("businessScope").asText() : "未知");
                analysis.setMainBusiness(ba.has("mainBusiness") ? ba.get("mainBusiness").asText() : "未知");

                if (ba.has("yearlyData") && ba.get("yearlyData").isArray()) {
                    List<CompanyQueryResponse.YearData> yearlyData = new ArrayList<>();
                    for (JsonNode yd : ba.get("yearlyData")) {
                        CompanyQueryResponse.YearData data = new CompanyQueryResponse.YearData();
                        data.setYear(yd.has("year") ? yd.get("year").asInt() : 0);
                        data.setCapital(yd.has("capital") ? new BigDecimal(yd.get("capital").asText()) : BigDecimal.ZERO);
                        data.setEmployee(yd.has("employee") ? yd.get("employee").asInt() : 0);
                        data.setRevenue(yd.has("revenue") ? yd.get("revenue").asText() : "");
                        yearlyData.add(data);
                    }
                    analysis.setYearlyData(yearlyData);
                }
                return analysis;
            }
        } catch (Exception e) {
            log.debug("解析经营分析失败: {}", e.getMessage());
        }
        CompanyQueryResponse.BusinessAnalysis analysis = new CompanyQueryResponse.BusinessAnalysis();
        analysis.setEmployeeCount(0);
        analysis.setEmployeeTrend("未知");
        analysis.setIndustry("未知");
        analysis.setBusinessScope("未知");
        return analysis;
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