package com.legalai.dto;

import lombok.Data;
import java.util.List;

@Data
public class PptGenerateRequest {
    private String title;
    private List<SearchResultItem> searchResults;
    private String templateId;
    private String userId;

    @Data
    public static class SearchResultItem {
        private String articleId;
        private String lawTitle;
        private String articleNo;
        private String title;
        private String content;
        private Double score;
    }
}
