package com.legalai.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("tb_case_element")
public class TbCaseElement {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long caseId;
    private String elementType;
    private String elementKey;
    private String elementValue;
    private BigDecimal importance;
}
