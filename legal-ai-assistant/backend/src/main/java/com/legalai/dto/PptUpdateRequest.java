package com.legalai.dto;

import lombok.Data;
import java.util.List;

@Data
public class PptUpdateRequest {
    private String title;
    private List<SlideDTO> slides;
    private String templateId;
}
