package com.legalai.service;

import com.legalai.config.PromptProperties;
import com.legalai.dto.CaseAnalysisResponse;
import com.legalai.dto.CaseSearchResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class CaseAnalysisService {
    private static final Logger log = LoggerFactory.getLogger(CaseAnalysisService.class);

    @Autowired
    private CaseSearchService caseSearchService;

    @Autowired
    private AIService aiService;

    @Autowired
    private PromptProperties promptProperties;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String getSystemPrompt() {
        return promptProperties.getCaseAnalysis();
    }

    public CaseAnalysisResponse getAnalysis(String caseUuid) {
        long start = System.currentTimeMillis();
        CaseSearchResponse.CaseSearchItem caseItem = caseSearchService.getCaseDetail(caseUuid);
        if (caseItem == null) {
            throw new IllegalArgumentException("案例不存在");
        }

        return buildAIAnalysis(caseItem, start);
    }

    public Flux<Map<String, Object>> streamAnalysis(String caseUuid) {
        CaseSearchResponse.CaseSearchItem caseItem = caseSearchService.getCaseDetail(caseUuid);
        if (caseItem == null) {
            return Flux.just(Map.of("type", "error", "message", "案例不存在"));
        }

        return aiStreamAnalysis(caseItem);
    }

    private Flux<Map<String, Object>> aiStreamAnalysis(CaseSearchResponse.CaseSearchItem caseItem) {
        try {
            String prompt = buildAnalysisPrompt(caseItem);
            AtomicReference<String> aiContent = new AtomicReference<>("");

            List<Map<String, Object>> progressEvents = List.of(
                Map.of("type", "progress", "phase", "load", "progress", 10, "message", "加载案件信息..."),
                Map.of("type", "progress", "phase", "facts", "progress", 25, "message", "解析案件事实..."),
                Map.of("type", "progress", "phase", "dispute", "progress", 45, "message", "识别争议焦点..."),
                Map.of("type", "progress", "phase", "reasoning", "progress", 65, "message", "AI 分析裁判逻辑..."),
                Map.of("type", "progress", "phase", "advice", "progress", 85, "message", "生成应对建议...")
            );

            return Flux.concat(
                Flux.fromIterable(progressEvents).delayElements(Duration.ofMillis(200)),
                Flux.defer(() -> {
                    try {
                        String result = aiService.chat(prompt);
                        aiContent.set(result);
                        CaseAnalysisResponse analysis = parseAIResponse(result, caseItem);
                        return Flux.just(Map.of(
                            "type", "analysis",
                            "data", analysis
                        ));
                    } catch (IOException e) {
                        log.error("AI 案情分析失败: {}", e.getMessage());
                        throw new IllegalStateException("AI 服务暂不可用，无法完成案情分析: " + e.getMessage());
                    }
                }),
                Flux.just(Map.of("type", "progress", "phase", "complete", "progress", 100, "message", "分析完成"))
            );
        } catch (Exception e) {
            log.error("流式分析异常: {}", e.getMessage());
            return Flux.just(Map.of("type", "error", "message", e.getMessage()));
        }
    }

    private String buildAnalysisPrompt(CaseSearchResponse.CaseSearchItem caseItem) {
        StringBuilder sb = new StringBuilder();
        sb.append(getSystemPrompt()).append("\n\n");
        sb.append("【案件信息】\n");
        sb.append("案号：").append(nullSafe(caseItem.getCaseNo())).append("\n");
        sb.append("案件名称：").append(nullSafe(caseItem.getTitle())).append("\n");
        sb.append("法院：").append(nullSafe(caseItem.getCourt())).append("\n");
        sb.append("案件类型：").append(nullSafe(caseItem.getCaseType())).append("\n");
        sb.append("案由：").append(nullSafe(caseItem.getCaseCause())).append("\n");
        sb.append("审理程序：").append(nullSafe(caseItem.getTrialProcedure())).append("\n");
        sb.append("裁判日期：").append(nullSafe(caseItem.getJudgeDate())).append("\n");
        sb.append("诉讼金额：").append(caseItem.getLitigationAmount() != null ? caseItem.getLitigationAmount() + "元" : "未知").append("\n\n");

        sb.append("【关键事实】\n").append(nullSafe(caseItem.getKeyFacts())).append("\n\n");
        sb.append("【案件摘要】\n").append(nullSafe(caseItem.getSummary())).append("\n\n");
        sb.append("【裁判要旨】\n").append(nullSafe(caseItem.getJudgmentSummary())).append("\n\n");
        sb.append("【法律依据】\n").append(nullSafe(caseItem.getLegalBasis())).append("\n\n");

        sb.append("请按以下 JSON 结构输出：\n");
        sb.append("{\n");
        sb.append("  \"sections\": [\n");
        sb.append("    {\"id\": \"facts\", \"title\": \"案件事实梳理\", \"icon\": \"📋\", \"content\": \"...\", \"keyPoints\": [\"要点1\", \"要点2\"]},\n");
        sb.append("    {\"id\": \"dispute\", \"title\": \"争议焦点识别\", \"icon\": \"⚖️\", \"content\": \"...\", \"keyPoints\": [...]},\n");
        sb.append("    {\"id\": \"reasoning\", \"title\": \"裁判逻辑分析\", \"icon\": \"🧠\", \"content\": \"...\", \"keyPoints\": [...]},\n");
        sb.append("    {\"id\": \"law\", \"title\": \"法律适用评析\", \"icon\": \"📖\", \"content\": \"...\", \"keyPoints\": [...]},\n");
        sb.append("    {\"id\": \"advice\", \"title\": \"应对策略建议\", \"icon\": \"💡\", \"content\": \"...\", \"keyPoints\": [...], \"level\": \"LOW|MEDIUM|HIGH\"},\n");
        sb.append("    {\"id\": \"risk\", \"title\": \"风险提示\", \"icon\": \"⚠️\", \"content\": \"...\", \"keyPoints\": [...], \"level\": \"LOW|MEDIUM|HIGH\"}\n");
        sb.append("  ],\n");
        sb.append("  \"relatedLaws\": [\"《XXX法》第X条 - 说明\"],\n");
        sb.append("  \"relatedCases\": [\"类似案号或场景\"]\n");
        sb.append("}\n\n");
        sb.append("只输出 JSON，不要任何解释。");

        return sb.toString();
    }

    private CaseAnalysisResponse parseAIResponse(String aiContent, CaseSearchResponse.CaseSearchItem caseItem) {
        long start = System.currentTimeMillis();
        try {
            String json = extractJson(aiContent);
            JsonNode root = objectMapper.readTree(json);

            CaseAnalysisResponse response = new CaseAnalysisResponse();
            response.setCaseUuid(caseItem.getCaseUuid());
            response.setCaseNo(caseItem.getCaseNo());
            response.setTitle(caseItem.getTitle());
            response.setGeneratedAt(System.currentTimeMillis());
            response.setDisclaimer("本分析由 AI 生成，仅供参考，不构成正式法律意见。");

            List<CaseAnalysisResponse.AnalysisSection> sections = new ArrayList<>();
            if (root.has("sections")) {
                for (JsonNode sec : root.get("sections")) {
                    CaseAnalysisResponse.AnalysisSection s = new CaseAnalysisResponse.AnalysisSection();
                    s.setId(sec.has("id") ? sec.get("id").asText() : "");
                    s.setTitle(sec.has("title") ? sec.get("title").asText() : "");
                    s.setIcon(sec.has("icon") ? sec.get("icon").asText() : "");
                    s.setContent(sec.has("content") ? sec.get("content").asText() : "");
                    s.setLevel(sec.has("level") ? sec.get("level").asText() : null);
                    List<String> points = new ArrayList<>();
                    if (sec.has("keyPoints") && sec.get("keyPoints").isArray()) {
                        for (JsonNode p : sec.get("keyPoints")) {
                            points.add(p.asText());
                        }
                    }
                    s.setKeyPoints(points);
                    sections.add(s);
                }
            }
            response.setSections(sections);

            List<String> laws = new ArrayList<>();
            if (root.has("relatedLaws") && root.get("relatedLaws").isArray()) {
                for (JsonNode l : root.get("relatedLaws")) {
                    laws.add(l.asText());
                }
            }
            response.setRelatedLaws(laws);

            List<String> cases = new ArrayList<>();
            if (root.has("relatedCases") && root.get("relatedCases").isArray()) {
                for (JsonNode c : root.get("relatedCases")) {
                    cases.add(c.asText());
                }
            }
            response.setRelatedCases(cases);

            response.setTookMs(System.currentTimeMillis() - start);
            return response;
        } catch (Exception e) {
            log.error("解析 AI 案情分析响应失败: {}", e.getMessage());
            throw new IllegalStateException("解析 AI 响应失败: " + e.getMessage());
        }
    }

    private String extractJson(String text) {
        if (text == null) return "{}";
        String trimmed = text.trim();
        int jsonStart = trimmed.indexOf("{");
        int jsonEnd = trimmed.lastIndexOf("}");
        if (jsonStart != -1 && jsonEnd != -1 && jsonStart < jsonEnd) {
            return trimmed.substring(jsonStart, jsonEnd + 1);
        }
        return trimmed;
    }

    private CaseAnalysisResponse buildAIAnalysis(CaseSearchResponse.CaseSearchItem caseItem, long start) {
        try {
            String prompt = buildAnalysisPrompt(caseItem);
            String result = aiService.chat(prompt);
            return parseAIResponse(result, caseItem);
        } catch (IOException e) {
            log.error("AI 案情分析失败: {}", e.getMessage());
            throw new IllegalStateException("AI 服务暂不可用，无法完成案情分析: " + e.getMessage());
        }
    }

    private String nullSafe(String s) {
        return s == null || s.isEmpty() ? "未知" : s;
    }
}
