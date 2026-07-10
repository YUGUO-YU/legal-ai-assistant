package com.legalai.dto;

import lombok.Data;
import java.util.List;

@Data
public class LawImportPreview {
    private String lawTitle;
    private String shortTitle;
    private String documentNo;
    private String issuingAuthority;
    private String issueDate;
    private String effectiveDate;
    private List<CategorySuggestion> suggestedCategories;
    private List<ChapterNode> chapterTree;
    private List<ArticleParse> articles;

    @Data
    public static class CategorySuggestion {
        private Long categoryTypeId;
        private String typeName;
        private Long categoryId;
        private String categoryName;
        private double confidence;
    }

    @Data
    public static class ChapterNode {
        private String title;
        private int level;
        private List<ChapterNode> children;
    }

    @Data
    public static class ArticleParse {
        private String articleUuid;
        private String articleNo;
        private String title;
        private String content;
        private String chapterPath;
        private Integer sortOrder;
        private String contentHash;
    }
}
