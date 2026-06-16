package com.legalai.dto;

import java.util.List;

public class LegalResearchResponse {
    private String reportId;
    private String reportContent;
    private List<String> referencedLaws;
    private List<String> referencedCases;
    private Long generatedAt;
    private Long tookMs;

    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }
    public String getReportContent() { return reportContent; }
    public void setReportContent(String reportContent) { this.reportContent = reportContent; }
    public List<String> getReferencedLaws() { return referencedLaws; }
    public void setReferencedLaws(List<String> referencedLaws) { this.referencedLaws = referencedLaws; }
    public List<String> getReferencedCases() { return referencedCases; }
    public void setReferencedCases(List<String> referencedCases) { this.referencedCases = referencedCases; }
    public Long getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Long generatedAt) { this.generatedAt = generatedAt; }
    public Long getTookMs() { return tookMs; }
    public void setTookMs(Long tookMs) { this.tookMs = tookMs; }
}