package com.legalai.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("search_log")
public class SearchLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String userId;
    private String queryText;
    private String intentType;
    private Integer resultCount;
    private Integer responseTimeMs;
    private LocalDateTime createdAt;
}