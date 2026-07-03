package com.legalai.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("law_category_type")
public class LawCategoryType {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String typeCode;
    private String typeName;
    private String description;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
