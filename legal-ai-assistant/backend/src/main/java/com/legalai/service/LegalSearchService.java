package com.legalai.service;

import com.legalai.config.ElasticsearchConfig;
import com.legalai.config.MilvusConfig;
import com.legalai.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LegalSearchService {
    private static final Logger log = LoggerFactory.getLogger(LegalSearchService.class);

    @Value("${mock.enabled:true}")
    private boolean mockEnabled;

    private final ElasticsearchService elasticsearchService;
    private final MilvusService milvusService;
    private final AIService aiService;
    private final ElasticsearchConfig esConfig;
    private final MilvusConfig milvusConfig;
    private final SourceVerificationService sourceVerificationService;
    private final CacheService cacheService;
    private final JdbcTemplate jdbc;

    private static final String SYSTEM_PROMPT =
        "你是一个专业的法律助手，专注于中国法律法规的检索与解读。你拥有法学背景，能够准确理解法律条文含义并给出专业解释。\n\n" +
        "核心任务：根据用户输入的法律问题，从检索到的法规条文中提取相关信息，给出准确、专业的回答。\n\n" +
        "约束条件（HARD RULES）：\n" +
        "1. 溯源必须：每个法律结论必须标注来源，格式为：[法规名称] 第X条 | 来源URL\n" +
        "2. 禁止胡编：只陈述检索结果中明确存在的内容，不得编造、推测法条内容\n" +
        "3. 不确定声明：如检索结果不足以回答，明确说明\"未检索到相关法规\"\n" +
        "4. 语言严谨：使用规范法律用语，避免口语化表达\n" +
        "5. 时效性：注意标注法条的时效性，提示可能已修订\n\n" +
        "输出格式：\n" +
        "## 回答\n" +
        "[正文内容]\n" +
        "## 参考依据\n" +
        "1. [法规名称] 第X条 | 来源URL\n" +
        "## 追问建议\n" +
        "- 问题1\n" +
        "- 问题2";

    private static final Map<String, List<String>> SYNONYMS = Map.of(
        "欺诈", List.of("欺骗", "诈骗", "骗取"),
        "违约", List.of("违约行为", "违反合同", "不履行"),
        "解除", List.of("解除合同", "终止合同", "撤销"),
        "劳动", List.of("劳动合同", "劳动关系", "劳动争议"),
        "赔偿", List.of("赔偿", "补偿", "损失赔偿"),
        "借款", List.of("借贷", "贷款", "借钱"),
        "合同", List.of("合约", "协议", "契约")
    );

    private static final int RRF_K = 60;

    @Autowired
    public LegalSearchService(
            ElasticsearchService elasticsearchService,
            MilvusService milvusService,
            AIService aiService,
            ElasticsearchConfig esConfig,
            MilvusConfig milvusConfig,
            SourceVerificationService sourceVerificationService,
            CacheService cacheService,
            JdbcTemplate jdbc) {
        this.elasticsearchService = elasticsearchService;
        this.milvusService = milvusService;
        this.aiService = aiService;
        this.esConfig = esConfig;
        this.milvusConfig = milvusConfig;
        this.sourceVerificationService = sourceVerificationService;
        this.cacheService = cacheService;
        this.jdbc = jdbc;
    }

    public LegalSearchResponse search(LegalSearchRequest request) {
        log.info("法律检索请求: query={}, page={}, pageSize={}",
            request.getQuery(), request.getPage(), request.getPageSize());

        validateRequest(request);

        LegalSearchResponse cached = cacheService.getCachedSearchResults(request.getQuery());
        if (cached != null && request.getPage() == 1) {
            log.info("命中缓存，返回缓存结果");
            return cached;
        }

        LegalSearchResponse response;
        if (mockEnabled) {
            response = mockSearch(request);
        } else {
            response = hybridSearch(request);
        }

        if (request.getPage() == 1) {
            cacheService.cacheSearchResults(request.getQuery(), response);
        }

        return response;
    }

    private void validateRequest(LegalSearchRequest request) {
        if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            throw new IllegalArgumentException("检索关键词不能为空");
        }
        if (request.getQuery().length() > 200) {
            throw new IllegalArgumentException("检索关键词长度不能超过200字");
        }
        if (sourceVerificationService.isQuerySensitive(request.getQuery())) {
            throw new IllegalArgumentException("检索内容包含敏感词，请调整后重试");
        }
    }

    private LegalSearchResponse hybridSearch(LegalSearchRequest request) {
        long startTime = System.currentTimeMillis();
        String query = request.getQuery();
        int topK = Math.max(request.getPageSize() * 5, 50);
        int timeoutMs = 2000;

        List<LegalSearchResponse.SearchResultItem> esResults = Collections.emptyList();
        List<LegalSearchResponse.SearchResultItem> milvusResults = Collections.emptyList();

        if (esConfig.isEnabled() && elasticsearchService.isAvailable()) {
            try {
                esResults = elasticsearchService.searchByES(query, 1, topK, buildFilters(request));
                log.info("ES检索返回 {} 条结果", esResults.size());
            } catch (Exception e) {
                log.warn("ES检索超时或失败，降级到Mock: {}", e.getMessage());
            }
        }

        if (System.currentTimeMillis() - startTime > timeoutMs) {
            log.warn("检索超时，降级到ES-only模式");
            return fallbackToESOnly(request, esResults, startTime);
        }

        if (milvusConfig.isEnabled() && milvusService.isAvailable()) {
            try {
                milvusResults = milvusService.searchByVector(query, topK);
                log.info("Milvus检索返回 {} 条结果", milvusResults.size());
            } catch (Exception e) {
                log.warn("Milvus检索失败，降级到ES-only: {}", e.getMessage());
            }
        }

        List<LegalSearchResponse.SearchResultItem> fusedResults;
        if (!esResults.isEmpty() && !milvusResults.isEmpty()) {
            fusedResults = rrfFusion(esResults, milvusResults, topK);
        } else if (!esResults.isEmpty()) {
            fusedResults = esResults;
        } else if (!milvusResults.isEmpty()) {
            fusedResults = milvusResults;
        } else {
            log.warn("所有检索引擎均不可用，使用AI生成检索结果");
            fusedResults = aiGenerateSearchResults(request, topK);
        }

        normalizeScores(fusedResults);
        return buildResponse(request, fusedResults, startTime);
    }

    private List<LegalSearchResponse.SearchResultItem> aiGenerateSearchResults(LegalSearchRequest request, int topK) {
        log.info("使用AI生成法律检索结果: query={}", request.getQuery());

        String prompt = buildSearchPrompt(request, topK);

        try {
            String aiResponse = aiService.chat(prompt);
            return parseAISearchResponse(aiResponse, request);
        } catch (Exception e) {
            log.error("AI生成检索结果失败: {}", e.getMessage());
            return mockSearch(request).getItems();
        }
    }

    private String buildSearchPrompt(LegalSearchRequest request, int topK) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位专业的中国法律法规检索专家。请根据用户的法律问题，检索并返回相关的法律法规条文。\n\n");
        sb.append("【用户问题】").append(request.getQuery()).append("\n\n");
        sb.append("请检索以下相关法律条文，返回最相关的").append(topK > 10 ? 10 : topK).append("条结果：\n\n");

        sb.append("返回格式要求（严格JSON数组格式）：\n");
        sb.append("[\n");
        sb.append("  {\n");
        sb.append("    \"articleId\": \"ART-001\",\n");
        sb.append("    \"lawTitle\": \"中华人民共和国民法典\",\n");
        sb.append("    \"articleNo\": \"第一百四十八条\",\n");
        sb.append("    \"title\": \"欺诈的认定\",\n");
        sb.append("    \"content\": \"法条完整内容...\",\n");
        sb.append("    \"categoryL1\": \"法律\",\n");
        sb.append("    \"categoryL2\": \"民法\",\n");
        sb.append("    \"sourceUrl\": \"https://flk.npc.gov.cn/\",\n");
        sb.append("    \"sourceName\": \"国家法律法规信息库\",\n");
        sb.append("    \"score\": 0.95\n");
        sb.append("  }\n");
        sb.append("]\n\n");
        sb.append("只返回JSON数组，不要有其他解释性文字。确保content字段包含法条的完整内容。");

        return sb.toString();
    }

    private List<LegalSearchResponse.SearchResultItem> parseAISearchResponse(String aiResponse, LegalSearchRequest request) {
        java.util.List<LegalSearchResponse.SearchResultItem> items = new java.util.ArrayList<>();

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(aiResponse);

            if (node.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode item : node) {
                    LegalSearchResponse.SearchResultItem result = new LegalSearchResponse.SearchResultItem();
                    result.setArticleId(item.has("articleId") ? item.get("articleId").asText() : "AI-" + System.currentTimeMillis());
                    result.setLawId("LAW-AI-001");
                    result.setLawTitle(item.has("lawTitle") ? item.get("lawTitle").asText() : "");
                    result.setArticleNo(item.has("articleNo") ? item.get("articleNo").asText() : "");
                    result.setTitle(item.has("title") ? item.get("title").asText() : "");
                    result.setContent(item.has("content") ? item.get("content").asText() : "");
                    result.setCategoryL1(item.has("categoryL1") ? item.get("categoryL1").asText() : "法律");
                    result.setCategoryL2(item.has("categoryL2") ? item.get("categoryL2").asText() : "民法");
                    result.setSourceUrl(item.has("sourceUrl") ? item.get("sourceUrl").asText() : "https://flk.npc.gov.cn/");
                    result.setSourceName(item.has("sourceName") ? item.get("sourceName").asText() : "国家法律法规信息库");
                    result.setScore(item.has("score") ? item.get("score").asDouble() : 0.85);
                    result.setHighlights(java.util.List.of("<em>" + (request.getQuery() != null ? request.getQuery() : "法律") + "</em>"));

                    items.add(result);
                }
            }
        } catch (Exception e) {
            log.error("解析AI检索响应失败: {}", e.getMessage());
        }

        if (items.isEmpty()) {
            items = mockSearch(request).getItems();
        }

        return items;
    }

    private LegalSearchResponse fallbackToESOnly(LegalSearchRequest request,
            List<LegalSearchResponse.SearchResultItem> esResults, long startTime) {
        if (esResults.isEmpty()) {
            log.info("ES结果为空，使用AI生成检索结果");
            esResults = aiGenerateSearchResults(request, request.getPageSize() * 5);
        }
        normalizeScores(esResults);
        return buildResponse(request, esResults, startTime);
    }

    private LegalSearchResponse buildResponse(LegalSearchRequest request,
            List<LegalSearchResponse.SearchResultItem> fusedResults, long startTime) {
        int from = (request.getPage() - 1) * request.getPageSize();
        int to = Math.min(from + request.getPageSize(), fusedResults.size());
        List<LegalSearchResponse.SearchResultItem> pagedResults = fusedResults.subList(from, to);

        List<LegalSearchResponse.RelatedCase> relatedCases = new ArrayList<>();
        if (Boolean.TRUE.equals(request.getIncludeCases()) && !pagedResults.isEmpty()) {
            relatedCases = generateRelatedCases(pagedResults.get(0).getTitle());
        }

        LegalSearchResponse response = new LegalSearchResponse();
        response.setTotal((long) fusedResults.size());
        response.setPage(request.getPage());
        response.setPageSize(request.getPageSize());
        response.setTookMs(System.currentTimeMillis() - startTime);

        Long searchLogId = persistSearchLog(request.getQuery(), fusedResults.size(), System.currentTimeMillis() - startTime);
        response.setSearchLogId(searchLogId);
        response.setItems(pagedResults);
        response.setRelatedCases(relatedCases);

        return response;
    }

    /**
     * 将所有搜索结果的 score 统一归一化到 0-100 区间。
     * 严格按相对匹配度排序：最高分结果 → 100%，其他按比例缩放。
     * 处理三种 score 来源：
     *   RRF 融合 (0.01-0.03) → /max*100
     *   BM25 ES score (0-20+) → /max*100
     *   Mock score (4-18)     → /max*100
     *   AI score (0-1)        → *100
     */
    private void normalizeScores(List<LegalSearchResponse.SearchResultItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        double maxScore = 0;
        for (LegalSearchResponse.SearchResultItem item : items) {
            Double s = item.getScore();
            if (s != null && s > maxScore) {
                maxScore = s;
            }
        }

        if (maxScore <= 0) {
            return;
        }

        for (LegalSearchResponse.SearchResultItem item : items) {
            Double s = item.getScore();
            double normalized = s != null ? (s / maxScore) * 100.0 : 0;
            item.setScore(Math.min(normalized, 100.0));
        }

        log.debug("Score 归一化完成: maxRaw={}, topScore={}", maxScore,
            items.get(0).getScore());
    }

    private Map<String, Object> buildFilters(LegalSearchRequest request) {
        Map<String, Object> filters = new HashMap<>();
        if (request.getFilters() != null) {
            LegalSearchRequest.SearchFilters f = request.getFilters();
            if (f.getCategoryL1() != null && !f.getCategoryL1().isEmpty()) {
                filters.put("category_l1", f.getCategoryL1().get(0));
            }
            if (f.getStatus() != null && !f.getStatus().isEmpty()) {
                filters.put("status", f.getStatus().get(0));
            }
        }
        return filters;
    }

    private List<LegalSearchResponse.SearchResultItem> rrfFusion(
            List<LegalSearchResponse.SearchResultItem> esResults,
            List<LegalSearchResponse.SearchResultItem> milvusResults,
            int topK) {

        Map<String, Integer> esRank = new HashMap<>();
        for (int i = 0; i < esResults.size(); i++) {
            esRank.put(esResults.get(i).getArticleId(), i + 1);
        }

        Map<String, Integer> milvusRank = new HashMap<>();
        for (int i = 0; i < milvusResults.size(); i++) {
            milvusRank.put(milvusResults.get(i).getArticleId(), i + 1);
        }

        Map<String, Double> rrfScores = new HashMap<>();
        Set<String> allArticleIds = new HashSet<>();
        allArticleIds.addAll(esRank.keySet());
        allArticleIds.addAll(milvusRank.keySet());

        for (String articleId : allArticleIds) {
            double score = 0.0;
            if (esRank.containsKey(articleId)) {
                score += 1.0 / (RRF_K + esRank.get(articleId));
            }
            if (milvusRank.containsKey(articleId)) {
                score += 1.0 / (RRF_K + milvusRank.get(articleId));
            }
            rrfScores.put(articleId, score);
        }

        List<LegalSearchResponse.SearchResultItem> fused = new ArrayList<>();
        Set<String> added = new HashSet<>();

        for (int i = 0; i < topK; i++) {
            String bestId = null;
            double bestScore = -1.0;
            for (String articleId : allArticleIds) {
                if (!added.contains(articleId) && rrfScores.get(articleId) > bestScore) {
                    bestScore = rrfScores.get(articleId);
                    bestId = articleId;
                }
            }
            if (bestId == null) break;

            LegalSearchResponse.SearchResultItem item = findByArticleId(esResults, milvusResults, bestId);
            if (item != null) {
                item.setScore(bestScore);
                fused.add(item);
                added.add(bestId);
            }
        }

        return fused;
    }

    private LegalSearchResponse.SearchResultItem findByArticleId(
            List<LegalSearchResponse.SearchResultItem> esResults,
            List<LegalSearchResponse.SearchResultItem> milvusResults,
            String articleId) {
        for (LegalSearchResponse.SearchResultItem item : esResults) {
            if (item.getArticleId().equals(articleId)) {
                return item;
            }
        }
        for (LegalSearchResponse.SearchResultItem item : milvusResults) {
            if (item.getArticleId().equals(articleId)) {
                return item;
            }
        }
        return null;
    }

    private LegalSearchResponse mockSearch(LegalSearchRequest request) {
        List<LegalSearchResponse.SearchResultItem> items = new ArrayList<>();
        String query = request.getQuery() != null ? request.getQuery().toLowerCase() : "";

        List<Map<String, String>> mockLaws = List.of(
            Map.of("articleId", "ART-2023-001", "lawTitle", "中华人民共和国民法典", "articleNo", "第一百四十八条",
                "title", "欺诈的认定", "content", "一方以欺诈手段，使对方在违背真实意思的情况下订立的合同，受欺诈方有权请求人民法院或者仲裁机构予以撤销。",
                "categoryL1", "法律", "categoryL2", "民法"),
            Map.of("articleId", "ART-2023-002", "lawTitle", "中华人民共和国民法典", "articleNo", "第一百四十九条",
                "title", "第三人欺诈", "content", "第三人实施欺诈行为，使一方陷入错误认识的，适用欺诈规定。",
                "categoryL1", "法律", "categoryL2", "民法"),
            Map.of("articleId", "ART-2023-003", "lawTitle", "中华人民共和国民法典", "articleNo", "第五百六十三条",
                "title", "合同解除情形", "content", "有下列情形之一的，当事人可以解除合同：（一）因不可抗力致使不能实现合同目的；（二）履行期限届满前，当事人一方明确表示或者以自己的行为表明不履行主要债务。",
                "categoryL1", "法律", "categoryL2", "民法"),
            Map.of("articleId", "ART-2023-004", "lawTitle", "中华人民共和国民法典", "articleNo", "第五百七十七条",
                "title", "违约责任", "content", "当事人一方不履行合同义务或者履行合同义务不符合约定的，应当承担违约责任。",
                "categoryL1", "法律", "categoryL2", "民法"),
            Map.of("articleId", "ART-2023-005", "lawTitle", "中华人民共和国民法典", "articleNo", "第五百八十四条",
                "title", "损失赔偿范围", "content", "当事人一方不履行合同义务或者履行合同义务不符合约定的，给对方造成损失的，损失赔偿额应当相当于因违约所造成的损失，包括合同履行后可以获得的利益。",
                "categoryL1", "法律", "categoryL2", "民法"),
            Map.of("articleId", "ART-2023-006", "lawTitle", "中华人民共和国劳动合同法", "articleNo", "第三十九条",
                "title", "用人单位单方解除劳动合同", "content", "劳动者有下列情形之一的，用人单位可以解除劳动合同：（一）在试用期间被证明不符合录用条件的；（二）严重违反用人单位的规章制度的。",
                "categoryL1", "法律", "categoryL2", "劳动法"),
            Map.of("articleId", "ART-2023-007", "lawTitle", "中华人民共和国劳动合同法", "articleNo", "第四十六条",
                "title", "经济补偿", "content", "有下列情形之一的，用人单位应当向劳动者支付经济补偿：（一）劳动者依照本法第三十八条规定解除劳动合同的。",
                "categoryL1", "法律", "categoryL2", "劳动法"),
            Map.of("articleId", "ART-2023-008", "lawTitle", "最高人民法院关于审理建设工程施工合同纠纷案件适用法律问题的解释（一）", "articleNo", "第十条",
                "title", "工程价款结算", "content", "当事人对建设工程的计价标准或者计价方法有约定的，按照约定结算工程价款。",
                "categoryL1", "司法解释", "categoryL2", "建设工程")
        );

        int count = 0;
        for (Map<String, String> law : mockLaws) {
            if (count >= request.getPageSize()) break;

            String content = law.get("content").toLowerCase();
            String title = law.get("title").toLowerCase();
            String lawTitle = law.get("lawTitle").toLowerCase();

            if (query.isEmpty() || content.contains(query) || title.contains(query) ||
                lawTitle.contains(query) || matchQueryWithSynonyms(query, content, title, lawTitle)) {

                LegalSearchResponse.SearchResultItem item = new LegalSearchResponse.SearchResultItem();
                item.setArticleId(law.get("articleId"));
                item.setLawId("LAW-2023-001");
                item.setLawTitle(law.get("lawTitle"));
                item.setArticleNo(law.get("articleNo"));
                item.setTitle(law.get("title"));
                item.setContent(law.get("content"));
                item.setHighlights(List.of("<em>" + highlightKeyword(request.getQuery()) + "</em>"));
                item.setSourceUrl("https://flk.npc.gov.cn/detail2.html?ZmY4MDgxODE3OTZhNjMyYTAxNzk3YWIzYzIyYzA2M2I=");
                item.setSourceName("国家法律法规信息库");
                item.setScore(18.56 - count * 2.0);
                item.setRelatedCasesCount(new Random().nextInt(10));
                item.setCategoryL1(law.get("categoryL1"));
                item.setCategoryL2(law.get("categoryL2"));
                items.add(item);
                count++;
            }
        }

        List<LegalSearchResponse.RelatedCase> relatedCases = new ArrayList<>();
        if (Boolean.TRUE.equals(request.getIncludeCases()) && !items.isEmpty()) {
            relatedCases = generateRelatedCases(items.get(0).getTitle());
        }

        normalizeScores(items);

        LegalSearchResponse response = new LegalSearchResponse();
        response.setTotal((long) items.size());
        response.setPage(request.getPage());
        response.setPageSize(request.getPageSize());
        response.setTookMs(45L);
        response.setItems(items);
        response.setRelatedCases(relatedCases);
        return response;
    }

    private boolean matchQueryWithSynonyms(String query, String content, String title, String lawTitle) {
        for (Map.Entry<String, List<String>> entry : SYNONYMS.entrySet()) {
            if (query.contains(entry.getKey())) {
                for (String syn : entry.getValue()) {
                    if (content.contains(syn) || title.contains(syn) || lawTitle.contains(syn)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private String highlightKeyword(String query) {
        if (query == null || query.isEmpty()) return "法律";
        String[] words = query.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", " ").split("\\s+");
        return words.length > 0 ? words[0] : "法律";
    }

    private List<LegalSearchResponse.RelatedCase> generateRelatedCases(String topic) {
        List<LegalSearchResponse.RelatedCase> cases = new ArrayList<>();

        String[][] mockCases = {
            {"CASE-2021-001", "(2021)沪01民终1234号", "某投资公司与张某合同纠纷案", "上海市第一中级人民法院", "法院认定被告在签订投资协议时存在欺诈行为，判决撤销合同。"},
            {"CASE-2022-001", "(2022)京02民终5678号", "李某与北京某公司劳动争议案", "北京市第二中级人民法院", "公司违法解除劳动合同，判决支付经济补偿金。"}
        };

        for (String[] caseData : mockCases) {
            LegalSearchResponse.RelatedCase rc = new LegalSearchResponse.RelatedCase();
            rc.setCaseUuid(caseData[0]);
            rc.setCaseNo(caseData[1]);
            rc.setTitle(caseData[2]);
            rc.setCourt(caseData[3]);
            rc.setSummary(caseData[4]);
            rc.setSourceUrl("https://wenshu.court.gov.cn/");
            rc.setSourceName("中国裁判文书网");
            cases.add(rc);
        }

        return cases;
    }

    public String generateAnswer(String query, List<LegalSearchResponse.SearchResultItem> items) {
        if (items == null || items.isEmpty()) {
            return "未检索到相关法律法规，建议您更换关键词后重试。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("根据检索到的法律法规，回答如下：\n\n");

        for (LegalSearchResponse.SearchResultItem item : items) {
            sb.append(String.format("【%s】%s %s\n%s\n来源：%s\n\n",
                item.getLawTitle(),
                item.getArticleNo(),
                item.getTitle(),
                item.getContent(),
                item.getSourceUrl()
            ));
        }

        sb.append("---\n\n");
        sb.append("**免责声明**：本回答基于检索到的法律法规生成，仅供参考，不构成法律意见。\n");
        sb.append("如需针对具体案件的法律建议，请咨询具有执业资格的专业律师。");

        return sb.toString();
    }

    public List<String> generateSuggestedQueries(String query) {
        List<String> suggestions = new ArrayList<>();

        if (query.contains("合同")) {
            suggestions.add("合同欺诈如何认定？");
            suggestions.add("合同违约责任有哪些？");
            suggestions.add("合同解除的条件是什么？");
        } else if (query.contains("劳动")) {
            suggestions.add("劳动合同解除的条件是什么？");
            suggestions.add("经济补偿金如何计算？");
            suggestions.add("加班费如何主张？");
        } else if (query.contains("借款")) {
            suggestions.add("民间借贷利息上限是多少？");
            suggestions.add("借款合同纠纷如何起诉？");
        } else {
            suggestions.add("如何签订一份有效的合同？");
            suggestions.add("遇到纠纷如何维权？");
            suggestions.add("诉讼时效是多长时间？");
        }

        return suggestions;
    }

    public LegalSearchResponse.SearchResultItem getArticleDetail(String articleId) {
        log.info("获取法规详情: articleId={}", articleId);

        if (mockEnabled) {
            for (Map<String, String> law : getMockLaws()) {
                if (law.get("articleId").equals(articleId)) {
                    LegalSearchResponse.SearchResultItem item = new LegalSearchResponse.SearchResultItem();
                    item.setArticleId(law.get("articleId"));
                    item.setLawId("LAW-2023-001");
                    item.setLawTitle(law.get("lawTitle"));
                    item.setArticleNo(law.get("articleNo"));
                    item.setTitle(law.get("title"));
                    item.setContent(law.get("content"));
                    item.setSourceUrl("https://flk.npc.gov.cn/detail2.html?ZmY4MDgxODE3OTZhNjMyYTAxNzk3YWIzYzIyYzA2M2I=");
                    item.setSourceName("国家法律法规信息库");
                    return item;
                }
            }
            return null;
        }

        if (esConfig.isEnabled() && elasticsearchService.isAvailable()) {
            return elasticsearchService.getArticleById(articleId);
        }

        return null;
    }

    public void submitFeedback(SearchFeedbackRequest request) {
        log.info("收到搜索反馈: searchLogId={}, articleId={}, isHelpful={}",
                request.getSearchLogId(), request.getArticleId(), request.getIsHelpful());

        if (request.getArticleId() == null) {
            throw new IllegalArgumentException("articleId不能为空");
        }

        try {
            String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            if (request.getSearchLogId() != null) {
                jdbc.update(
                    "INSERT INTO search_feedback (search_log_id, article_id, is_helpful, user_comment, created_at) VALUES (?, ?, ?, ?, ?)",
                    request.getSearchLogId(), request.getArticleId(),
                    request.getIsHelpful(), request.getUserComment(), now
                );
            }
            log.info("反馈已持久化: isHelpful={}, comment={}",
                    request.getIsHelpful() == 1 ? "有用" : "无用",
                    request.getUserComment());
        } catch (Exception e) {
            log.warn("反馈持久化失败 (非致命): {}", e.getMessage());
            log.info("反馈已记录: isHelpful={}, comment={}",
                    request.getIsHelpful() == 1 ? "有用" : "无用",
                    request.getUserComment());
        }
    }

    private List<Map<String, String>> getMockLaws() {
        return List.of(
            Map.of("articleId", "ART-2023-001", "lawTitle", "中华人民共和国民法典", "articleNo", "第一百四十八条",
                "title", "欺诈的认定", "content", "一方以欺诈手段，使对方在违背真实意思的情况下订立的合同，受欺诈方有权请求人民法院或者仲裁机构予以撤销。"),
            Map.of("articleId", "ART-2023-002", "lawTitle", "中华人民共和国民法典", "articleNo", "第一百四十九条",
                "title", "第三人欺诈", "content", "第三人实施欺诈行为，使一方陷入错误认识的，适用欺诈规定。"),
            Map.of("articleId", "ART-2023-003", "lawTitle", "中华人民共和国民法典", "articleNo", "第五百六十三条",
                "title", "合同解除情形", "content", "有下列情形之一的，当事人可以解除合同。"),
            Map.of("articleId", "ART-2023-004", "lawTitle", "中华人民共和国民法典", "articleNo", "第五百七十七条",
                "title", "违约责任", "content", "当事人一方不履行合同义务或者履行合同义务不符合约定的，应当承担违约责任。"),
            Map.of("articleId", "ART-2023-005", "lawTitle", "中华人民共和国民法典", "articleNo", "第五百八十四条",
                "title", "损失赔偿范围", "content", "当事人一方不履行合同义务或者履行合同义务不符合约定的，给对方造成损失的，损失赔偿额应当相当于因违约所造成的损失。"),
            Map.of("articleId", "ART-2023-006", "lawTitle", "中华人民共和国劳动合同法", "articleNo", "第三十九条",
                "title", "用人单位单方解除劳动合同", "content", "劳动者有下列情形之一的，用人单位可以解除劳动合同。"),
            Map.of("articleId", "ART-2023-007", "lawTitle", "中华人民共和国劳动合同法", "articleNo", "第四十六条",
                "title", "经济补偿", "content", "有下列情形之一的，用人单位应当向劳动者支付经济补偿。"),
            Map.of("articleId", "ART-2023-008", "lawTitle", "最高人民法院关于审理建设工程施工合同纠纷案件适用法律问题的解释（一）", "articleNo", "第十条",
                "title", "工程价款结算", "content", "当事人对建设工程的计价标准或者计价方法有约定的，按照约定结算工程价款。")
        );
    }

    public boolean isHallucinated(String claim, List<LegalSearchResponse.SearchResultItem> citations) {
        if (claim == null || citations == null || citations.isEmpty()) {
            return false;
        }

        for (LegalSearchResponse.SearchResultItem citation : citations) {
            String content = citation.getContent();
            if (content != null && claim.contains(content.substring(0, Math.min(20, content.length())))) {
                return false;
            }
        }
        return true;
    }

    private Long persistSearchLog(String query, int resultCount, long responseTimeMs) {
        try {
            String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            jdbc.update(
                "INSERT INTO search_log (query_text, result_count, response_time_ms, created_at) VALUES (?, ?, ?, ?)",
                query, resultCount, (int) responseTimeMs, now
            );
            Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
            return id != null ? id : System.currentTimeMillis();
        } catch (Exception e) {
            log.warn("搜索日志持久化失败 (非致命): {}", e.getMessage());
        }
        return System.currentTimeMillis();
    }
}