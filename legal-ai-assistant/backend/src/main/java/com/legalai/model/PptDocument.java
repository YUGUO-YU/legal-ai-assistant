package com.legalai.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PptDocument {
    private Long id;
    private String pptUuid;
    private String title;
    private String slidesJson;
    private String templateId;
    private String userId;
    private String filePath;
    private Long fileSize;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
