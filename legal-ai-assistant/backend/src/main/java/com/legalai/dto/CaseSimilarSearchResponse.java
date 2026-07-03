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
        private WinRatePrediction winRatePrediction;
        private Map<String, Integer> judgmentDistribution;
        private CompensationDistribution compensationDistribution;
        private TimelineAnalysis timelineAnalysis;
        private List<String> strategyRecommendations;

        public Integer getTotalCount() { return totalCount; }
        public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }
        public Double getWinRate() { return winRate; }
        public void setWinRate(Double winRate) { this.winRate = winRate; }
        public BigDecimal getAvgCompensation() { return avgCompensation; }
        public void setAvgCompensation(BigDecimal avgCompensation) { this.avgCompensation = avgCompensation; }
        public WinRatePrediction getWinRatePrediction() { return winRatePrediction; }
        public void setWinRatePrediction(WinRatePrediction winRatePrediction) { this.winRatePrediction = winRatePrediction; }
        public Map<String, Integer> getJudgmentDistribution() { return judgmentDistribution; }
        public void setJudgmentDistribution(Map<String, Integer> judgmentDistribution) { this.judgmentDistribution = judgmentDistribution; }
        public CompensationDistribution getCompensationDistribution() { return compensationDistribution; }
        public void setCompensationDistribution(CompensationDistribution compensationDistribution) { this.compensationDistribution = compensationDistribution; }
        public TimelineAnalysis getTimelineAnalysis() { return timelineAnalysis; }
        public void setTimelineAnalysis(TimelineAnalysis timelineAnalysis) { this.timelineAnalysis = timelineAnalysis; }
        public List<String> getStrategyRecommendations() { return strategyRecommendations; }
        public void setStrategyRecommendations(List<String> strategyRecommendations) { this.strategyRecommendations = strategyRecommendations; }
    }

    public static class WinRatePrediction {
        private Double predictedWinRate;
        private Double confidence;
        private FactorAnalysis factorAnalysis;
        private Map<String, Double> resultProbabilities;

        public Double getPredictedWinRate() { return predictedWinRate; }
        public void setPredictedWinRate(Double predictedWinRate) { this.predictedWinRate = predictedWinRate; }
        public Double getConfidence() { return confidence; }
        public void setConfidence(Double confidence) { this.confidence = confidence; }
        public FactorAnalysis getFactorAnalysis() { return factorAnalysis; }
        public void setFactorAnalysis(FactorAnalysis factorAnalysis) { this.factorAnalysis = factorAnalysis; }
        public Map<String, Double> getResultProbabilities() { return resultProbabilities; }
        public void setResultProbabilities(Map<String, Double> resultProbabilities) { this.resultProbabilities = resultProbabilities; }
    }

    public static class FactorAnalysis {
        private List<String> favorableFactors;
        private List<String> unfavorableFactors;
        private List<String> neutralFactors;

        public List<String> getFavorableFactors() { return favorableFactors; }
        public void setFavorableFactors(List<String> favorableFactors) { this.favorableFactors = favorableFactors; }
        public List<String> getUnfavorableFactors() { return unfavorableFactors; }
        public void setUnfavorableFactors(List<String> unfavorableFactors) { this.unfavorableFactors = unfavorableFactors; }
        public List<String> getNeutralFactors() { return neutralFactors; }
        public void setNeutralFactors(List<String> neutralFactors) { this.neutralFactors = neutralFactors; }
    }

    public static class CompensationDistribution {
        private Integer range0to5w;
        private Integer range5to20w;
        private Integer range20to50w;
        private Integer rangeAbove50w;

        public Integer getRange0to5w() { return range0to5w; }
        public void setRange0to5w(Integer range0to5w) { this.range0to5w = range0to5w; }
        public Integer getRange5to20w() { return range5to20w; }
        public void setRange5to20w(Integer range5to20w) { this.range5to20w = range5to20w; }
        public Integer getRange20to50w() { return range20to50w; }
        public void setRange20to50w(Integer range20to50w) { this.range20to50w = range20to50w; }
        public Integer getRangeAbove50w() { return rangeAbove50w; }
        public void setRangeAbove50w(Integer rangeAbove50w) { this.rangeAbove50w = rangeAbove50w; }
    }

    public static class TimelineAnalysis {
        private List<YearDistribution> caseDistribution;
        private Integer avgDuration;
        private String trendDirection;
        private Map<String, Integer> courtLevelDistribution;

        public List<YearDistribution> getCaseDistribution() { return caseDistribution; }
        public void setCaseDistribution(List<YearDistribution> caseDistribution) { this.caseDistribution = caseDistribution; }
        public Integer getAvgDuration() { return avgDuration; }
        public void setAvgDuration(Integer avgDuration) { this.avgDuration = avgDuration; }
        public String getTrendDirection() { return trendDirection; }
        public void setTrendDirection(String trendDirection) { this.trendDirection = trendDirection; }
        public Map<String, Integer> getCourtLevelDistribution() { return courtLevelDistribution; }
        public void setCourtLevelDistribution(Map<String, Integer> courtLevelDistribution) { this.courtLevelDistribution = courtLevelDistribution; }
    }

    public static class YearDistribution {
        private Integer year;
        private Integer count;

        public YearDistribution() {}

        public YearDistribution(Integer year, Integer count) {
            this.year = year;
            this.count = count;
        }

        public Integer getYear() { return year; }
        public void setYear(Integer year) { this.year = year; }
        public Integer getCount() { return count; }
        public void setCount(Integer count) { this.count = count; }
    }
}
