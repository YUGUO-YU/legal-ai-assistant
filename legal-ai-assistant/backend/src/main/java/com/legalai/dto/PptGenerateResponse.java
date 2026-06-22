package com.legalai.dto;

import lombok.Data;
import java.util.List;

@Data
public class PptGenerateResponse {
    private String id;
    private String uuid;
    private String title;
    private List<SlideDTO> slides;
    private String templateId;
    private Long createdAt;
}
