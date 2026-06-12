package com.legalai.dto;

import java.math.BigDecimal;
import java.util.Map;

public class CaseSimilarSearchRequest {
    private String caseDescription;
    private Integer caseType;
    private String caseCause;
    private Integer topK = 10;
    private CaseFilters filters;

    public String getCaseDescription() { return caseDescription; }
    public void setCaseDescription(String caseDescription) { this.caseDescription = caseDescription; }
    public Integer getCaseType() { return caseType; }
    public void setCaseType(Integer caseType) { this.caseType = caseType; }
    public String getCaseCause() { return caseCause; }
    public void setCaseCause(String caseCause) { this.caseCause = caseCause; }
    public Integer getTopK() { return topK; }
    public void setTopK(Integer topK) { this.topK = topK; }
    public CaseFilters getFilters() { return filters; }
    public void setFilters(CaseFilters filters) { this.filters = filters; }

    public static class CaseFilters {
        private Integer[] courtLevel;
        private Integer[] judgmentResult;
        private Integer judgeYearMin;

        public Integer[] getCourtLevel() { return courtLevel; }
        public void setCourtLevel(Integer[] courtLevel) { this.courtLevel = courtLevel; }
        public Integer[] getJudgmentResult() { return judgmentResult; }
        public void setJudgmentResult(Integer[] judgmentResult) { this.judgmentResult = judgmentResult; }
        public Integer getJudgeYearMin() { return judgeYearMin; }
        public void setJudgeYearMin(Integer judgeYearMin) { this.judgeYearMin = judgeYearMin; }
    }
}