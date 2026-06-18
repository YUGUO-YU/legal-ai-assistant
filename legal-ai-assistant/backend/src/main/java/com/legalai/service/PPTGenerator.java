package com.legalai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalai.dto.PptGenerateRequest;
import com.legalai.dto.SlideDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 驱动的 PPT 幻灯片内容生成器。
 * 使用 LLM 根据法律检索结果生成结构化的幻灯片内容，
 * AI 调用失败时自动降级到模板化生成。
 */
@Component
public class PPTGenerator {

    private static final Logger log = LoggerFactory.getLogger(PPTGenerator.class);

    @Autowired
    private AIService aiService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
        你是一位专业的法律演示文稿设计师，擅长将法律检索结果转化为结构化的PPT幻灯片内容。

        要求：
        1. 每张幻灯片必须有明确的标题和3-5个要点
        2. 要点简洁有力，适合演讲展示
        3. 法律术语准确，引用规范
        4. 逻辑清晰：概述 -> 法规分析 -> 案例分析 -> 结论建议
        5. 封面和结语幻灯片必须包含
        """;

    private static final int MAX_AI_SLIDES = 8;

    /**
     * 根据检索结果生成完整幻灯片列表。
     */
    public List<SlideDTO> generate(String title,
                                   List<PptGenerateRequest.SearchResultItem> searchResults) {
        List<SlideDTO> slides = new ArrayList<>();

        slides.add(buildCoverSlide(title));

        if (searchResults != null && !searchResults.isEmpty()) {
            List<SlideDTO> aiSlides = generateWithAI(title, searchResults);
            if (aiSlides != null && !aiSlides.isEmpty()) {
                slides.addAll(aiSlides);
            } else {
                slides.addAll(buildTemplateSlides(searchResults));
            }
        } else {
            slides.addAll(buildGenericSlides(title));
        }

        slides.add(buildConclusionSlide());

        return slides;
    }

    private List<SlideDTO> generateWithAI(String title,
                                          List<PptGenerateRequest.SearchResultItem> searchResults) {
        String prompt = buildAIPrompt(title, searchResults);
        try {
            String response = aiService.chatWithMessages(List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", prompt)
            ));
            return parseAISlides(response);
        } catch (Exception e) {
            log.warn("AI生成PPT幻灯片失败，降级到模板生成: {}", e.getMessage());
            return null;
        }
    }

    private String buildAIPrompt(String title,
                                  List<PptGenerateRequest.SearchResultItem> searchResults) {
        StringBuilder sb = new StringBuilder();
        sb.append("请根据以下法律检索结果，生成一套PPT幻灯片内容的JSON数组。\n\n");
        sb.append("PPT主题：").append(title).append("\n\n");
        sb.append("检索结果（").append(searchResults.size()).append("条）：\n");

        for (int i = 0; i < Math.min(searchResults.size(), 8); i++) {
            PptGenerateRequest.SearchResultItem item = searchResults.get(i);
            sb.append(i + 1).append(". 《").append(item.getLawTitle()).append("》")
              .append(item.getArticleNo() != null ? " " + item.getArticleNo() : "")
              .append(" ").append(item.getTitle() != null ? item.getTitle() : "").append("\n");
            if (item.getContent() != null) {
                sb.append("   内容：").append(item.getContent().length() > 300
                    ? item.getContent().substring(0, 300) + "..."
                    : item.getContent()).append("\n");
            }
        }

        sb.append("\n返回JSON格式（严格按此结构）：\n");
        sb.append("[\n");
        sb.append("  {\"layout\":\"title_content\",\"title\":\"幻灯片标题\",\"bulletPoints\":[\"要点1\",\"要点2\",\"要点3\"],\"notes\":\"演讲备注\"}\n");
        sb.append("]\n\n");
        sb.append("规则：\n");
        sb.append("- 第1张：概述幻灯片，总结检索到的法规范围\n");
        sb.append("- 中间每张：深入分析一条关键法规，引用法条原文\n");
        sb.append("- 倒数第2张：案例/建议幻灯片\n");
        sb.append("- 总共不超过").append(MAX_AI_SLIDES).append("张\n");
        sb.append("- bulletPoints 每张3-5条，每条不超过30字\n");
        sb.append("只返回JSON数组，不要其他文字。");

        return sb.toString();
    }

    private List<SlideDTO> parseAISlides(String aiResponse) {
        List<SlideDTO> slides = new ArrayList<>();
        try {
            String json = cleanJson(aiResponse);
            List<Map<String, Object>> rawSlides = objectMapper.readValue(
                json, new TypeReference<List<Map<String, Object>>>() {});
            for (Map<String, Object> raw : rawSlides) {
                SlideDTO slide = new SlideDTO();
                slide.setId(UUID.randomUUID().toString());
                slide.setLayout(getString(raw, "layout", "title_content"));
                slide.setTitle(getString(raw, "title", ""));
                slide.setNotes(getString(raw, "notes", ""));
                Object bullets = raw.get("bulletPoints");
                if (bullets instanceof List<?> list) {
                    slide.setBulletPoints(list.stream()
                        .map(Object::toString)
                        .collect(Collectors.toList()));
                } else {
                    slide.setBulletPoints(List.of(getString(raw, "content", "")));
                }
                if (slide.getTitle() != null && !slide.getTitle().isEmpty()) {
                    slides.add(slide);
                }
            }
        } catch (Exception e) {
            log.warn("解析AI生成的幻灯片JSON失败: {}", e.getMessage());
        }
        return slides;
    }

    private String cleanJson(String raw) {
        String s = raw.trim();
        int start = s.indexOf('[');
        int end = s.lastIndexOf(']');
        if (start >= 0 && end > start) {
            s = s.substring(start, end + 1);
        }
        s = s.replaceAll("```json\\s*", "").replaceAll("```\\s*", "");
        s = s.replaceAll(",(\\s*[}\\]])", "$1");
        return s;
    }

    private String getString(Map<String, Object> map, String key, String defaultValue) {
        Object val = map.get(key);
        return val != null ? val.toString() : defaultValue;
    }

    private SlideDTO buildCoverSlide(String title) {
        SlideDTO slide = new SlideDTO();
        slide.setId(UUID.randomUUID().toString());
        slide.setLayout("title_only");
        slide.setTitle(title);
        slide.setBulletPoints(List.of(
            "基于法律AI助手的智能生成",
            "整理自相关法规和案例检索结果"
        ));
        slide.setNotes("封面幻灯片");
        return slide;
    }

    private List<SlideDTO> buildTemplateSlides(
            List<PptGenerateRequest.SearchResultItem> searchResults) {
        List<SlideDTO> slides = new ArrayList<>();

        SlideDTO overview = new SlideDTO();
        overview.setId(UUID.randomUUID().toString());
        overview.setLayout("title_content");
        overview.setTitle("检索结果概述");
        overview.setBulletPoints(List.of(
            "共检索到 " + searchResults.size() + " 条相关法规",
            "涵盖以下法律领域：",
            searchResults.stream()
                .map(r -> r.getLawTitle())
                .distinct()
                .limit(4)
                .collect(Collectors.joining("、"))
        ));
        overview.setNotes("概述检索到的法律信息");
        slides.add(overview);

        for (int i = 0; i < Math.min(searchResults.size(), 5); i++) {
            PptGenerateRequest.SearchResultItem item = searchResults.get(i);
            SlideDTO lawSlide = new SlideDTO();
            lawSlide.setId(UUID.randomUUID().toString());
            lawSlide.setLayout("title_content");
            lawSlide.setTitle(item.getTitle());
            lawSlide.setBulletPoints(List.of(
                "法规来源：" + item.getLawTitle(),
                "条款编号：" + (item.getArticleNo() != null ? item.getArticleNo() : "-"),
                "内容要点：" + truncate(item.getContent(), 200)
            ));
            lawSlide.setNotes("第" + (i + 1) + "条法规详情");
            slides.add(lawSlide);
        }

        return slides;
    }

    private List<SlideDTO> buildGenericSlides(String title) {
        SlideDTO overview = new SlideDTO();
        overview.setId(UUID.randomUUID().toString());
        overview.setLayout("title_content");
        overview.setTitle(title + " - 概述");
        overview.setBulletPoints(List.of(
            "本演示文稿基于法律AI助手生成",
            "可涵盖相关法律法规、司法解释和案例参考",
            "请结合实际案情补充具体内容"
        ));
        overview.setNotes("通用概述幻灯片");
        return List.of(overview);
    }

    private SlideDTO buildConclusionSlide() {
        SlideDTO slide = new SlideDTO();
        slide.setId(UUID.randomUUID().toString());
        slide.setLayout("title_content");
        slide.setTitle("总结与建议");
        slide.setBulletPoints(List.of(
            "结合本案实际情况，综合考虑相关法律规定",
            "建议咨询专业律师获取个性化法律意见",
            "本PPT内容由法律AI助手生成，仅供参考"
        ));
        slide.setNotes("结语幻灯片");
        return slide;
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }
}
