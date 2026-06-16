package com.legalai.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Citation {

    private String type;
    private String lawName;
    private String articleNo;
    private String sourceUrl;
    private String sourceName;
    private String crawlTime;
    private Double confidence;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public Citation() {
    }

    public Citation(String lawName, String articleNo, String sourceUrl, String sourceName) {
        this.type = "law";
        this.lawName = lawName;
        this.articleNo = articleNo;
        this.sourceUrl = sourceUrl;
        this.sourceName = sourceName;
        this.crawlTime = LocalDateTime.now().format(FORMATTER);
        this.confidence = 1.0;
    }

    public String toFormattedString() {
        return String.format("%s %s | %s",
            lawName != null ? lawName : "",
            articleNo != null ? articleNo : "",
            sourceUrl != null ? sourceUrl : ""
        );
    }

    public String toMarkdownFormat() {
        StringBuilder sb = new StringBuilder();
        sb.append("1. ");
        if (lawName != null && articleNo != null) {
            sb.append("[").append(lawName).append("] ").append(articleNo);
        }
        if (sourceUrl != null) {
            sb.append(" | ").append(sourceUrl);
        }
        return sb.toString();
    }

    public static Citation fromSearchResultItem(LegalSearchResponse.SearchResultItem item) {
        Citation citation = new Citation();
        citation.setType("law");
        citation.setLawName(item.getLawTitle());
        citation.setArticleNo(item.getArticleNo());
        citation.setSourceUrl(item.getSourceUrl());
        citation.setSourceName(item.getSourceName());
        citation.setCrawlTime(LocalDateTime.now().format(FORMATTER));
        citation.setConfidence(0.98);
        return citation;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getLawName() { return lawName; }
    public void setLawName(String lawName) { this.lawName = lawName; }
    public String getArticleNo() { return articleNo; }
    public void setArticleNo(String articleNo) { this.articleNo = articleNo; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public String getCrawlTime() { return crawlTime; }
    public void setCrawlTime(String crawlTime) { this.crawlTime = crawlTime; }
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
}