package com.legalai.service;

import com.legalai.dto.PptTemplateDTO;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class PptTemplateService {

    private static final List<PptTemplateDTO> BUILT_IN_TEMPLATES = Arrays.asList(
            createTemplate("legal-blue", "法律蓝调", "#1a365d", "#2c5282", "gradient", "适合正式法律论坛、学术演讲"),
            createTemplate("purple-peak", "紫禁之巅", "#553c9a", "#805ad5", "gradient", "适合高端商务演示"),
            createTemplate("professional", "专业沉稳", "#2d3748", "#4a5568", "solid", "适合内部培训、工作汇报"),
            createTemplate("fresh-minimal", "清新简约", "#319795", "#38b2ac", "light", "适合案例分析分享"),
            createTemplate("court-gold", "法院灰金", "#744210", "#d69e2e", "gradient", "适合司法研讨、学术交流")
    );

    public List<PptTemplateDTO> getTemplates() {
        return BUILT_IN_TEMPLATES;
    }

    public List<PptTemplateDTO> getAiRecommendedTemplates(String scenario) {
        return BUILT_IN_TEMPLATES;
    }

    public PptTemplateDTO getTemplateById(String id) {
        return BUILT_IN_TEMPLATES.stream()
                .filter(t -> t.getId().equals(id))
                .findFirst()
                .orElse(BUILT_IN_TEMPLATES.get(0));
    }

    private static PptTemplateDTO createTemplate(String id, String name, String primaryColor,
                                                  String secondaryColor, String backgroundStyle, String description) {
        PptTemplateDTO template = new PptTemplateDTO();
        template.setId(id);
        template.setName(name);
        template.setPrimaryColor(primaryColor);
        template.setSecondaryColor(secondaryColor);
        template.setFontFamily("Microsoft YaHei");
        template.setBackgroundStyle(backgroundStyle);
        template.setSource("local");
        return template;
    }
}
