package com.legalai.service;

import com.legalai.dto.CaseAnalysisResponse;
import com.legalai.dto.CaseSearchResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class CaseAnalysisService {
    private static final Logger log = LoggerFactory.getLogger(CaseAnalysisService.class);

    private static final String SYSTEM_PROMPT =
        "你是一位资深中国法律专家，擅长深度分析司法判例。请基于用户提供的案例信息，" +
        "输出结构化的 AI 案情分析，帮助律师快速理解案件核心要素和裁判逻辑。\n\n" +
        "分析要求：\n" +
        "1. 必须严格基于提供的案件信息，不要凭空捏造事实\n" +
        "2. 使用专业法律术语，语言严谨\n" +
        "3. 引用法律条文需具体到条号\n" +
        "4. 风险提示需明确等级（LOW/MEDIUM/HIGH）\n" +
        "5. 输出为 JSON 格式，结构清晰\n";

    @Value("${mock.enabled:true}")
    private boolean mockEnabled;

    @Autowired
    private CaseSearchService caseSearchService;

    @Autowired
    private AIService aiService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public CaseAnalysisResponse getAnalysis(String caseUuid) {
        long start = System.currentTimeMillis();
        CaseSearchResponse.CaseSearchItem caseItem = caseSearchService.getCaseDetail(caseUuid);
        if (caseItem == null) {
            throw new IllegalArgumentException("案例不存在");
        }

        if (mockEnabled) {
            return buildMockAnalysis(caseItem, start);
        }
        return buildAIAnalysis(caseItem, start);
    }

    public Flux<Map<String, Object>> streamAnalysis(String caseUuid) {
        CaseSearchResponse.CaseSearchItem caseItem = caseSearchService.getCaseDetail(caseUuid);
        if (caseItem == null) {
            return Flux.just(Map.of("type", "error", "message", "案例不存在"));
        }

        if (mockEnabled) {
            return mockStreamAnalysis(caseItem);
        }
        return aiStreamAnalysis(caseItem);
    }

    private Flux<Map<String, Object>> mockStreamAnalysis(CaseSearchResponse.CaseSearchItem caseItem) {
        List<Map<String, Object>> phases = List.of(
            Map.of("type", "progress", "phase", "load", "progress", 10, "message", "加载案件信息..."),
            Map.of("type", "progress", "phase", "facts", "progress", 25, "message", "解析案件事实..."),
            Map.of("type", "progress", "phase", "dispute", "progress", 45, "message", "识别争议焦点..."),
            Map.of("type", "progress", "phase", "reasoning", "progress", 65, "message", "分析裁判逻辑..."),
            Map.of("type", "progress", "phase", "advice", "progress", 85, "message", "生成应对建议..."),
            Map.of("type", "progress", "phase", "complete", "progress", 100, "message", "分析完成")
        );

        CaseAnalysisResponse mockResp = buildMockAnalysis(caseItem, System.currentTimeMillis());

        return Flux.concat(
            Flux.fromIterable(phases).delayElements(Duration.ofMillis(300)),
            Flux.just(Map.of(
                "type", "analysis",
                "data", mockResp
            ))
        );
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
                        CaseAnalysisResponse fallback = buildMockAnalysis(caseItem, System.currentTimeMillis());
                        return Flux.just(Map.of(
                            "type", "analysis",
                            "data", fallback,
                            "warning", "AI 服务暂不可用，已返回示例分析"
                        ));
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
        sb.append(SYSTEM_PROMPT).append("\n\n");
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
            return buildMockAnalysis(caseItem, start);
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
            return buildMockAnalysis(caseItem, start);
        }
    }

    private CaseAnalysisResponse buildMockAnalysis(CaseSearchResponse.CaseSearchItem caseItem, long start) {
        CaseAnalysisResponse response = new CaseAnalysisResponse();
        response.setCaseUuid(caseItem.getCaseUuid());
        response.setCaseNo(caseItem.getCaseNo());
        response.setTitle(caseItem.getTitle());
        response.setGeneratedAt(System.currentTimeMillis());
        response.setTookMs(System.currentTimeMillis() - start);
        response.setDisclaimer("本分析由 AI 生成（示例数据），仅供参考，不构成正式法律意见。");

        List<CaseAnalysisResponse.AnalysisSection> sections = new ArrayList<>();

        CaseAnalysisResponse.AnalysisSection facts = new CaseAnalysisResponse.AnalysisSection();
        facts.setId("facts");
        facts.setTitle("案件事实梳理");
        facts.setIcon("📋");
        facts.setContent(buildFactsContent(caseItem));
        facts.setKeyPoints(Arrays.asList(
            "案件类型：" + nullSafe(caseItem.getCaseType()),
            "案由：" + nullSafe(caseItem.getCaseCause()),
            "审理程序：" + nullSafe(caseItem.getTrialProcedure())
        ));
        sections.add(facts);

        CaseAnalysisResponse.AnalysisSection dispute = new CaseAnalysisResponse.AnalysisSection();
        dispute.setId("dispute");
        dispute.setTitle("争议焦点识别");
        dispute.setIcon("⚖️");
        dispute.setContent("基于案件事实，本案的核心争议焦点集中在以下方面：\n1. 法律关系的性质认定\n2. 各方当事人的权利义务边界\n3. 法律责任的承担与分配方式");
        dispute.setKeyPoints(Arrays.asList(
            "法律关系性质认定",
            "权利义务边界",
            "责任承担与分配"
        ));
        sections.add(dispute);

        CaseAnalysisResponse.AnalysisSection reasoning = new CaseAnalysisResponse.AnalysisSection();
        reasoning.setId("reasoning");
        reasoning.setTitle("裁判逻辑分析");
        reasoning.setIcon("🧠");
        reasoning.setContent("法院在审理过程中遵循了"三段论"式法律推理方法：\n\n1. **事实认定**：基于双方提交的证据材料，对争议事实进行认定\n2. **法律适用**：根据认定的法律关系性质，援引相应的法律条文\n3. **裁判结论**：综合事实与法律，作出裁判");
        reasoning.setKeyPoints(Arrays.asList(
            "采用三段论推理方法",
            "事实认定以证据为基础",
            "法律适用严格依据法律条文"
        ));
        sections.add(reasoning);

        CaseAnalysisResponse.AnalysisSection law = new CaseAnalysisResponse.AnalysisSection();
        law.setId("law");
        law.setTitle("法律适用评析");
        law.setIcon("📖");
        law.setContent("本案的法律适用体现了以下特点：\n\n1. 实体法与程序法并重\n2. 注重法律条文的目的解释\n3. 参考同类案件的裁判尺度");
        law.setKeyPoints(Arrays.asList(
            "实体法与程序法并重",
            "目的解释方法",
            "参考同类裁判尺度"
        ));
        sections.add(law);

        CaseAnalysisResponse.AnalysisSection advice = new CaseAnalysisResponse.AnalysisSection();
        advice.setId("advice");
        advice.setTitle("应对策略建议");
        advice.setIcon("💡");
        advice.setContent("基于本案的裁判逻辑，建议：\n\n1. **证据准备**：围绕案件核心争议点充分举证\n2. **法律论证**：强化法律条文与事实的对应关系\n3. **程序合规**：确保诉讼程序合法合规");
        advice.setKeyPoints(Arrays.asList(
            "强化证据准备",
            "注重法律论证",
            "确保程序合规"
        ));
        advice.setLevel("MEDIUM");
        sections.add(advice);

        CaseAnalysisResponse.AnalysisSection risk = new CaseAnalysisResponse.AnalysisSection();
        risk.setId("risk");
        risk.setTitle("风险提示");
        risk.setIcon("⚠️");
        risk.setContent("本案涉及的主要风险点：\n\n1. **证据风险**：证据是否充分直接影响裁判结果\n2. **时效风险**：注意诉讼时效和除斥期间\n3. **执行风险**：胜诉后的实际执行存在不确定性");
        risk.setKeyPoints(Arrays.asList(
            "证据风险",
            "时效风险",
            "执行风险"
        ));
        risk.setLevel("MEDIUM");
        sections.add(risk);

        response.setSections(sections);

        response.setRelatedLaws(Arrays.asList(
            "《中华人民共和国民法典》相关条款",
            "《中华人民共和国民事诉讼法》相关条款",
            "最高人民法院相关司法解释"
        ));

        response.setRelatedCases(Arrays.asList(
            "（示例）同类型案件参考",
            "（示例）上级法院同类裁判"
        ));

        return response;
    }

    private String buildFactsContent(CaseSearchResponse.CaseSearchItem caseItem) {
        StringBuilder sb = new StringBuilder();
        sb.append("本案为").append(nullSafe(caseItem.getCaseType())).append("案件，");
        sb.append("案由为").append(nullSafe(caseItem.getCaseCause())).append("，");
        sb.append("由").append(nullSafe(caseItem.getCourt())).append("审理。\n\n");
        if (caseItem.getKeyFacts() != null && !caseItem.getKeyFacts().isEmpty()) {
            sb.append("**关键事实**：").append(caseItem.getKeyFacts()).append("\n");
        }
        if (caseItem.getSummary() != null && !caseItem.getSummary().isEmpty()) {
            sb.append("\n**案件摘要**：").append(caseItem.getSummary());
        }
        return sb.toString();
    }

    private String nullSafe(String s) {
        return s == null || s.isEmpty() ? "未知" : s;
    }
}
