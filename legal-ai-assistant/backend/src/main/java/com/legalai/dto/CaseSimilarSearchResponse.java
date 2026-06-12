package com.legalai.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class CaseSimilarSearchResponse {
    private String sourceCaseHash;
    private Integer totalSimilar;
    private List<SimilarCaseItem> items;
    private CaseStatistics statistics;

    public String getSourceCaseHash() { return sourceCaseHash; }
    public void setSourceCaseHash(String sourceCaseHash) { this.sourceCaseHash = sourceCaseHash; }
    public Integer getTotalSimilar() { return totalSimilar; }
    public void setTotalSimilar(Integer totalSimilar) { this.totalSimilar = totalSimilar; }
    public List<SimilarCaseItem> getItems() { return items; }
    public void setItems(List<SimilarCaseItem> items) { this.items = items; }
    public CaseStatistics getStatistics() { return statistics; }
    public void setStatistics(CaseStatistics statistics) { this.statistics = statistics; }

    public static class SimilarCaseItem {
        private Long caseId;
        private String caseNo;
        private String caseName;
        private Integer courtLevel;
        private String courtName;
        private String judgeDate;
        private Integer judgmentResult;
        private BigDecimal litigationAmount;
        private Double similarityScore;
        private Map<String, Double> matchingFeatures;
        private String keyFacts;
        private String judgmentSummary;
        private List<String> legalBasis;
        private String sourceUrl;
        private String sourceName;

        public Long getCaseId() { return caseId; }
        public void setCaseId(Long caseId) { this.caseId = caseId; }
        public String getCaseNo() { return caseNo; }
        public void setCaseNo(String caseNo) { this.caseNo = caseNo; }
        public String getCaseName() { return caseName; }
        public void setCaseName(String caseName) { this.caseName = caseName; }
        public Integer getCourtLevel() { return courtLevel; }
        public void setCourtLevel(Integer courtLevel) { this.courtLevel = courtLevel; }
        public String getCourtName() { return courtName; }
        public void setCourtName(String courtName) { this.courtName = courtName; }
        public String getJudgeDate() { return judgeDate; }
        public void setJudgeDate(String judgeDate) { this.judgeDate = judgeDate; }
        public Integer getJudgmentResult() { return judgmentResult; }
        public void setJudgmentResult(Integer judgmentResult) { this.judgmentResult = judgmentResult; }
        public BigDecimal getLitigationAmount() { return litigationAmount; }
        public void setLitigationAmount(BigDecimal litigationAmount) { this.litigationAmount = litigationAmount; }
        public Double getSimilarityScore() { return similarityScore; }
        public void setSimilarityScore(Double similarityScore) { this.similarityScore = similarityScore; }
        public Map<String, Double> getMatchingFeatures() { return matchingFeatures; }
        public void setMatchingFeatures(Map<String, Double> matchingFeatures) { this.matchingFeatures = matchingFeatures; }
        public String getKeyFacts() { return keyFacts; }
        public void setKeyFacts(String keyFacts) { this.keyFacts = keyFacts; }
        public String getJudgmentSummary() { return judgmentSummary; }
        public void setJudgmentSummary(String judgmentSummary) { this.judgmentSummary = judgmentSummary; }
        public List<String> getLegalBasis() { return legalBasis; }
        public void setLegalBasis(List<String> legalBasis) { this.legalBasis = legalBasis; }
        public String getSourceUrl() { return sourceUrl; }
        public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
        public String getSourceName() { return sourceName; }
        public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    }

    public static class CaseStatistics {
        private Integer totalCount;
        private Double winRate;
        private BigDecimal avgCompensation;

        public Integer getTotalCount() { return totalCount; }
        public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }
        public Double getWinRate() { return winRate; }
        public void setWinRate(Double winRate) { this.winRate = winRate; }
        public BigDecimal getAvgCompensation() { return avgCompensation; }
        public void setAvgCompensation(BigDecimal avgCompensation) { this.avgCompensation = avgCompensation; }
    }
}