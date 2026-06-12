package com.legalai.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("law_document")
public class LawDocument {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String lawUuid;
    private String title;
    private String shortTitle;
    private String categoryL1;
    private String categoryL2;
    private String issuingAuthority;
    private LocalDate issueDate;
    private LocalDate effectiveDate;
    private Integer status;
    private String sourceUrl;
    private String sourceName;
    private Integer viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}