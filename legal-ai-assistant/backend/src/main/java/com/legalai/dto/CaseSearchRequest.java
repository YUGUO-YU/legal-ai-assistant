package com.legalai.dto;

import java.util.List;

public class CaseSearchRequest {
    private Integer caseType;
    private String caseCause;
    private Integer courtLevel;
    private Integer trialProcedure;
    private Integer judgmentResult;
    private Integer judgeYearMin;
    private Integer judgeYearMax;
    private String keyword;
    private String sort;
    private String order;
    private Integer page = 1;
    private Integer pageSize = 10;

    public Integer getCaseType() { return caseType; }
    public void setCaseType(Integer caseType) { this.caseType = caseType; }
    public String getCaseCause() { return caseCause; }
    public void setCaseCause(String caseCause) { this.caseCause = caseCause; }
    public Integer getCourtLevel() { return courtLevel; }
    public void setCourtLevel(Integer courtLevel) { this.courtLevel = courtLevel; }
    public Integer getTrialProcedure() { return trialProcedure; }
    public void setTrialProcedure(Integer trialProcedure) { this.trialProcedure = trialProcedure; }
    public Integer getJudgmentResult() { return judgmentResult; }
    public void setJudgmentResult(Integer judgmentResult) { this.judgmentResult = judgmentResult; }
    public Integer getJudgeYearMin() { return judgeYearMin; }
    public void setJudgeYearMin(Integer judgeYearMin) { this.judgeYearMin = judgeYearMin; }
    public Integer getJudgeYearMax() { return judgeYearMax; }
    public void setJudgeYearMax(Integer judgeYearMax) { this.judgeYearMax = judgeYearMax; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort; }
    public String getOrder() { return order; }
    public void setOrder(String order) { this.order = order; }
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }

    public boolean hasCaseTypeFilter() {
        return caseType != null && caseType > 0;
    }

    public boolean hasCourtLevelFilter() {
        return courtLevel != null && courtLevel > 0;
    }

    public String getCaseTypeName() {
        if (caseType == null) return null;
        switch (caseType) {
            case 1: return "民事";
            case 2: return "刑事";
            case 3: return "行政";
            case 4: return "执行";
            default: return "民事";
        }
    }
}