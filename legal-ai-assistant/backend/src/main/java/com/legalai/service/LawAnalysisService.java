package com.legalai.service;

import com.legalai.llm.LLMClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class LawAnalysisService {

    @Autowired
    private LLMClient llmClient;

    @Value("${mock.enabled:false}")
    private boolean mockEnabled;

    public Map<String, Object> analyzeLaw(String lawUuid, String lawTitle, List<Map<String, Object>> articles) {
        log.info("AI法规解读: lawUuid={}, lawTitle={}, articlesCount={}", lawUuid, lawTitle, articles != null ? articles.size() : 0);

        Map<String, Object> result = new HashMap<>();
        result.put("lawUuid", lawUuid);
        result.put("lawTitle", lawTitle);

        if (mockEnabled || llmClient == null) {
            return getMockAnalysis(lawUuid, lawTitle);
        }

        try {
            StringBuilder content = new StringBuilder();
            if (articles != null) {
                for (Map<String, Object> article : articles) {
                    String articleNo = (String) article.get("articleNo");
                    String title = (String) article.get("title");
                    String articleContent = (String) article.get("content");
                    content.append(articleNo);
                    if (title != null && !title.isBlank()) {
                        content.append(" ").append(title);
                    }
                    content.append("\n").append(articleContent).append("\n\n");
                }
            }

            String prompt = String.format("""
                请对以下法律法规进行深度解读分析：

                法律名称：%s

                条款内容：
                %s

                请从以下维度进行分析：
                1. 立法目的与背景
                2. 核心条款解读
                3. 重点法条释义
                4. 实践适用场景
                5. 关联法规对比
                6. 潜在法律风险点
                7. 合规建议

                请以JSON格式输出分析结果，格式如下：
                {
                    "summary": "总体概述（200字以内）",
                    "legislativePurpose": "立法目的与背景",
                    "coreProvisions": "核心条款解读",
                    "keyArticles": [{"articleNo": "条款号", "title": "条款标题", "interpretation": "条款释义"}],
                    "practicalScenarios": ["适用场景1", "适用场景2"],
                    "relatedLaws": ["关联法规1", "关联法规2"],
                    "riskPoints": ["风险点1", "风险点2"],
                    "complianceSuggestions": ["合规建议1", "合规建议2"]
                }
                """, lawTitle != null ? lawTitle : "未知法律", content.toString());

            String analysisResult = llmClient.chat(prompt);

            Map<String, Object> parsed = parseAnalysisResult(analysisResult);
            result.putAll(parsed);
            result.put("success", true);

        } catch (Exception e) {
            log.error("AI法规解读失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", "AI分析失败: " + e.getMessage());
            result.putAll(getMockAnalysis(lawUuid, lawTitle));
        }

        return result;
    }

    private Map<String, Object> getMockAnalysis(String lawUuid, String lawTitle) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("summary", "该法规是重要的法律文件，对相关领域具有指导和规范作用。");
        result.put("legislativePurpose", "明确立法目的，保护当事人合法权益，维护社会秩序。");
        result.put("coreProvisions", "核心条款规定了主体权利义务、法律责任及程序要求。");
        result.put("keyArticles", Arrays.asList(
                Map.of("articleNo", "第一条", "title", "基本原则", "interpretation", "规定法律适用的基本原则和指导思想"),
                Map.of("articleNo", "第二条", "title", "适用范围", "interpretation", "明确法律适用的范围和对象")
        ));
        result.put("practicalScenarios", Arrays.asList(
                "民事纠纷处理",
                "合同签订与履行",
                "权利救济途径"
        ));
        result.put("relatedLaws", Arrays.asList(
                "《中华人民共和国民法典》",
                "《中华人民共和国民事诉讼法》"
        ));
        result.put("riskPoints", Arrays.asList(
                "程序合规风险",
                "证据收集风险",
                "时效性风险"
        ));
        result.put("complianceSuggestions", Arrays.asList(
                "严格按照法定程序行事",
                "妥善保管相关证据材料",
                "注意权利行使的时效"
        ));
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseAnalysisResult(String jsonStr) {
        Map<String, Object> result = new HashMap<>();
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String cleanJson = jsonStr.trim();
            int start = cleanJson.indexOf('{');
            int end = cleanJson.lastIndexOf('}');
            if (start >= 0 && end > start) {
                cleanJson = cleanJson.substring(start, end + 1);
            }
            result = mapper.readValue(cleanJson, Map.class);
        } catch (Exception e) {
            log.warn("解析AI分析结果失败: {}", e.getMessage());
        }
        return result;
    }
}
