package com.legalai.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalai.config.ElasticsearchConfig;
import com.legalai.dto.*;
import com.legalai.model.LawArticle;
import com.legalai.model.LawDocument;
import com.legalai.model.LawCategory;
import com.legalai.model.LawCategoryType;
import com.legalai.repository.LawArticleMapper;
import com.legalai.repository.LawDocumentMapper;
import com.legalai.repository.LawCategoryMapper;
import com.legalai.repository.LawCategoryTypeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LawSearchService {
    private static final Logger log = LoggerFactory.getLogger(LawSearchService.class);

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

    @Autowired
    private LawCategoryMapper lawCategoryMapper;

    @Autowired
    private LawCategoryTypeMapper lawCategoryTypeMapper;

    public LawSearchResponse searchLaws(LawSearchRequest request) {
        log.info("法规查询请求: keyword={}, categoryL1={}, status={}",
            request.getKeyword(), request.getCategoryL1(), request.getStatus());

        LawSearchResponse dbResponse = dbSearchLaws(request);
        if (dbResponse != null && dbResponse.getTotal() > 0) {
            return dbResponse;
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

            Map<Long, Integer> articleCountMap = batchQueryArticleCounts(lawDocuments);

            List<LawSearchResponse.LawSearchItem> items = lawDocuments.stream()
                .map(doc -> convertToLawSearchItem(doc, articleCountMap))
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

    private Map<Long, Integer> batchQueryArticleCounts(List<LawDocument> documents) {
        Map<Long, Integer> articleCountMap = new HashMap<>();
        if (documents == null || documents.isEmpty()) {
            return articleCountMap;
        }
        List<Long> docIds = documents.stream()
            .map(LawDocument::getId)
            .collect(Collectors.toList());

        QueryWrapper<LawArticle> countQuery = new QueryWrapper<>();
        countQuery.select("law_id", "COUNT(*) as cnt")
            .in("law_id", docIds)
            .groupBy("law_id");
        List<Map<String, Object>> countResults = lawArticleMapper.selectMaps(countQuery);

        for (Map<String, Object> row : countResults) {
            Object lawIdObj = row.get("law_id");
            Object cntObj = row.get("cnt");
            if (lawIdObj != null && cntObj != null) {
                Long lawId = ((Number) lawIdObj).longValue();
                int count = ((Number) cntObj).intValue();
                articleCountMap.put(lawId, count);
            }
        }
        return articleCountMap;
    }

    private LawSearchResponse.LawSearchItem convertToLawSearchItem(LawDocument doc, Map<Long, Integer> articleCountMap) {
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
        item.setArticleCount(articleCountMap.getOrDefault(doc.getId(), 0));
        return item;
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
            throw new IllegalStateException("AI生成法规失败，无法返回结果: " + e.getMessage());
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
        List<LawSearchResponse.LawSearchItem> items = new ArrayList<>();

        String jsonContent = extractJsonFromResponse(aiResponse);
        if (jsonContent == null || jsonContent.isEmpty()) {
            log.error("无法从AI响应中提取JSON内容");
            throw new IllegalStateException("无法从AI响应中提取JSON内容");
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(jsonContent);

            if (node.isArray()) {
                for (JsonNode item : node) {
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
            throw new IllegalStateException("AI未能返回有效的法规数据");
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

    private LawSearchResponse.LawSearchItem parseLawItem(JsonNode item) {
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
            ObjectMapper mapper2 = new ObjectMapper();
            JsonNode node = mapper2.readTree(aiResponse);

            QueryWrapper<LawDocument> lawQuery = new QueryWrapper<>();
            lawQuery.eq("law_uuid", lawUuid);
            LawDocument lawDocument = lawDocumentMapper.selectOne(lawQuery);

            if (lawDocument == null || !node.isArray()) {
                return;
            }

            int sortOrder = 1;
            for (JsonNode articleNode : node) {
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

        try {
            QueryWrapper<LawCategoryType> typeQuery = new QueryWrapper<>();
            typeQuery.orderByAsc("sort_order");
            List<LawCategoryType> categoryTypes = lawCategoryTypeMapper.selectList(typeQuery);

            List<Map<String, String>> categoryL1 = categoryTypes.stream()
                .map(t -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("code", t.getTypeCode() != null ? t.getTypeCode() : t.getTypeName());
                    map.put("name", t.getTypeName());
                    return map;
                })
                .collect(Collectors.toList());
            result.put("categoryL1", categoryL1);

            QueryWrapper<LawCategory> catQuery = new QueryWrapper<>();
            catQuery.orderByAsc("sort_order");
            List<LawCategory> categories = lawCategoryMapper.selectList(catQuery);

            List<Map<String, String>> categoryL2 = categories.stream()
                .filter(c -> c.getParentId() == null || c.getParentId() == 0)
                .map(c -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("code", c.getCategoryCode() != null ? c.getCategoryCode() : c.getCategoryName());
                    map.put("name", c.getCategoryName());
                    return map;
                })
                .collect(Collectors.toList());
            result.put("categoryL2", categoryL2);

        } catch (Exception e) {
            log.warn("从数据库加载分类失败，使用默认分类: {}", e.getMessage());
            result.put("categoryL1", List.of(
                Map.of("code", "法律", "name", "法律"),
                Map.of("code", "行政法规", "name", "行政法规"),
                Map.of("code", "部门规章", "name", "部门规章"),
                Map.of("code", "地方性法规", "name", "地方性法规"),
                Map.of("code", "司法解释", "name", "司法解释")
            ));
            result.put("categoryL2", List.of(
                Map.of("code", "民法", "name", "民法", "parent", "法律"),
                Map.of("code", "商法", "name", "商法", "parent", "法律"),
                Map.of("code", "刑法", "name", "刑法", "parent", "法律"),
                Map.of("code", "行政法", "name", "行政法", "parent", "法律"),
                Map.of("code", "劳动法", "name", "劳动法", "parent", "法律"),
                Map.of("code", "知识产权法", "name", "知识产权法", "parent", "法律"),
                Map.of("code", "诉讼法", "name", "诉讼法", "parent", "法律")
            ));
        }

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
                return convertToLawSearchItem(lawDocument, new HashMap<>());
            }
        } catch (Exception e) {
            log.error("数据库查询法规详情失败: {}", e.getMessage());
        }

        throw new IllegalStateException("法规未找到: " + lawUuid);
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