package com.legalai.dto;

import java.util.List;

public class LegalResearchRequest {
    private String question;
    private List<String> relevantLaws;
    private List<String> relevantCases;
    private Boolean includeAcademic;
    private Boolean includePractice;
    private Boolean includeRiskAnalysis;

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public List<String> getRelevantLaws() { return relevantLaws; }
    public void setRelevantLaws(List<String> relevantLaws) { this.relevantLaws = relevantLaws; }
    public List<String> getRelevantCases() { return relevantCases; }
    public void setRelevantCases(List<String> relevantCases) { this.relevantCases = relevantCases; }
    public Boolean getIncludeAcademic() { return includeAcademic; }
    public void setIncludeAcademic(Boolean includeAcademic) { this.includeAcademic = includeAcademic; }
    public Boolean getIncludePractice() { return includePractice; }
    public void setIncludePractice(Boolean includePractice) { this.includePractice = includePractice; }
    public Boolean getIncludeRiskAnalysis() { return includeRiskAnalysis; }
    public void setIncludeRiskAnalysis(Boolean includeRiskAnalysis) { this.includeRiskAnalysis = includeRiskAnalysis; }
}