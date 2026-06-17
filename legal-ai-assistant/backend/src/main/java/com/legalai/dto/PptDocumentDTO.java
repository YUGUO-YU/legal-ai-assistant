package com.legalai.dto;

import lombok.Data;
import java.util.List;

@Data
public class PptDocumentDTO {
    private String id;
    private String uuid;
    private String title;
    private List<SlideDTO> slides;
    private String templateId;
    private String userId;
    private String status;
    private Long fileSize;
    private String createdAt;
    private String updatedAt;
}
