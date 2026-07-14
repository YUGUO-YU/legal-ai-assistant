package com.legalai.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("case_element_dict")
public class CaseElementDict {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String elementCode;
    private String elementName;
    private String category;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
}
