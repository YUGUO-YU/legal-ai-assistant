package com.legalai.dto;

import java.util.List;

public class LawSearchResponse {
    private Long total;
    private Integer page;
    private Integer pageSize;
    private Long tookMs;
    private List<LawSearchItem> items;

    public Long getTotal() { return total; }
    public void setTotal(Long total) { this.total = total; }
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
    public Long getTookMs() { return tookMs; }
    public void setTookMs(Long tookMs) { this.tookMs = tookMs; }
    public List<LawSearchItem> getItems() { return items; }
    public void setItems(List<LawSearchItem> items) { this.items = items; }

    public static class LawSearchItem {
        private String lawUuid;
        private String title;
        private String shortTitle;
        private String categoryL1;
        private String categoryL2;
        private String issuingAuthority;
        private String issueDate;
        private String effectiveDate;
        private Integer status;
        private String statusName;
        private String sourceUrl;
        private String sourceName;
        private Integer articleCount;
        private Integer viewCount;

        public String getLawUuid() { return lawUuid; }
        public void setLawUuid(String lawUuid) { this.lawUuid = lawUuid; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getShortTitle() { return shortTitle; }
        public void setShortTitle(String shortTitle) { this.shortTitle = shortTitle; }
        public String getCategoryL1() { return categoryL1; }
        public void setCategoryL1(String categoryL1) { this.categoryL1 = categoryL1; }
        public String getCategoryL2() { return categoryL2; }
        public void setCategoryL2(String categoryL2) { this.categoryL2 = categoryL2; }
        public String getIssuingAuthority() { return issuingAuthority; }
        public void setIssuingAuthority(String issuingAuthority) { this.issuingAuthority = issuingAuthority; }
        public String getIssueDate() { return issueDate; }
        public void setIssueDate(String issueDate) { this.issueDate = issueDate; }
        public String getEffectiveDate() { return effectiveDate; }
        public void setEffectiveDate(String effectiveDate) { this.effectiveDate = effectiveDate; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
        public String getStatusName() { return statusName; }
        public void setStatusName(String statusName) { this.statusName = statusName; }
        public String getSourceUrl() { return sourceUrl; }
        public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
        public String getSourceName() { return sourceName; }
        public void setSourceName(String sourceName) { this.sourceName = sourceName; }
        public Integer getArticleCount() { return articleCount; }
        public void setArticleCount(Integer articleCount) { this.articleCount = articleCount; }
        public Integer getViewCount() { return viewCount; }
        public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }
    }
}