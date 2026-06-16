package com.legalai.dto;

import java.util.List;

public class LegalSearchResponse {
    private Long total;
    private Integer page;
    private Integer pageSize;
    private Long tookMs;
    private Long searchLogId;
    private List<SearchResultItem> items;
    private List<RelatedCase> relatedCases;

    public Long getTotal() { return total; }
    public void setTotal(Long total) { this.total = total; }
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
    public Long getTookMs() { return tookMs; }
    public void setTookMs(Long tookMs) { this.tookMs = tookMs; }
    public Long getSearchLogId() { return searchLogId; }
    public void setSearchLogId(Long searchLogId) { this.searchLogId = searchLogId; }
    public List<SearchResultItem> getItems() { return items; }
    public void setItems(List<SearchResultItem> items) { this.items = items; }
    public List<RelatedCase> getRelatedCases() { return relatedCases; }
    public void setRelatedCases(List<RelatedCase> relatedCases) { this.relatedCases = relatedCases; }

    public static class SearchResultItem {
        private String articleId;
        private String lawId;
        private String lawTitle;
        private String articleNo;
        private String title;
        private String content;
        private List<String> highlights;
        private String sourceUrl;
        private String sourceName;
        private Double score;
        private Integer relatedCasesCount;
        private String categoryL1;
        private String categoryL2;

        public String getArticleId() { return articleId; }
        public void setArticleId(String articleId) { this.articleId = articleId; }
        public String getLawId() { return lawId; }
        public void setLawId(String lawId) { this.lawId = lawId; }
        public String getLawTitle() { return lawTitle; }
        public void setLawTitle(String lawTitle) { this.lawTitle = lawTitle; }
        public String getArticleNo() { return articleNo; }
        public void setArticleNo(String articleNo) { this.articleNo = articleNo; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public List<String> getHighlights() { return highlights; }
        public void setHighlights(List<String> highlights) { this.highlights = highlights; }
        public String getSourceUrl() { return sourceUrl; }
        public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
        public String getSourceName() { return sourceName; }
        public void setSourceName(String sourceName) { this.sourceName = sourceName; }
        public Double getScore() { return score; }
        public void setScore(Double score) { this.score = score; }
        public Integer getRelatedCasesCount() { return relatedCasesCount; }
        public void setRelatedCasesCount(Integer relatedCasesCount) { this.relatedCasesCount = relatedCasesCount; }
        public String getCategoryL1() { return categoryL1; }
        public void setCategoryL1(String categoryL1) { this.categoryL1 = categoryL1; }
        public String getCategoryL2() { return categoryL2; }
        public void setCategoryL2(String categoryL2) { this.categoryL2 = categoryL2; }
    }

    public static class RelatedCase {
        private String caseUuid;
        private String caseNo;
        private String title;
        private String court;
        private String summary;
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
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        public String getSourceUrl() { return sourceUrl; }
        public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
        public String getSourceName() { return sourceName; }
        public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    }
}