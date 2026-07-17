package com.legalai.dto;

import lombok.Data;

import java.time.LocalDateTime;

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
}
