package com.legalai.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("law_article")
public class LawArticle {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long lawId;
    private String articleUuid;
    private String articleNo;
    private String title;
    private String content;
    private String contentHash;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}