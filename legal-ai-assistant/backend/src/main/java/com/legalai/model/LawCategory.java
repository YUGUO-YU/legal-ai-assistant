package com.legalai.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("law_category")
public class LawCategory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long categoryTypeId;
    private Long parentId;
    private String categoryCode;
    private String categoryName;
    private String color;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
}
