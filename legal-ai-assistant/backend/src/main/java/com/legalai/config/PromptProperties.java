package com.legalai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai.prompts")
public class PromptProperties {

    private String legalSearch;
    private String caseAnalysis;
    private String legalResearch;
    private String pptGeneration;
}
