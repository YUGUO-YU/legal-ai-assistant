package com.legalai.dto;

import java.util.List;

public class CaseSearchResponse {
    private Long total;
    private Integer page;
    private Integer pageSize;
    private Long tookMs;
    private List<CaseSearchItem> items;

    public Long getTotal() { return total; }
    public void setTotal(Long total) { this.total = total; }
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
    public Long getTookMs() { return tookMs; }
    public void setTookMs(Long tookMs) { this.tookMs = tookMs; }
    public List<CaseSearchItem> getItems() { return items; }
    public void setItems(List<CaseSearchItem> items) { this.items = items; }

    public static class CaseSearchItem {
        private String caseUuid;
        private String caseNo;
        private String title;
        private String court;
        private Integer courtLevel;
        private String caseType;
        private String caseCause;
        private String judgeDate;
        private String trialProcedure;
        private Integer judgmentResult;
        private Long litigationAmount;
        private String summary;
        private String keyFacts;
        private String judgmentSummary;
        private String legalBasis;
        private String sourceUrl;
        private String sourceName;

        public String getCaseUuid() { return caseUuid; }
        public void setCaseUuid(String caseUuid) { this.caseUuid = caseUuid; }
        public String getCaseNo() { return caseNo; }
        public void setCaseNo(String caseNo) { this.caseNo = caseNo; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getCourt() { return court; }
        public void setCourt(String court) { this.court = court; }
        public Integer getCourtLevel() { return courtLevel; }
        public void setCourtLevel(Integer courtLevel) { this.courtLevel = courtLevel; }
        public String getCaseType() { return caseType; }
        public void setCaseType(String caseType) { this.caseType = caseType; }
        public String getCaseCause() { return caseCause; }
        public void setCaseCause(String caseCause) { this.caseCause = caseCause; }
        public String getJudgeDate() { return judgeDate; }
        public void setJudgeDate(String judgeDate) { this.judgeDate = judgeDate; }
        public String getTrialProcedure() { return trialProcedure; }
        public void setTrialProcedure(String trialProcedure) { this.trialProcedure = trialProcedure; }
        public Integer getJudgmentResult() { return judgmentResult; }
        public void setJudgmentResult(Integer judgmentResult) { this.judgmentResult = judgmentResult; }
        public Long getLitigationAmount() { return litigationAmount; }
        public void setLitigationAmount(Long litigationAmount) { this.litigationAmount = litigationAmount; }
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        public String getKeyFacts() { return keyFacts; }
        public void setKeyFacts(String keyFacts) { this.keyFacts = keyFacts; }
        public String getJudgmentSummary() { return judgmentSummary; }
        public void setJudgmentSummary(String judgmentSummary) { this.judgmentSummary = judgmentSummary; }
        public String getLegalBasis() { return legalBasis; }
        public void setLegalBasis(String legalBasis) { this.legalBasis = legalBasis; }
        public String getSourceUrl() { return sourceUrl; }
        public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
        public String getSourceName() { return sourceName; }
        public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    }
}