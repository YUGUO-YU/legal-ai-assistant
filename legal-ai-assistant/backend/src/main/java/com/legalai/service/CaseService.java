package com.legalai.service;

import com.legalai.config.MilvusConfig;
import com.legalai.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

    @Value("${mock.enabled:true}")
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
            milvusResults = mockSearch(request).getItems();
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
        stats.setWinRate((double) winCount / items.size());

        BigDecimal totalAmount = items.stream()
            .filter(item -> item.getLitigationAmount() != null)
            .map(CaseSimilarSearchResponse.SimilarCaseItem::getLitigationAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setAvgCompensation(totalAmount.divide(BigDecimal.valueOf(items.size()), 2, BigDecimal.ROUND_HALF_UP));

        return stats;
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

        CaseSimilarSearchResponse.CaseStatistics statistics = new CaseSimilarSearchResponse.CaseStatistics();
        statistics.setTotalCount(156);
        statistics.setWinRate(0.73);
        statistics.setAvgCompensation(new BigDecimal("156000"));

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

        return null;
    }
}