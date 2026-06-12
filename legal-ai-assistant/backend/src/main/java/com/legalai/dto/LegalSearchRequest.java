package com.legalai.dto;

import java.util.List;
import java.util.Map;

public class LegalSearchRequest {
    private String query;
    private Integer page = 1;
    private Integer pageSize = 10;
    private SearchFilters filters;
    private Boolean includeCases = true;
    private Boolean highlight = true;

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
    public SearchFilters getFilters() { return filters; }
    public void setFilters(SearchFilters filters) { this.filters = filters; }
    public Boolean getIncludeCases() { return includeCases; }
    public void setIncludeCases(Boolean includeCases) { this.includeCases = includeCases; }
    public Boolean getHighlight() { return highlight; }
    public void setHighlight(Boolean highlight) { this.highlight = highlight; }

    public static class SearchFilters {
        private List<String> categoryL1;
        private List<Integer> status;
        private Map<String, String> effectiveDateRange;

        public List<String> getCategoryL1() { return categoryL1; }
        public void setCategoryL1(List<String> categoryL1) { this.categoryL1 = categoryL1; }
        public List<Integer> getStatus() { return status; }
        public void setStatus(List<Integer> status) { this.status = status; }
        public Map<String, String> getEffectiveDateRange() { return effectiveDateRange; }
        public void setEffectiveDateRange(Map<String, String> effectiveDateRange) { this.effectiveDateRange = effectiveDateRange; }
    }
}