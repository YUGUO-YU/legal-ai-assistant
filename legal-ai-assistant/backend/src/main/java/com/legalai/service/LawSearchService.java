package com.legalai.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.legalai.config.ElasticsearchConfig;
import com.legalai.dto.*;
import com.legalai.model.LawArticle;
import com.legalai.model.LawDocument;
import com.legalai.repository.LawArticleMapper;
import com.legalai.repository.LawDocumentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LawSearchService {
    private static final Logger log = LoggerFactory.getLogger(LawSearchService.class);

    @Value("${mock.enabled:false}")
    private boolean mockEnabled;

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Autowired
    private ElasticsearchConfig esConfig;

    @Autowired
    private AIService aiService;

    @Autowired
    private LawDocumentMapper lawDocumentMapper;

    @Autowired
    private LawArticleMapper lawArticleMapper;

    private static final List<Map<String, String>> CATEGORIES_L1 = List.of(
        Map.of("code", "法律", "name", "法律"),
        Map.of("code", "行政法规", "name", "行政法规"),
        Map.of("code", "部门规章", "name", "部门规章"),
        Map.of("code", "地方性法规", "name", "地方性法规"),
        Map.of("code", "司法解释", "name", "司法解释")
    );

    private static final List<Map<String, String>> CATEGORIES_L2 = List.of(
        Map.of("code", "民法", "name", "民法", "parent", "法律"),
        Map.of("code", "商法", "name", "商法", "parent", "法律"),
        Map.of("code", "刑法", "name", "刑法", "parent", "法律"),
        Map.of("code", "行政法", "name", "行政法", "parent", "法律"),
        Map.of("code", "劳动法", "name", "劳动法", "parent", "法律"),
        Map.of("code", "知识产权法", "name", "知识产权法", "parent", "法律"),
        Map.of("code", "诉讼法", "name", "诉讼法", "parent", "法律")
    );

    public LawSearchResponse searchLaws(LawSearchRequest request) {
        log.info("法规查询请求: keyword={}, categoryL1={}, status={}",
            request.getKeyword(), request.getCategoryL1(), request.getStatus());

        if (!mockEnabled) {
            LawSearchResponse dbResponse = dbSearchLaws(request);
            if (dbResponse != null && dbResponse.getTotal() > 0) {
                return dbResponse;
            }
        }

        if (mockEnabled) {
            return mockSearchLaws(request);
        }

        return esSearchLaws(request);
    }

    private LawSearchResponse dbSearchLaws(LawSearchRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            QueryWrapper<LawDocument> queryWrapper = new QueryWrapper<>();

            if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
                queryWrapper.like("title", request.getKeyword())
                    .or()
                    .like("short_title", request.getKeyword());
            }

            if (request.getCategoryL1() != null && !request.getCategoryL1().isEmpty()) {
                queryWrapper.eq("category_l1", request.getCategoryL1());
            }

            if (request.getCategoryL2() != null && !request.getCategoryL2().isEmpty()) {
                queryWrapper.eq("category_l2", request.getCategoryL2());
            }

            if (request.getStatus() != null) {
                queryWrapper.eq("status", request.getStatus());
            }

            queryWrapper.orderByDesc("view_count", "created_at");

            List<LawDocument> lawDocuments = lawDocumentMapper.selectList(queryWrapper);

            if (lawDocuments == null || lawDocuments.isEmpty()) {
                return null;
            }

            List<LawSearchResponse.LawSearchItem> items = lawDocuments.stream()
                .map(this::convertToLawSearchItem)
                .collect(Collectors.toList());

            int total = items.size();
            int from = (request.getPage() - 1) * request.getPageSize();
            int to = Math.min(from + request.getPageSize(), total);

            List<LawSearchResponse.LawSearchItem> pagedItems = total > from
                ? items.subList(from, to)
                : Collections.emptyList();

            LawSearchResponse response = new LawSearchResponse();
            response.setTotal((long) total);
            response.setPage(request.getPage());
            response.setPageSize(request.getPageSize());
            response.setTookMs(System.currentTimeMillis() - startTime);
            response.setItems(pagedItems);

            return response;

        } catch (Exception e) {
            log.error("数据库查询法规失败: {}", e.getMessage());
            return null;
        }
    }

    private LawSearchResponse.LawSearchItem convertToLawSearchItem(LawDocument doc) {
        LawSearchResponse.LawSearchItem item = new LawSearchResponse.LawSearchItem();
        item.setLawUuid(doc.getLawUuid());
        item.setTitle(doc.getTitle());
        item.setShortTitle(doc.getShortTitle());
        item.setCategoryL1(doc.getCategoryL1());
        item.setCategoryL2(doc.getCategoryL2());
        item.setIssuingAuthority(doc.getIssuingAuthority());
        item.setIssueDate(doc.getIssueDate() != null ? doc.getIssueDate().toString() : "");
        item.setEffectiveDate(doc.getEffectiveDate() != null ? doc.getEffectiveDate().toString() : "");
        item.setStatus(doc.getStatus());
        item.setStatusName(getStatusName(doc.getStatus()));
        item.setViewCount(doc.getViewCount());
        item.setSourceUrl(doc.getSourceUrl());
        item.setSourceName(doc.getSourceName());

        QueryWrapper<LawArticle> articleQuery = new QueryWrapper<>();
        articleQuery.eq("law_id", doc.getId());
        item.setArticleCount(Long.valueOf(lawArticleMapper.selectCount(articleQuery)).intValue());

        return item;
    }

    private LawSearchResponse mockSearchLaws(LawSearchRequest request) {
        long startTime = System.currentTimeMillis();

        List<LawSearchResponse.LawSearchItem> allLaws = generateMockLaws(request);

        List<LawSearchResponse.LawSearchItem> filteredLaws = applyLawFilters(allLaws, request);

        int total = filteredLaws.size();
        int from = (request.getPage() - 1) * request.getPageSize();
        int to = Math.min(from + request.getPageSize(), total);

        List<LawSearchResponse.LawSearchItem> pagedLaws = total > from
            ? filteredLaws.subList(from, to)
            : Collections.emptyList();

        LawSearchResponse response = new LawSearchResponse();
        response.setTotal((long) total);
        response.setPage(request.getPage());
        response.setPageSize(request.getPageSize());
        response.setTookMs(System.currentTimeMillis() - startTime);
        response.setItems(pagedLaws);

        return response;
    }

    private LawSearchResponse esSearchLaws(LawSearchRequest request) {
        long startTime = System.currentTimeMillis();

        if (!esConfig.isEnabled() || !elasticsearchService.isAvailable()) {
            log.info("ES不可用，使用AI生成法规检索结果");
            return aiGenerateLaws(request, startTime);
        }

        Map<String, Object> filters = new HashMap<>();
        if (request.getCategoryL1() != null) {
            filters.put("category_l1", request.getCategoryL1());
        }
        if (request.getStatus() != null) {
            filters.put("status", request.getStatus());
        }

        List<LegalSearchResponse.SearchResultItem> esResults;
        try {
            esResults = elasticsearchService.searchByES(
                request.getKeyword(),
                request.getPage(),
                request.getPageSize(),
                filters
            );
        } catch (Exception e) {
            log.warn("ES检索失败: {}，使用AI生成", e.getMessage());
            return aiGenerateLaws(request, startTime);
        }

        List<LawSearchResponse.LawSearchItem> items = esResults.stream()
            .map(this::convertToLawSearchItem)
            .collect(Collectors.toList());

        LawSearchResponse response = new LawSearchResponse();
        response.setTotal((long) items.size());
        response.setPage(request.getPage());
        response.setPageSize(request.getPageSize());
        response.setTookMs(System.currentTimeMillis() - startTime);
        response.setItems(items);

        return response;
    }

    private LawSearchResponse aiGenerateLaws(LawSearchRequest request, long startTime) {
        log.info("使用AI生成法规检索结果: keyword={}", request.getKeyword());

        String prompt = buildLawSearchPrompt(request);

        try {
            String aiResponse = aiService.chat(prompt);
            return parseAIResponse(aiResponse, request, startTime);
        } catch (IOException e) {
            log.error("AI生成法规失败: {}", e.getMessage());
            return mockSearchLaws(request);
        }
    }

    private String buildLawSearchPrompt(LawSearchRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位专业的中国法律法规检索专家。请使用网络搜索能力，查找与用户关键词相关的法规。\n\n");
        sb.append("【检索关键词】").append(request.getKeyword() != null ? request.getKeyword() : "").append("\n\n");

        if (request.getCategoryL1() != null) {
            sb.append("【法规类别】").append(request.getCategoryL1()).append("\n\n");
        }
        if (request.getStatus() != null) {
            sb.append("【法规状态】").append(getStatusName(request.getStatus())).append("\n\n");
        }

        sb.append("请执行以下搜索：\n");
        sb.append("1. 搜索相关法规的基本信息（标题、发布机关、发布日期等）\n");
        sb.append("2. 搜索法规的现行状态和有效性\n\n");

        sb.append("请返回最相关的法规，采用JSON数组格式：\n\n");
        sb.append("[\n");
        sb.append("  {\n");
        sb.append("    \"lawUuid\": \"LAW-2023-001\",\n");
        sb.append("    \"title\": \"中华人民共和国民法典\",\n");
        sb.append("    \"shortTitle\": \"民法典\",\n");
        sb.append("    \"categoryL1\": \"法律\",\n");
        sb.append("    \"categoryL2\": \"民法\",\n");
        sb.append("    \"issuingAuthority\": \"全国人民代表大会\",\n");
        sb.append("    \"issueDate\": \"2020-05-28\",\n");
        sb.append("    \"effectiveDate\": \"2021-01-01\",\n");
        sb.append("    \"status\": 1,\n");
        sb.append("    \"articleCount\": 1260,\n");
        sb.append("    \"viewCount\": 156789,\n");
        sb.append("    \"sourceUrl\": \"https://flk.npc.gov.cn/\",\n");
        sb.append("    \"sourceName\": \"国家法律法规信息库\"\n");
        sb.append("  }\n");
        sb.append("]\n\n");
        sb.append("status说明：1=现行有效，2=已废止，3=修订中，4=尚未生效，5=部分失效。\n");
        sb.append("只返回JSON数组，不要有其他解释性文字。");

        return sb.toString();
    }

    private String getStatusName(Integer status) {
        if (status == null) return "全部";
        return switch (status) {
            case 1 -> "现行有效";
            case 2 -> "已废止";
            case 3 -> "修订中";
            case 4 -> "尚未生效";
            case 5 -> "部分失效";
            default -> "全部";
        };
    }

    private LawSearchResponse parseAIResponse(String aiResponse, LawSearchRequest request, long startTime) {
        java.util.List<LawSearchResponse.LawSearchItem> items = new java.util.ArrayList<>();

        String jsonContent = extractJsonFromResponse(aiResponse);
        if (jsonContent == null || jsonContent.isEmpty()) {
            log.error("无法从AI响应中提取JSON内容");
            return mockSearchLaws(request);
        }

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(jsonContent);

            if (node.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode item : node) {
                    LawSearchResponse.LawSearchItem lawItem = parseLawItem(item);
                    if (lawItem != null && lawItem.getTitle() != null && !lawItem.getTitle().isEmpty()) {
                        items.add(lawItem);
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析AI法规响应失败: {}", e.getMessage());
        }

        if (items.isEmpty()) {
            return mockSearchLaws(request);
        }

        for (LawSearchResponse.LawSearchItem item : items) {
            saveLawToDatabase(item);
        }

        int from = (request.getPage() - 1) * request.getPageSize();
        int to = Math.min(from + request.getPageSize(), items.size());
        List<LawSearchResponse.LawSearchItem> pagedItems = from < items.size() ? items.subList(from, to) : java.util.Collections.emptyList();

        LawSearchResponse response = new LawSearchResponse();
        response.setTotal((long) items.size());
        response.setPage(request.getPage());
        response.setPageSize(request.getPageSize());
        response.setTookMs(System.currentTimeMillis() - startTime);
        response.setItems(pagedItems);

        return response;
    }

    private LawSearchResponse.LawSearchItem parseLawItem(com.fasterxml.jackson.databind.JsonNode item) {
        try {
            LawSearchResponse.LawSearchItem lawItem = new LawSearchResponse.LawSearchItem();
            lawItem.setLawUuid(item.has("lawUuid") ? item.get("lawUuid").asText() : "AI-" + System.currentTimeMillis());
            lawItem.setTitle(item.has("title") ? item.get("title").asText() : "");
            lawItem.setShortTitle(item.has("shortTitle") ? item.get("shortTitle").asText() : lawItem.getTitle());
            lawItem.setCategoryL1(item.has("categoryL1") ? item.get("categoryL1").asText() : "法律");
            lawItem.setCategoryL2(item.has("categoryL2") ? item.get("categoryL2").asText() : "民法");
            lawItem.setIssuingAuthority(item.has("issuingAuthority") ? item.get("issuingAuthority").asText() : "");
            lawItem.setIssueDate(item.has("issueDate") ? item.get("issueDate").asText() : "");
            lawItem.setEffectiveDate(item.has("effectiveDate") ? item.get("effectiveDate").asText() : "");
            lawItem.setStatus(item.has("status") ? item.get("status").asInt() : 1);
            lawItem.setStatusName(getStatusName(item.has("status") ? item.get("status").asInt() : 1));
            lawItem.setArticleCount(item.has("articleCount") ? item.get("articleCount").asInt() : 0);
            lawItem.setViewCount(item.has("viewCount") ? item.get("viewCount").asInt() : 0);
            lawItem.setSourceUrl(item.has("sourceUrl") ? item.get("sourceUrl").asText() : "https://flk.npc.gov.cn/");
            lawItem.setSourceName(item.has("sourceName") ? item.get("sourceName").asText() : "国家法律法规信息库");
            return lawItem;
        } catch (Exception e) {
            log.error("解析法规项失败: {}", e.getMessage());
            return null;
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

    private void saveLawToDatabase(LawSearchResponse.LawSearchItem item) {
        try {
            QueryWrapper<LawDocument> checkWrapper = new QueryWrapper<>();
            checkWrapper.eq("law_uuid", item.getLawUuid());
            LawDocument existing = lawDocumentMapper.selectOne(checkWrapper);

            if (existing != null) {
                log.info("法规已存在，跳过保存: {}", item.getTitle());
                return;
            }

            LawDocument lawDocument = new LawDocument();
            lawDocument.setLawUuid(item.getLawUuid());
            lawDocument.setTitle(item.getTitle());
            lawDocument.setShortTitle(item.getShortTitle());
            lawDocument.setCategoryL1(item.getCategoryL1());
            lawDocument.setCategoryL2(item.getCategoryL2());
            lawDocument.setIssuingAuthority(item.getIssuingAuthority());
            if (item.getIssueDate() != null && !item.getIssueDate().isEmpty()) {
                try {
                    lawDocument.setIssueDate(java.time.LocalDate.parse(item.getIssueDate()));
                } catch (Exception e) {
                    log.debug("解析发布日期失败: {}", item.getIssueDate());
                }
            }
            if (item.getEffectiveDate() != null && !item.getEffectiveDate().isEmpty()) {
                try {
                    lawDocument.setEffectiveDate(java.time.LocalDate.parse(item.getEffectiveDate()));
                } catch (Exception e) {
                    log.debug("解析生效日期失败: {}", item.getEffectiveDate());
                }
            }
            lawDocument.setStatus(item.getStatus());
            lawDocument.setViewCount(item.getViewCount());
            lawDocument.setSourceUrl(item.getSourceUrl());
            lawDocument.setSourceName(item.getSourceName());

            lawDocumentMapper.insert(lawDocument);
            log.info("法规已保存到数据库: {}", item.getTitle());

            if (item.getArticleCount() != null && item.getArticleCount() > 0) {
                saveArticlesFromAI(item.getLawUuid(), item.getTitle());
            }

        } catch (Exception e) {
            log.error("保存法规到数据库失败: {}", e.getMessage());
        }
    }

    private void saveArticlesFromAI(String lawUuid, String lawTitle) {
        try {
            String prompt = "请搜索并列出《" + lawTitle + "》的主要条款（包括条款编号和内容摘要）。\n" +
                    "请以JSON数组格式返回：\n" +
                    "[\n" +
                    "  {\"articleNo\": \"第一条\", \"title\": \"条款标题\", \"content\": \"条款内容摘要\"}\n" +
                    "]\n" +
                    "只返回JSON数组，列出主要条款（不少于5条）。";

            String aiResponse = aiService.chat(prompt);
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(aiResponse);

            QueryWrapper<LawDocument> lawQuery = new QueryWrapper<>();
            lawQuery.eq("law_uuid", lawUuid);
            LawDocument lawDocument = lawDocumentMapper.selectOne(lawQuery);

            if (lawDocument == null || !node.isArray()) {
                return;
            }

            int sortOrder = 1;
            for (com.fasterxml.jackson.databind.JsonNode articleNode : node) {
                LawArticle article = new LawArticle();
                article.setLawId(lawDocument.getId());
                article.setArticleUuid("ART-" + lawUuid.substring(lawUuid.length() - 8) + "-" + String.format("%04d", sortOrder));
                article.setArticleNo(articleNode.has("articleNo") ? articleNode.get("articleNo").asText() : "第" + sortOrder + "条");
                article.setTitle(articleNode.has("title") ? articleNode.get("title").asText() : "");
                article.setContent(articleNode.has("content") ? articleNode.get("content").asText() : "");
                article.setSortOrder(sortOrder++);

                lawArticleMapper.insert(article);
            }

            log.info("法规条款已保存: {}, 共{}条", lawTitle, sortOrder - 1);

        } catch (Exception e) {
            log.error("保存法规条款失败: {}", e.getMessage());
        }
    }

    private LawSearchResponse.LawSearchItem convertToLawSearchItem(LegalSearchResponse.SearchResultItem item) {
        LawSearchResponse.LawSearchItem lawItem = new LawSearchResponse.LawSearchItem();
        lawItem.setLawUuid(item.getArticleId());
        lawItem.setTitle(item.getLawTitle());
        lawItem.setShortTitle(item.getLawTitle());
        lawItem.setCategoryL1(item.getCategoryL1());
        lawItem.setCategoryL2(item.getCategoryL2());
        lawItem.setSourceUrl(item.getSourceUrl());
        lawItem.setSourceName(item.getSourceName());
        return lawItem;
    }

    private List<LawSearchResponse.LawSearchItem> generateMockLaws(LawSearchRequest request) {
        List<LawSearchResponse.LawSearchItem> laws = new ArrayList<>();

        laws.add(createLawItem("LAW-2023-001", "中华人民共和国民法典", "民法典", "法律", "民法",
            "全国人民代表大会", "2020-05-28", "2021-01-01", 1, 1260, 156789));
        laws.add(createLawItem("LAW-2023-002", "中华人民共和国劳动合同法", "劳动合同法", "法律", "劳动法",
            "全国人民代表大会常务委员会", "2012-12-28", "2013-07-01", 1, 89, 89456));
        laws.add(createLawItem("LAW-2023-003", "中华人民共和国公司法", "公司法", "法律", "商法",
            "全国人民代表大会常务委员会", "2023-12-29", "2024-07-01", 4, 216, 45678));
        laws.add(createLawItem("LAW-2023-004", "中华人民共和国刑法", "刑法", "法律", "刑法",
            "全国人民代表大会", "2023-12-29", "2024-03-01", 1, 452, 234567));
        laws.add(createLawItem("LAW-2023-005", "最高人民法院关于审理建设工程施工合同纠纷案件适用法律问题的解释（一）",
            "建设工程施工合同司法解释", "司法解释", "民法", "最高人民法院", "2020-12-29", "2021-01-01", 1, 26, 67890));
        laws.add(createLawItem("LAW-2023-006", "中华人民共和国消费者权益保护法", "消费者权益保护法", "法律", "经济法",
            "全国人民代表大会常务委员会", "2023-10-25", "2024-01-01", 1, 69, 34567));
        laws.add(createLawItem("LAW-2023-007", "中华人民共和国行政处罚法", "行政处罚法", "法律", "行政法",
            "全国人民代表大会", "2021-01-22", "2021-07-15", 1, 63, 23456));

        return laws;
    }

    private LawSearchResponse.LawSearchItem createLawItem(String lawUuid, String title, String shortTitle,
            String categoryL1, String categoryL2, String issuingAuthority,
            String issueDate, String effectiveDate, int status, int articleCount, int viewCount) {
        LawSearchResponse.LawSearchItem item = new LawSearchResponse.LawSearchItem();
        item.setLawUuid(lawUuid);
        item.setTitle(title);
        item.setShortTitle(shortTitle);
        item.setCategoryL1(categoryL1);
        item.setCategoryL2(categoryL2);
        item.setIssuingAuthority(issuingAuthority);
        item.setIssueDate(issueDate);
        item.setEffectiveDate(effectiveDate);
        item.setStatus(status);
        item.setStatusName(getStatusName(status));
        item.setArticleCount(articleCount);
        item.setViewCount(viewCount);
        item.setSourceUrl("https://flk.npc.gov.cn/");
        item.setSourceName("国家法律法规信息库");
        return item;
    }

    private List<LawSearchResponse.LawSearchItem> applyLawFilters(
            List<LawSearchResponse.LawSearchItem> laws,
            LawSearchRequest request) {

        return laws.stream()
            .filter(law -> {
                if (request.getCategoryL1() != null && !request.getCategoryL1().equals(law.getCategoryL1())) {
                    return false;
                }
                if (request.getCategoryL2() != null && !request.getCategoryL2().equals(law.getCategoryL2())) {
                    return false;
                }
                if (request.getStatus() != null && !request.getStatus().equals(law.getStatus())) {
                    return false;
                }
                if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
                    String keyword = request.getKeyword().toLowerCase();
                    if (!law.getTitle().toLowerCase().contains(keyword)) {
                        return false;
                    }
                }
                return true;
            })
            .collect(Collectors.toList());
    }

    private String getStatusName(int status) {
        return switch (status) {
            case 1 -> "现行有效";
            case 2 -> "已废止";
            case 3 -> "修订中";
            case 4 -> "尚未生效";
            case 5 -> "部分失效";
            default -> "未知";
        };
    }

    public Map<String, Object> getCategories() {
        Map<String, Object> result = new HashMap<>();
        result.put("categoryL1", CATEGORIES_L1);
        result.put("categoryL2", CATEGORIES_L2);
        result.put("statusOptions", List.of(
            Map.of("value", 1, "label", "现行有效"),
            Map.of("value", 2, "label", "已废止"),
            Map.of("value", 3, "label", "修订中"),
            Map.of("value", 4, "label", "尚未生效"),
            Map.of("value", 5, "label", "部分失效")
        ));
        return result;
    }

    public LawSearchResponse.LawSearchItem getLawDetail(String lawUuid) {
        log.info("获取法规详情: lawUuid={}", lawUuid);

        try {
            QueryWrapper<LawDocument> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("law_uuid", lawUuid);
            LawDocument lawDocument = lawDocumentMapper.selectOne(queryWrapper);

            if (lawDocument != null) {
                return convertToLawSearchItem(lawDocument);
            }
        } catch (Exception e) {
            log.error("数据库查询法规详情失败: {}", e.getMessage());
        }

        LawSearchRequest request = new LawSearchRequest();
        request.setKeyword("");
        request.setPageSize(100);

        LawSearchResponse response = mockSearchLaws(request);

        return response.getItems().stream()
            .filter(item -> item.getLawUuid().equals(lawUuid))
            .findFirst()
            .orElse(null);
    }

    public LawDocument getLawDocument(String lawUuid) {
        try {
            QueryWrapper<LawDocument> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("law_uuid", lawUuid);
            return lawDocumentMapper.selectOne(queryWrapper);
        } catch (Exception e) {
            log.error("获取法规文档失败: {}", e.getMessage());
            return null;
        }
    }

    public List<LawArticle> getLawArticles(String lawUuid) {
        try {
            LawDocument lawDocument = getLawDocument(lawUuid);
            if (lawDocument == null) {
                return Collections.emptyList();
            }

            QueryWrapper<LawArticle> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("law_id", lawDocument.getId());
            queryWrapper.orderByAsc("sort_order", "article_no");

            return lawArticleMapper.selectList(queryWrapper);
        } catch (Exception e) {
            log.error("获取法规条款失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}