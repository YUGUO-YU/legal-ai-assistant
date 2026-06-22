package com.legalai.dto;

import java.util.List;

public class DocumentDraftResponse {
    private String documentContent;
    private String riskPrompt;
    private String disclaimer;
    private List<String> referencedLaws;
    private String contentSource;

    public String getDocumentContent() { return documentContent; }
    public void setDocumentContent(String documentContent) { this.documentContent = documentContent; }
    public String getRiskPrompt() { return riskPrompt; }
    public void setRiskPrompt(String riskPrompt) { this.riskPrompt = riskPrompt; }
    public String getDisclaimer() { return disclaimer; }
    public void setDisclaimer(String disclaimer) { this.disclaimer = disclaimer; }
    public List<String> getReferencedLaws() { return referencedLaws; }
    public void setReferencedLaws(List<String> referencedLaws) { this.referencedLaws = referencedLaws; }
    public String getContentSource() { return contentSource; }
    public void setContentSource(String contentSource) { this.contentSource = contentSource; }
}