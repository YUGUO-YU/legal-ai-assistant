package com.legalai.service;

import com.legalai.dto.PptTemplateDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PptTemplateService {

    private static final Logger log = LoggerFactory.getLogger(PptTemplateService.class);

    @Autowired
    private AIService aiService;

    private static final List<PptTemplateDTO> BUILT_IN_TEMPLATES = Arrays.asList(
        createTemplate("legal-blue", "法律蓝调", "1a365d", "2c5282", "gradient",
            "适合正式法律论坛、学术演讲", "cases,seminar,court"),
        createTemplate("purple-peak", "紫禁之巅", "553c9a", "805ad5", "gradient",
            "适合高端商务演示", "contract,business,merger"),
        createTemplate("professional", "专业沉稳", "2d3748", "4a5568", "solid",
            "适合内部培训、工作汇报", "training,report,internal"),
        createTemplate("fresh-minimal", "清新简约", "319795", "38b2ac", "light",
            "适合案例分析分享", "case_study,share,analysis"),
        createTemplate("court-gold", "法院灰金", "744210", "d69e2e", "gradient",
            "适合司法研讨、学术交流", "judicial,academic,discussion"),
        createTemplate("minimal-dark", "极简深色", "111827", "1f2937", "solid",
            "适合严肃主题、刑事辩护", "criminal,defense,formal"),
        createTemplate("ocean-breeze", "海洋清风", "0ea5e9", "0284c7", "gradient",
            "适合知识产权、创新主题", "ip,innovation,tech"),
        createTemplate("coral-sunset", "珊瑚日落", "e11d48", "be123c", "gradient",
            "适合消费者权益、社会热点", "consumer,public,rights")
    );

    public List<PptTemplateDTO> getTemplates() {
        return BUILT_IN_TEMPLATES;
    }

    /**
     * AI 场景化模板推荐：根据用户输入的场景描述，由 AI 选择最匹配的模板。
     */
    public List<PptTemplateDTO> getAiRecommendedTemplates(String scenario) {
        if (scenario == null || scenario.trim().isEmpty()) {
            return BUILT_IN_TEMPLATES.subList(0, 4);
        }

        try {
            String aiPicks = aiService.chatWithMessages(List.of(
                Map.of("role", "system", "content", """
                    你是PPT模板推荐专家。根据用户描述的场景，从以下8个模板中选择最合适的3个。
                    只返回模板ID，逗号分隔。不要其他文字。
                    
                    模板列表：
                    legal-blue - 法律蓝调 - 关键词: cases,seminar,court
                    purple-peak - 紫禁之巅 - 关键词: contract,business,merger
                    professional - 专业沉稳 - 关键词: training,report,internal
                    fresh-minimal - 清新简约 - 关键词: case_study,share,analysis
                    court-gold - 法院灰金 - 关键词: judicial,academic,discussion
                    minimal-dark - 极简深色 - 关键词: criminal,defense,formal
                    ocean-breeze - 海洋清风 - 关键词: ip,innovation,tech
                    coral-sunset - 珊瑚日落 - 关键词: consumer,public,rights
                    """),
                Map.of("role", "user", "content", "场景：" + scenario)
            ));

            List<String> ids = Arrays.stream(aiPicks.trim().split("[,，\\s]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

            List<PptTemplateDTO> recommended = new ArrayList<>();
            for (String id : ids) {
                BUILT_IN_TEMPLATES.stream()
                    .filter(t -> t.getId().equals(id))
                    .findFirst()
                    .ifPresent(t -> {
                        PptTemplateDTO copy = copyTemplate(t);
                        copy.setSource("ai");
                        recommended.add(copy);
                    });
            }

            if (!recommended.isEmpty()) {
                return recommended;
            }
        } catch (Exception e) {
            log.warn("AI模板推荐失败，使用默认推荐: {}", e.getMessage());
        }

        return keywordMatch(scenario);
    }

    private List<PptTemplateDTO> keywordMatch(String scenario) {
        String lower = scenario.toLowerCase();
        Map<PptTemplateDTO, Integer> scores = new LinkedHashMap<>();

        for (PptTemplateDTO t : BUILT_IN_TEMPLATES) {
            int score = 0;
            String tags = t.getDescription() + " " + t.getTags();
            for (String word : lower.split("[\\s,，]+")) {
                if (tags.contains(word)) score += 10;
            }
            if (score > 0) {
                scores.put(t, score);
            }
        }

        List<PptTemplateDTO> result = scores.entrySet().stream()
            .sorted(Map.Entry.<PptTemplateDTO, Integer>comparingByValue().reversed())
            .limit(4)
            .map(e -> {
                PptTemplateDTO copy = copyTemplate(e.getKey());
                copy.setSource("ai");
                return copy;
            })
            .collect(Collectors.toList());

        if (result.isEmpty()) {
            return BUILT_IN_TEMPLATES.subList(0, 4).stream()
                .map(this::copyTemplate)
                .collect(Collectors.toList());
        }
        return result;
    }

    public PptTemplateDTO getTemplateById(String id) {
        return BUILT_IN_TEMPLATES.stream()
            .filter(t -> t.getId().equals(id))
            .findFirst()
            .orElse(BUILT_IN_TEMPLATES.get(0));
    }

    private PptTemplateDTO copyTemplate(PptTemplateDTO source) {
        PptTemplateDTO copy = new PptTemplateDTO();
        copy.setId(source.getId());
        copy.setName(source.getName());
        copy.setPrimaryColor(source.getPrimaryColor());
        copy.setSecondaryColor(source.getSecondaryColor());
        copy.setFontFamily(source.getFontFamily());
        copy.setBackgroundStyle(source.getBackgroundStyle());
        copy.setDescription(source.getDescription());
        copy.setTags(source.getTags());
        return copy;
    }

    private static PptTemplateDTO createTemplate(String id, String name, String primaryColor,
                                                  String secondaryColor, String backgroundStyle,
                                                  String description, String tags) {
        PptTemplateDTO template = new PptTemplateDTO();
        template.setId(id);
        template.setName(name);
        template.setPrimaryColor(primaryColor);
        template.setSecondaryColor(secondaryColor);
        template.setFontFamily("Microsoft YaHei");
        template.setBackgroundStyle(backgroundStyle);
        template.setDescription(description);
        template.setTags(tags);
        template.setSource("local");
        return template;
    }
}
