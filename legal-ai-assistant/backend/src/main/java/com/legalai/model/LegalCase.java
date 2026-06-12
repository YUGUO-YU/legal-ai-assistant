package com.legalai.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("legal_case")
public class LegalCase {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String caseUuid;
    private String caseNo;
    private String title;
    private String court;
    private String caseType;
    private String caseCause;
    private LocalDate judgmentDate;
    private String summary;
    private String fullTextUrl;
    private String sourceUrl;
    private String sourceName;
    private LocalDateTime createdAt;
}