package com.legalai.dto;

import java.util.List;

public class CaseAnalysisResponse {
    private String caseUuid;
    private String caseNo;
    private String title;
    private List<AnalysisSection> sections;
    private List<String> relatedLaws;
    private List<String> relatedCases;
    private String disclaimer;
    private Long generatedAt;
    private Long tookMs;

    public String getCaseUuid() { return caseUuid; }
    public void setCaseUuid(String caseUuid) { this.caseUuid = caseUuid; }
    public String getCaseNo() { return caseNo; }
    public void setCaseNo(String caseNo) { this.caseNo = caseNo; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public List<AnalysisSection> getSections() { return sections; }
    public void setSections(List<AnalysisSection> sections) { this.sections = sections; }
    public List<String> getRelatedLaws() { return relatedLaws; }
    public void setRelatedLaws(List<String> relatedLaws) { this.relatedLaws = relatedLaws; }
    public List<String> getRelatedCases() { return relatedCases; }
    public void setRelatedCases(List<String> relatedCases) { this.relatedCases = relatedCases; }
    public String getDisclaimer() { return disclaimer; }
    public void setDisclaimer(String disclaimer) { this.disclaimer = disclaimer; }
    public Long getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Long generatedAt) { this.generatedAt = generatedAt; }
    public Long getTookMs() { return tookMs; }
    public void setTookMs(Long tookMs) { this.tookMs = tookMs; }

    public static class AnalysisSection {
        private String id;
        private String title;
        private String icon;
        private String content;
        private List<String> keyPoints;
        private String level;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public List<String> getKeyPoints() { return keyPoints; }
        public void setKeyPoints(List<String> keyPoints) { this.keyPoints = keyPoints; }
        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }
    }
}
