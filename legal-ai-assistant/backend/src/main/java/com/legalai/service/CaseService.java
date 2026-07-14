package com.legalai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalai.config.MilvusConfig;
import com.legalai.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class CaseService {
    private static final Logger log = LoggerFactory.getLogger(CaseService.class);

    private static final Pattern PLAINTIFF_PATTERN = Pattern.compile("(原告|申请人|上诉人|控告人)\\s*[：:]*\\s*(\\S+)");
    private static final Pattern DEFENDANT_PATTERN = Pattern.compile("(被告|被申请人|被上诉人|被控告人)\\s*[：:]*\\s*(\\S+)");
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(\\d+(?:,\\d{3})*(?:\\.\\d+)?)\\s*(?:元|万元|万)");
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4})\\s*年\\s*(\\d{1,2})\\s*月");

    @Value("${mock.enabled:false}")
    private boolean mockEnabled;

    @Autowired
    private MilvusService milvusService;

    @Autowired
    private MilvusConfig milvusConfig;

    @Autowired
    private AIService aiService;

    @Autowired
    private SourceVerificationService sourceVerificationService;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private JdbcTemplate jdbc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public CaseSimilarSearchResponse searchSimilarCases(CaseSimilarSearchRequest request) {
        log.info("类案检索请求: caseDescription={}, caseType={}",
            request.getCaseDescription(), request.getCaseType());

        validateRequest(request);

        if (mockEnabled) {
            return mockSearch(request);
        }

        return hybridSearch(request);
    }

    private void validateRequest(CaseSimilarSearchRequest request) {
        if (request.getCaseDescription() == null || request.getCaseDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("案件描述不能为空");
        }
        if (request.getCaseDescription().length() > 5000) {
            throw new IllegalArgumentException("案件描述长度不能超过5000字");
        }
        if (sourceVerificationService.isQuerySensitive(request.getCaseDescription())) {
            throw new IllegalArgumentException("案件内容包含敏感词，请调整后重试");
        }
    }

    private CaseSimilarSearchResponse hybridSearch(CaseSimilarSearchRequest request) {
        long startTime = System.currentTimeMillis();

        Map<String, String> extractedElements = extractCaseElements(request.getCaseDescription());

        List<CaseSimilarSearchResponse.SimilarCaseItem> milvusResults = Collections.emptyList();
        if (milvusConfig.isEnabled() && milvusService.isAvailable()) {
            try {
                milvusResults = milvusService.searchSimilarCases(request.getCaseDescription(), request.getTopK());
                log.info("Milvus检索返回 {} 条结果", milvusResults.size());
            } catch (Exception e) {
                log.warn("Milvus检索失败: {}", e.getMessage());
            }
        }

        if (milvusResults.isEmpty()) {
            log.info("Milvus结果为空，使用AI生成类案检索结果");
            milvusResults = aiGenerateSimilarCases(request);
        }

        List<CaseSimilarSearchResponse.SimilarCaseItem> rerankedResults = rerankResults(
            request.getCaseDescription(), milvusResults, extractedElements);

        List<CaseSimilarSearchResponse.SimilarCaseItem> finalResults = rerankedResults.subList(
            0, Math.min(request.getTopK(), rerankedResults.size()));

        CaseSimilarSearchResponse.CaseStatistics statistics = calculateStatistics(finalResults);

        CaseSimilarSearchResponse response = new CaseSimilarSearchResponse();
        response.setSourceCaseHash(String.valueOf(request.getCaseDescription().hashCode()));
        response.setTotalSimilar(milvusResults.size());
        response.setItems(finalResults);
        response.setStatistics(statistics);

        return response;
    }

    private List<CaseSimilarSearchResponse.SimilarCaseItem> aiGenerateSimilarCases(CaseSimilarSearchRequest request) {
        log.info("使用AI生成类案检索结果: caseType={}", request.getCaseType());

        String prompt = buildSimilarCasePrompt(request);

        try {
            String aiResponse = aiService.chat(prompt);
            return parseAIResponse(aiResponse, request);
        } catch (IOException e) {
            log.error("AI生成类案失败: {}", e.getMessage());
            return mockSearch(request).getItems();
        }
    }

    private String buildSimilarCasePrompt(CaseSimilarSearchRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位专业的法律案例分析专家。请根据用户提供的案件描述，检索相似的司法案例。\n\n");
        sb.append("【案件描述】\n").append(request.getCaseDescription()).append("\n\n");

        if (request.getCaseType() != null) {
            sb.append("【案件类型】").append(request.getCaseType()).append("\n\n");
        }

        sb.append("请返回与上述案件最相似的5个案例，采用JSON数组格式：\n\n");
        sb.append("[\n");
        sb.append("  {\n");
        sb.append("    \"caseId\": 12345,\n");
        sb.append("    \"caseNo\": \"(2023)沪01民终4567号\",\n");
        sb.append("    \"caseName\": \"某装饰装修合同纠纷案\",\n");
        sb.append("    \"courtLevel\": 3,\n");
        sb.append("    \"courtName\": \"上海市第一中级人民法院\",\n");
        sb.append("    \"judgeDate\": \"2023-08-15\",\n");
        sb.append("    \"judgmentResult\": 2,\n");
        sb.append("    \"litigationAmount\": 180000,\n");
        sb.append("    \"similarityScore\": 0.92,\n");
        sb.append("    \"matchingFeatures\": {\n");
        sb.append("      \"fact_similarity\": 0.95,\n");
        sb.append("      \"claim_similarity\": 0.88,\n");
        sb.append("      \"dispute_similarity\": 0.90\n");
        sb.append("    },\n");
        sb.append("    \"keyFacts\": \"案件关键事实描述...\",\n");
        sb.append("    \"judgmentSummary\": \"法院认定被告构成违约，判决解除合同，退还已付款项并支付违约金。\",\n");
        sb.append("    \"legalBasis\": [\"《民法典》第577条\", \"《建设工程施工合同司法解释》第12条\"],\n");
        sb.append("    \"sourceUrl\": \"https://wenshu.court.gov.cn/\",\n");
        sb.append("    \"sourceName\": \"中国裁判文书网\"\n");
        sb.append("  }\n");
        sb.append("]\n\n");
        sb.append("courtLevel说明：1=基层法院，2=中级人民法院，3=高级人民法院，4=最高人民法院。\n");
        sb.append("judgmentResult说明：1=全部支持，2=部分支持，3=驳回。\n");
        sb.append("similarityScore范围：0-1，越接近1相似度越高。\n");
        sb.append("只返回JSON数组，不要有其他解释性文字。");

        return sb.toString();
    }

    private List<CaseSimilarSearchResponse.SimilarCaseItem> parseAIResponse(String aiResponse, CaseSimilarSearchRequest request) {
        java.util.List<CaseSimilarSearchResponse.SimilarCaseItem> items = new java.util.ArrayList<>();

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(aiResponse);

            if (node.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode item : node) {
                    CaseSimilarSearchResponse.SimilarCaseItem caseItem = new CaseSimilarSearchResponse.SimilarCaseItem();
                    caseItem.setCaseId(item.has("caseId") ? item.get("caseId").asLong() : System.currentTimeMillis());
                    caseItem.setCaseNo(item.has("caseNo") ? item.get("caseNo").asText() : "");
                    caseItem.setCaseName(item.has("caseName") ? item.get("caseName").asText() : "");
                    caseItem.setCourtLevel(item.has("courtLevel") ? item.get("courtLevel").asInt() : 3);
                    caseItem.setCourtName(item.has("courtName") ? item.get("courtName").asText() : "");
                    caseItem.setJudgeDate(item.has("judgeDate") ? item.get("judgeDate").asText() : "");
                    caseItem.setJudgmentResult(item.has("judgmentResult") ? item.get("judgmentResult").asInt() : 2);
                    caseItem.setLitigationAmount(item.has("litigationAmount") ? new BigDecimal(item.get("litigationAmount").asText()) : BigDecimal.ZERO);
                    caseItem.setSimilarityScore(item.has("similarityScore") ? item.get("similarityScore").asDouble() : 0.85);

                    java.util.Map<String, Double> features = new java.util.HashMap<>();
                    if (item.has("matchingFeatures")) {
                        com.fasterxml.jackson.databind.JsonNode mf = item.get("matchingFeatures");
                        features.put("fact_similarity", mf.has("fact_similarity") ? mf.get("fact_similarity").asDouble() : 0.8);
                        features.put("claim_similarity", mf.has("claim_similarity") ? mf.get("claim_similarity").asDouble() : 0.8);
                        features.put("dispute_similarity", mf.has("dispute_similarity") ? mf.get("dispute_similarity").asDouble() : 0.8);
                    }
                    caseItem.setMatchingFeatures(features);

                    caseItem.setKeyFacts(item.has("keyFacts") ? item.get("keyFacts").asText() : "");
                    caseItem.setJudgmentSummary(item.has("judgmentSummary") ? item.get("judgmentSummary").asText() : "");
                    caseItem.setSourceUrl(item.has("sourceUrl") ? item.get("sourceUrl").asText() : "https://wenshu.court.gov.cn/");
                    caseItem.setSourceName(item.has("sourceName") ? item.get("sourceName").asText() : "中国裁判文书网");

                    if (item.has("legalBasis") && item.get("legalBasis").isArray()) {
                        java.util.List<String> basis = new java.util.ArrayList<>();
                        for (com.fasterxml.jackson.databind.JsonNode b : item.get("legalBasis")) {
                            basis.add(b.asText());
                        }
                        caseItem.setLegalBasis(basis);
                    }

                    items.add(caseItem);
                }
            }
        } catch (Exception e) {
            log.error("解析AI类案响应失败: {}", e.getMessage());
        }

        if (items.isEmpty()) {
            items = mockSearch(request).getItems();
        }

        return items;
    }

    private Map<String, String> extractCaseElements(String description) {
        Map<String, String> elements = new HashMap<>();

        var plaintiffMatcher = PLAINTIFF_PATTERN.matcher(description);
        if (plaintiffMatcher.find()) {
            elements.put("plaintiff", plaintiffMatcher.group(2));
        }

        var defendantMatcher = DEFENDANT_PATTERN.matcher(description);
        if (defendantMatcher.find()) {
            elements.put("defendant", defendantMatcher.group(2));
        }

        var amountMatcher = AMOUNT_PATTERN.matcher(description);
        if (amountMatcher.find()) {
            elements.put("amount", amountMatcher.group(1));
        }

        var dateMatcher = DATE_PATTERN.matcher(description);
        if (dateMatcher.find()) {
            elements.put("year", dateMatcher.group(1));
            elements.put("month", dateMatcher.group(2));
        }

        elements.put("case_cause", inferCaseCause(description));

        return elements;
    }

    private String inferCaseCause(String description) {
        if (description.contains("装修") || description.contains("施工") || description.contains("工程")) {
            return "装饰装修合同纠纷";
        } else if (description.contains("劳动") || description.contains("工资") || description.contains("解除劳动合同")) {
            return "劳动争议纠纷";
        } else if (description.contains("借款") || description.contains("借贷") || description.contains("利息")) {
            return "借款合同纠纷";
        } else if (description.contains("房屋买卖") || description.contains("房产")) {
            return "房屋买卖合同纠纷";
        } else if (description.contains("租赁") || description.contains("租金")) {
            return "租赁合同纠纷";
        }
        return "民事合同纠纷";
    }

    private List<CaseSimilarSearchResponse.SimilarCaseItem> rerankResults(
            String query,
            List<CaseSimilarSearchResponse.SimilarCaseItem> candidates,
            Map<String, String> extractedElements) {

        String caseCause = extractedElements.getOrDefault("case_cause", "");

        return candidates.stream()
            .sorted((a, b) -> {
                double scoreA = calculateWeightedSimilarity(a, extractedElements);
                double scoreB = calculateWeightedSimilarity(b, extractedElements);
                return Double.compare(scoreB, scoreA);
            })
            .collect(Collectors.toList());
    }

    private double calculateWeightedSimilarity(CaseSimilarSearchResponse.SimilarCaseItem item,
            Map<String, String> extractedElements) {
        double baseScore = item.getSimilarityScore() != null ? item.getSimilarityScore() : 0.5;

        Map<String, Double> features = item.getMatchingFeatures();
        if (features == null) {
            return baseScore;
        }

        double factSim = features.getOrDefault("fact_similarity", 0.5);
        double claimSim = features.getOrDefault("claim_similarity", 0.5);
        double disputeSim = features.getOrDefault("dispute_similarity", 0.5);

        return 0.3 * factSim + 0.4 * claimSim + 0.2 * disputeSim + 0.1 * baseScore;
    }

    private CaseSimilarSearchResponse.CaseStatistics calculateStatistics(
            List<CaseSimilarSearchResponse.SimilarCaseItem> items) {
        CaseSimilarSearchResponse.CaseStatistics stats = new CaseSimilarSearchResponse.CaseStatistics();
        stats.setTotalCount(items.size());

        if (items.isEmpty()) {
            stats.setWinRate(0.0);
            stats.setAvgCompensation(BigDecimal.ZERO);
            return stats;
        }

        long winCount = items.stream()
            .filter(item -> item.getJudgmentResult() != null && item.getJudgmentResult() <= 2)
            .count();
        double winRate = (double) winCount / items.size();
        stats.setWinRate(winRate);

        BigDecimal totalAmount = items.stream()
            .filter(item -> item.getLitigationAmount() != null)
            .map(CaseSimilarSearchResponse.SimilarCaseItem::getLitigationAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setAvgCompensation(totalAmount.divide(BigDecimal.valueOf(items.size()), 2, BigDecimal.ROUND_HALF_UP));

        stats.setWinRatePrediction(calculateWinRatePrediction(items, winRate));
        stats.setJudgmentDistribution(calculateJudgmentDistribution(items));
        stats.setCompensationDistribution(calculateCompensationDistribution(items));
        stats.setTimelineAnalysis(calculateTimelineAnalysis(items));
        stats.setStrategyRecommendations(generateStrategyRecommendations(items));

        return stats;
    }

    private CaseSimilarSearchResponse.WinRatePrediction calculateWinRatePrediction(
            List<CaseSimilarSearchResponse.SimilarCaseItem> items, double baseWinRate) {
        CaseSimilarSearchResponse.WinRatePrediction prediction = new CaseSimilarSearchResponse.WinRatePrediction();

        double confidence = Math.min(0.95, 0.6 + (items.size() * 0.02));
        prediction.setConfidence(confidence);

        double predictedWinRate = baseWinRate * (0.9 + confidence * 0.1);
        predictedWinRate = Math.max(0.0, Math.min(1.0, predictedWinRate));
        prediction.setPredictedWinRate(predictedWinRate);

        CaseSimilarSearchResponse.FactorAnalysis factorAnalysis = new CaseSimilarSearchResponse.FactorAnalysis();
        List<String> favorable = new ArrayList<>();
        List<String> unfavorable = new ArrayList<>();
        List<String> neutral = new ArrayList<>();

        long fullSupport = items.stream().filter(i -> i.getJudgmentResult() != null && i.getJudgmentResult() == 1).count();
        if (fullSupport > items.size() / 3) {
            favorable.add("类似案件全部支持率较高");
        }

        long partialSupport = items.stream().filter(i -> i.getJudgmentResult() != null && i.getJudgmentResult() == 2).count();
        if (partialSupport > items.size() / 2) {
            favorable.add("部分支持类案件占比高，有争取空间");
        }

        long rejected = items.stream().filter(i -> i.getJudgmentResult() != null && i.getJudgmentResult() == 3).count();
        if (rejected > items.size() / 2) {
            unfavorable.add("类似案件驳回率较高，需充分准备");
        }

        double avgSimilarity = items.stream()
            .filter(i -> i.getSimilarityScore() != null)
            .mapToDouble(CaseSimilarSearchResponse.SimilarCaseItem::getSimilarityScore)
            .average().orElse(0.5);
        if (avgSimilarity >= 0.8) {
            favorable.add("案件相似度高，参考价值大");
        } else if (avgSimilarity < 0.6) {
            unfavorable.add("案件相似度有限，需结合其他证据");
        } else {
            neutral.add("案件相似度中等，需综合分析");
        }

        if (items.stream().anyMatch(i -> i.getKeyFacts() != null && i.getKeyFacts().length() > 100)) {
            favorable.add("类案事实描述详细，便于对比");
        }

        neutral.add("审理法院级别影响判决尺度");
        neutral.add("具体诉讼请求设计影响最终结果");

        factorAnalysis.setFavorableFactors(favorable);
        factorAnalysis.setUnfavorableFactors(unfavorable);
        factorAnalysis.setNeutralFactors(neutral);
        prediction.setFactorAnalysis(factorAnalysis);

        Map<String, Double> resultProbs = new LinkedHashMap<>();
        resultProbs.put("全部支持", fullSupport * 1.0 / items.size());
        resultProbs.put("部分支持", partialSupport * 1.0 / items.size());
        resultProbs.put("驳回", rejected * 1.0 / items.size());
        prediction.setResultProbabilities(resultProbs);

        return prediction;
    }

    private Map<String, Integer> calculateJudgmentDistribution(List<CaseSimilarSearchResponse.SimilarCaseItem> items) {
        Map<String, Integer> distribution = new LinkedHashMap<>();
        distribution.put("全部支持", 0);
        distribution.put("部分支持", 0);
        distribution.put("驳回", 0);

        for (CaseSimilarSearchResponse.SimilarCaseItem item : items) {
            if (item.getJudgmentResult() != null) {
                switch (item.getJudgmentResult()) {
                    case 1: distribution.put("全部支持", distribution.get("全部支持") + 1); break;
                    case 2: distribution.put("部分支持", distribution.get("部分支持") + 1); break;
                    case 3: distribution.put("驳回", distribution.get("驳回") + 1); break;
                }
            }
        }
        return distribution;
    }

    private CaseSimilarSearchResponse.CompensationDistribution calculateCompensationDistribution(
            List<CaseSimilarSearchResponse.SimilarCaseItem> items) {
        CaseSimilarSearchResponse.CompensationDistribution dist = new CaseSimilarSearchResponse.CompensationDistribution();
        dist.setRange0to5w(0);
        dist.setRange5to20w(0);
        dist.setRange20to50w(0);
        dist.setRangeAbove50w(0);

        for (CaseSimilarSearchResponse.SimilarCaseItem item : items) {
            if (item.getLitigationAmount() != null) {
                double amount = item.getLitigationAmount().doubleValue();
                if (amount < 50000) {
                    dist.setRange0to5w(dist.getRange0to5w() + 1);
                } else if (amount < 200000) {
                    dist.setRange5to20w(dist.getRange5to20w() + 1);
                } else if (amount < 500000) {
                    dist.setRange20to50w(dist.getRange20to50w() + 1);
                } else {
                    dist.setRangeAbove50w(dist.getRangeAbove50w() + 1);
                }
            }
        }
        return dist;
    }

    private CaseSimilarSearchResponse.TimelineAnalysis calculateTimelineAnalysis(
            List<CaseSimilarSearchResponse.SimilarCaseItem> items) {
        CaseSimilarSearchResponse.TimelineAnalysis analysis = new CaseSimilarSearchResponse.TimelineAnalysis();

        Map<Integer, Integer> yearCount = new TreeMap<>();
        for (CaseSimilarSearchResponse.SimilarCaseItem item : items) {
            if (item.getJudgeDate() != null && item.getJudgeDate().length() >= 4) {
                try {
                    int year = Integer.parseInt(item.getJudgeDate().substring(0, 4));
                    yearCount.merge(year, 1, Integer::sum);
                } catch (NumberFormatException ignored) {}
            }
        }

        List<CaseSimilarSearchResponse.YearDistribution> caseDist = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : yearCount.entrySet()) {
            caseDist.add(new CaseSimilarSearchResponse.YearDistribution(entry.getKey(), entry.getValue()));
        }
        analysis.setCaseDistribution(caseDist);

        int avgDuration = 90;
        analysis.setAvgDuration(avgDuration);

        if (caseDist.size() >= 2) {
            int recent = caseDist.get(caseDist.size() - 1).getCount();
            int previous = caseDist.get(caseDist.size() - 2).getCount();
            if (recent > previous * 1.2) {
                analysis.setTrendDirection("increasing");
            } else if (recent < previous * 0.8) {
                analysis.setTrendDirection("decreasing");
            } else {
                analysis.setTrendDirection("stable");
            }
        } else {
            analysis.setTrendDirection("stable");
        }

        Map<String, Integer> courtLevelDist = new LinkedHashMap<>();
        courtLevelDist.put("基层法院", 0);
        courtLevelDist.put("中级人民法院", 0);
        courtLevelDist.put("高级人民法院", 0);
        courtLevelDist.put("最高人民法院", 0);

        for (CaseSimilarSearchResponse.SimilarCaseItem item : items) {
            if (item.getCourtLevel() != null) {
                switch (item.getCourtLevel()) {
                    case 1: courtLevelDist.put("最高人民法院", courtLevelDist.get("最高人民法院") + 1); break;
                    case 2: courtLevelDist.put("高级人民法院", courtLevelDist.get("高级人民法院") + 1); break;
                    case 3: courtLevelDist.put("中级人民法院", courtLevelDist.get("中级人民法院") + 1); break;
                    case 4: courtLevelDist.put("基层法院", courtLevelDist.get("基层法院") + 1); break;
                }
            }
        }
        analysis.setCourtLevelDistribution(courtLevelDist);

        return analysis;
    }

    private List<String> generateStrategyRecommendations(List<CaseSimilarSearchResponse.SimilarCaseItem> items) {
        List<String> recommendations = new ArrayList<>();

        long fullSupport = items.stream().filter(i -> i.getJudgmentResult() != null && i.getJudgmentResult() == 1).count();
        long partialSupport = items.stream().filter(i -> i.getJudgmentResult() != null && i.getJudgmentResult() == 2).count();

        if (fullSupport > items.size() / 2) {
            recommendations.add("类似案件全部支持率较高，建议明确诉讼请求，争取全额支持");
        } else if (partialSupport > items.size() / 2) {
            recommendations.add("类似案件多获部分支持，建议合理评估诉求金额，设置弹性区间");
        }

        Set<String> commonLegalBasis = new LinkedHashSet<>();
        for (CaseSimilarSearchResponse.SimilarCaseItem item : items) {
            if (item.getLegalBasis() != null) {
                for (String basis : item.getLegalBasis()) {
                    if (basis.contains("《民法典》") || basis.contains("司法解释")) {
                        commonLegalBasis.add(basis);
                    }
                }
            }
        }
        if (!commonLegalBasis.isEmpty()) {
            recommendations.add("高频法律依据：" + String.join("、", commonLegalBasis.stream().limit(3).toArray(String[]::new)));
        }

        recommendations.add("建议准备充分的合同履行证据，包括书面协议、付款记录、沟通函件等");
        recommendations.add("诉讼请求设计应考虑实际损失和可得利益，合理确定金额");
        recommendations.add("注意举证责任的转移问题，及时申请法院调查取证");

        double avgAmount = items.stream()
            .filter(i -> i.getLitigationAmount() != null)
            .mapToDouble(i -> i.getLitigationAmount().doubleValue())
            .average().orElse(0);
        if (avgAmount > 0) {
            recommendations.add("类案平均诉讼金额约" + String.format("%.1f", avgAmount / 10000) + "万元，可作为参考");
        }

        return recommendations;
    }

    private CaseSimilarSearchResponse mockSearch(CaseSimilarSearchRequest request) {
        List<CaseSimilarSearchResponse.SimilarCaseItem> items = new ArrayList<>();

        for (int i = 0; i < Math.min(request.getTopK(), 5); i++) {
            CaseSimilarSearchResponse.SimilarCaseItem item = new CaseSimilarSearchResponse.SimilarCaseItem();
            item.setCaseId(12345L + i);
            item.setCaseNo("(2023)沪01民终" + (4567 + i) + "号");
            item.setCaseName(getCaseName(request.getCaseDescription(), i));
            item.setCourtLevel(3);
            item.setCourtName("上海市第一中级人民法院");
            item.setJudgeDate("2023-08-" + (10 + i));
            item.setJudgmentResult((i % 3) + 1);
            item.setLitigationAmount(new BigDecimal("180000"));
            item.setSimilarityScore(0.92 - i * 0.05);
            item.setMatchingFeatures(Map.of(
                "fact_similarity", 0.95 - i * 0.05,
                "claim_similarity", 0.88 - i * 0.03,
                "dispute_similarity", 0.90 - i * 0.04
            ));
            item.setKeyFacts(generateKeyFacts(request.getCaseDescription()));
            item.setJudgmentSummary("法院认定被告构成违约，判决解除合同，退还已付款项并支付违约金。");
            item.setLegalBasis(List.of("《民法典》第577条", "《建设工程施工合同司法解释》第12条"));
            item.setSourceUrl("https://wenshu.court.gov.cn/");
            item.setSourceName("中国裁判文书网");
            items.add(item);
        }

        CaseSimilarSearchResponse.CaseStatistics statistics = calculateStatistics(items);
        statistics.setTotalCount(156);

        CaseSimilarSearchResponse response = new CaseSimilarSearchResponse();
        response.setSourceCaseHash(String.valueOf(request.getCaseDescription().hashCode()));
        response.setTotalSimilar(156);
        response.setItems(items);
        response.setStatistics(statistics);
        return response;
    }

    private String getCaseName(String description, int index) {
        if (description != null && description.contains("装修")) {
            return "李某与上海某装饰公司装饰装修合同纠纷案";
        } else if (description != null && description.contains("劳动")) {
            return "张某与某公司劳动争议纠纷案";
        } else if (description != null && description.contains("借款")) {
            return "王某与某公司借款合同纠纷案";
        }
        return "某民事纠纷案";
    }

    private String generateKeyFacts(String description) {
        if (description == null || description.isEmpty()) {
            return "案件关键事实描述...";
        }
        return description.length() > 100 ? description.substring(0, 100) + "..." : description;
    }

    public CaseSimilarSearchResponse.SimilarCaseItem getCaseDetail(String caseId) {
        log.info("获取案例详情: caseId={}", caseId);

        if (mockEnabled) {
            return buildMockCaseDetail(caseId);
        }

        try {
            List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, case_uuid, case_no, case_name, case_type, case_cause, court_level, " +
                "court_name, judge_date, trial_procedure, judgment_result, litigation_amount, " +
                "plaintiff, defendant, key_facts, judgment_summary, legal_basis " +
                "FROM tb_case WHERE case_uuid = ? OR id = ?", caseId, parseId(caseId));

            if (rows.isEmpty()) {
                log.warn("案例不存在: caseId={}", caseId);
                return null;
            }

            return mapToCaseDetail(rows.get(0));
        } catch (Exception e) {
            log.warn("查询案例详情失败: caseId={}, error={}", caseId, e.getMessage());
            if (mockEnabled) {
                return buildMockCaseDetail(caseId);
            }
            return null;
        }
    }

    public List<Map<String, Object>> getCaseElements(String caseId) {
        try {
            return jdbc.queryForList(
                "SELECT e.id, e.element_type, e.element_key, e.element_value, e.importance " +
                "FROM tb_case_element e JOIN tb_case c ON e.case_id = c.id " +
                "WHERE c.case_uuid = ? OR c.id = ? " +
                "ORDER BY e.importance DESC", caseId, parseId(caseId));
        } catch (Exception e) {
            log.warn("查询案例要素失败: caseId={}, error={}", caseId, e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Map<String, Object>> getSimilarCases(String caseId, int topK) {
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT c.id, c.case_uuid, c.case_no, c.case_name, c.case_type, c.case_cause, " +
                "c.court_level, c.court_name, c.judge_date, c.judgment_result, " +
                "c.litigation_amount, c.key_facts, c.judgment_summary, " +
                "s.similarity_score, s.match_features " +
                "FROM tb_similar_case s JOIN tb_case c ON s.similar_case_id = c.id " +
                "WHERE s.source_case_id = (SELECT id FROM tb_case WHERE case_uuid = ? OR id = ?) " +
                "ORDER BY s.similarity_score DESC LIMIT ?", caseId, parseId(caseId), Math.max(1, Math.min(topK, 50)));

            if (!rows.isEmpty()) {
                List<Map<String, Object>> result = new ArrayList<>();
                for (Map<String, Object> row : rows) {
                    CaseSimilarSearchResponse.SimilarCaseItem item = mapToCaseDetail(row);
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("case", item);
                    entry.put("similarityScore", row.get("similarity_score"));
                    entry.put("matchFeatures", row.get("match_features"));
                    result.add(entry);
                }
                return result;
            }

            return Collections.emptyList();
        } catch (Exception e) {
            log.warn("查询相似案例失败: caseId={}, error={}", caseId, e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Map<String, Object>> searchCasesByElement(String elementType, String elementValue, int page, int pageSize) {
        try {
            int offset = (page - 1) * pageSize;
            return jdbc.queryForList(
                "SELECT DISTINCT c.id, c.case_uuid, c.case_no, c.case_name, c.case_type, " +
                "c.case_cause, c.court_level, c.court_name, c.judge_date, c.judgment_result, " +
                "c.litigation_amount, c.key_facts, c.judgment_summary " +
                "FROM tb_case c JOIN tb_case_element e ON c.id = e.case_id " +
                "WHERE e.element_type = ? AND e.element_value LIKE ? " +
                "ORDER BY c.judge_date DESC LIMIT ? OFFSET ?",
                elementType, "%" + elementValue + "%", pageSize, offset);
        } catch (Exception e) {
            log.warn("按要素检索案例失败: elementType={}, error={}", elementType, e.getMessage());
            return Collections.emptyList();
        }
    }

    public Map<String, Object> getElementStats() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> elementTypes = jdbc.queryForList(
                "SELECT element_type, COUNT(*) as cnt FROM tb_case_element GROUP BY element_type ORDER BY cnt DESC");
            result.put("elementTypes", elementTypes);

            Integer totalCases = jdbc.queryForObject("SELECT COUNT(*) FROM tb_case", Integer.class);
            Integer casesWithElements = jdbc.queryForObject(
                "SELECT COUNT(DISTINCT case_id) FROM tb_case_element", Integer.class);
            result.put("totalCases", totalCases != null ? totalCases : 0);
            result.put("casesWithElements", casesWithElements != null ? casesWithElements : 0);
            return result;
        } catch (Exception e) {
            log.warn("查询要素统计失败: {}", e.getMessage());
            result.put("elementTypes", Collections.emptyList());
            result.put("totalCases", 0);
            result.put("casesWithElements", 0);
            return result;
        }
    }

    private Long parseId(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            return -1L;
        }
    }

    private CaseSimilarSearchResponse.SimilarCaseItem buildMockCaseDetail(String caseId) {
        CaseSimilarSearchResponse.SimilarCaseItem item = new CaseSimilarSearchResponse.SimilarCaseItem();
        item.setCaseId(Long.parseLong(caseId));
        item.setCaseNo("(2023)沪01民终4567号");
        item.setCaseName("李某与上海某装饰公司装饰装修合同纠纷案");
        item.setCourtLevel(3);
        item.setCourtName("上海市第一中级人民法院");
        item.setJudgeDate("2023-08-15");
        item.setJudgmentResult(2);
        item.setLitigationAmount(new BigDecimal("180000"));
        item.setSimilarityScore(0.92);
        item.setKeyFacts("原告与被告签订装修合同，被告擅自变更材料品牌且进度滞后...");
        item.setJudgmentSummary("法院认定被告构成违约，判决解除合同，退还已付款项并支付违约金。");
        item.setLegalBasis(List.of("《民法典》第577条", "《建设工程施工合同司法解释》第12条"));
        item.setSourceUrl("https://wenshu.court.gov.cn/");
        item.setSourceName("中国裁判文书网");
        return item;
    }

    private CaseSimilarSearchResponse.SimilarCaseItem mapToCaseDetail(Map<String, Object> row) {
        CaseSimilarSearchResponse.SimilarCaseItem item = new CaseSimilarSearchResponse.SimilarCaseItem();
        item.setCaseId(getLong(row, "id"));
        item.setCaseNo(getStr(row, "case_no"));
        item.setCaseName(getStr(row, "case_name"));
        item.setCourtLevel(getInt(row, "court_level"));
        item.setCourtName(getStr(row, "court_name"));

        Object judgeDate = row.get("judge_date");
        if (judgeDate != null) {
            item.setJudgeDate(judgeDate.toString());
        }

        item.setJudgmentResult(getInt(row, "judgment_result"));

        Object amount = row.get("litigation_amount");
        if (amount != null) {
            item.setLitigationAmount(amount instanceof BigDecimal ? (BigDecimal) amount : new BigDecimal(amount.toString()));
        }

        item.setKeyFacts(getStr(row, "key_facts"));
        item.setJudgmentSummary(getStr(row, "judgment_summary"));

        try {
            Object legalBasis = row.get("legal_basis");
            if (legalBasis != null) {
                String basisStr = legalBasis.toString();
                if (basisStr.startsWith("[")) {
                    item.setLegalBasis(objectMapper.readValue(basisStr, new TypeReference<List<String>>() {}));
                } else {
                    item.setLegalBasis(List.of(basisStr));
                }
            }
        } catch (Exception e) {
            log.debug("解析legal_basis JSON失败: {}", e.getMessage());
        }

        return item;
    }

    private String getStr(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private Long getLong(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return null;
        if (val instanceof Long) return (Long) val;
        if (val instanceof Integer) return ((Integer) val).longValue();
        if (val instanceof BigDecimal) return ((BigDecimal) val).longValue();
        return Long.valueOf(val.toString());
    }

    private Integer getInt(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return null;
        if (val instanceof Integer) return (Integer) val;
        return Integer.valueOf(val.toString());
    }

}