package com.legalai.dto;

import java.util.List;

public class CaseSearchRequest {
    private List<Integer> caseType;
    private List<String> caseCause;
    private List<Integer> courtLevel;
    private List<Integer> trialProcedure;
    private List<Integer> judgmentResult;
    private Integer judgeYearMin;
    private Integer judgeYearMax;
    private String keyword;
    private Integer page = 1;
    private Integer pageSize = 10;

    public List<Integer> getCaseType() { return caseType; }
    public void setCaseType(List<Integer> caseType) { this.caseType = caseType; }
    public List<String> getCaseCause() { return caseCause; }
    public void setCaseCause(List<String> caseCause) { this.caseCause = caseCause; }
    public List<Integer> getCourtLevel() { return courtLevel; }
    public void setCourtLevel(List<Integer> courtLevel) { this.courtLevel = courtLevel; }
    public List<Integer> getTrialProcedure() { return trialProcedure; }
    public void setTrialProcedure(List<Integer> trialProcedure) { this.trialProcedure = trialProcedure; }
    public List<Integer> getJudgmentResult() { return judgmentResult; }
    public void setJudgmentResult(List<Integer> judgmentResult) { this.judgmentResult = judgmentResult; }
    public Integer getJudgeYearMin() { return judgeYearMin; }
    public void setJudgeYearMin(Integer judgeYearMin) { this.judgeYearMin = judgeYearMin; }
    public Integer getJudgeYearMax() { return judgeYearMax; }
    public void setJudgeYearMax(Integer judgeYearMax) { this.judgeYearMax = judgeYearMax; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
}