package com.legalai.dto;

import lombok.Data;
import java.util.List;

@Data
public class SlideDTO {
    private String id;
    private String layout;
    private String title;
    private List<String> bulletPoints;
    private String notes;
    private String backgroundUrl;
}
