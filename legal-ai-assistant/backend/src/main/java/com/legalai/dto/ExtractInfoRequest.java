package com.legalai.dto;

public class ExtractInfoRequest {
    private String text;
    private String templateCode;

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }
}
