package com.legalai.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("tb_case")
public class CaseInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String caseUuid;
    private String caseNo;
    private String caseName;
    private Integer caseType;
    private String caseCause;
    private Integer courtLevel;
    private String courtName;
    private LocalDate judgeDate;
    private String trialProcedure;
    private Integer judgmentResult;
    private BigDecimal litigationAmount;
    private String plaintiff;
    private String defendant;
    private String keyFacts;
    private String judgmentSummary;
    private String legalBasis;
    private Integer vectorStatus;
    private LocalDateTime createdAt;
}