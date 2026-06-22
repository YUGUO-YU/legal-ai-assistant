package com.legalai.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class LawImportJob {
    private Long id;
    private String taskUuid;
    private String lawName;
    private String source;
    private String status;
    private Integer totalArticles;
    private Integer insertedArticles;
    private Integer updatedArticles;
    private Boolean mysqlOk;
    private Boolean esOk;
    private Boolean milvusOk;
    private String errorMessage;
    private String operator;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private List<ImportedArticle> articles;

    @Data
    public static class ImportedArticle {
        private String articleNo;
        private String title;
        private String content;
    }
}
