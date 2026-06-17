package com.legalai.dto;

import java.util.List;

public class LegalResearchRequest {
    private String question;
    private String depth;
    private List<String> sources;

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getDepth() { return depth; }
    public void setDepth(String depth) { this.depth = depth; }
    public List<String> getSources() { return sources; }
    public void setSources(List<String> sources) { this.sources = sources; }
}