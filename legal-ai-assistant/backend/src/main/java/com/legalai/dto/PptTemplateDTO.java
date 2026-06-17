package com.legalai.dto;

import lombok.Data;

@Data
public class PptTemplateDTO {
    private String id;
    private String name;
    private String thumbnail;
    private String primaryColor;
    private String secondaryColor;
    private String fontFamily;
    private String backgroundStyle;
    private String source;
}
