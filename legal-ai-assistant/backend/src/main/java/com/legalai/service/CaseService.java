package com.legalai.service;

import com.legalai.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class CaseService {
    private static final Logger log = LoggerFactory.getLogger(CaseService.class);

    public CaseSimilarSearchResponse searchSimilarCases(CaseSimilarSearchRequest request) {
        log.info("类案检索请求: caseDescription={}, caseType={}",
            request.getCaseDescription(), request.getCaseType());

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
        response.setSourceCaseHash("案件描述向量指纹");
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
}