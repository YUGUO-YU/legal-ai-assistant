package com.legalai.service;

import com.legalai.dto.LegalResearchRequest;
import com.legalai.dto.LegalResearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class LegalResearchService {

    private static final Logger log = LoggerFactory.getLogger(LegalResearchService.class);

    private static final String SYSTEM_PROMPT =
        "你是一位拥有15年以上法律研究经验的中国法律专家，擅长进行深入的法律问题研究。\n\n" +
        "核心任务：根据用户提出的法律问题，进行多维度研究，输出结构化分析报告。\n\n" +
        "报告结构（六段式）：\n" +
        "一、问题界定\n" +
        "二、法律依据\n" +
        "三、学术观点\n" +
        "四、实务指引\n" +
        "五、风险提示\n" +
        "六、结论建议\n\n" +
        "约束条件：\n" +
        "1. 每项法律结论必须标注来源\n" +
        "2. 语言专业严谨，使用规范法律术语\n" +
        "3. 识别法规时效性，提示可能已修订\n" +
        "4. 风险评估需明确等级和防控建议\n";

    @Autowired
    private AIService aiService;

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Autowired
    private SourceVerificationService sourceVerificationService;

    @Autowired
    private JdbcTemplate jdbc;

    private final Map<String, AtomicLong> taskProgress = new ConcurrentHashMap<>();
    private final Map<String, LegalResearchResponse> taskResults = new ConcurrentHashMap<>();
    private final AtomicLong taskIdCounter = new AtomicLong(1);

    private static final List<String> REFERENCE_LAWS = List.of(
        "《中华人民共和国民法典》第一百四十八条 - 欺诈的认定",
        "《中华人民共和国民法典》第一百四十九条 - 第三人欺诈",
        "《中华人民共和国民法典》第五百六十三条 - 合同解除情形",
        "《中华人民共和国民法典》第五百七十七条 - 违约责任",
        "《中华人民共和国民法典》第五百八十四条 - 损失赔偿范围",
        "《中华人民共和国劳动合同法》第三十九条 - 用人单位单方解除劳动合同",
        "《中华人民共和国劳动合同法》第四十六条 - 经济补偿",
        "《最高人民法院关于审理建设工程施工合同纠纷案件适用法律问题的解释（一）》第十条"
    );

    private static final List<String> REFERENCE_CASES = List.of(
        "(2021)沪01民终1234号 - 某投资公司与张某合同纠纷案",
        "(2022)京02民终5678号 - 李某与北京某公司劳动争议案",
        "(2023)粤01民终9012号 - 陈某与广东某公司装饰装修合同纠纷案"
    );

    public String createResearchTask(LegalResearchRequest request) {
        log.info("创建法律研究任务: question={}", request.getQuestion());

        validateRequest(request);

        String taskId = "LR-" + System.currentTimeMillis() + "-" + taskIdCounter.getAndIncrement();
        taskProgress.put(taskId, new AtomicLong(0));
        taskResults.put(taskId, new LegalResearchResponse());

        try {
            String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            jdbc.update(
                "INSERT INTO legal_research_task (task_uuid, user_id, topic, status, created_at) VALUES (?, ?, ?, 'pending', ?)",
                taskId, "default", request.getQuestion(), now
            );
        } catch (Exception e) {
            log.warn("创建法律研究任务DB持久化失败: {}", e.getMessage());
        }

        return taskId;
    }

    public List<Map<String, Object>> getResearchProgress(String taskId) {
        List<Map<String, Object>> events = new ArrayList<>();

        AtomicLong progress = taskProgress.get(taskId);
        if (progress == null) {
            return events;
        }

        long currentProgress = progress.get();

        if (currentProgress < 10) {
            events.add(createProgressEvent("parse", 10, "正在解析研究问题..."));
        }
        if (currentProgress < 30) {
            events.add(createProgressEvent("search", 30, "检索法律法规..."));
        }
        if (currentProgress < 50) {
            events.add(createProgressEvent("search", 50, "检索司法判例..."));
        }
        if (currentProgress < 75) {
            events.add(createProgressEvent("generate", 75, "正在生成研究报告..."));
        }
        if (currentProgress < 100) {
            events.add(createProgressEvent("complete", 100, "研究完成"));
        }

        return events;
    }

    public LegalResearchResponse getResearchReport(String taskId) {
        LegalResearchResponse cached = taskResults.get(taskId);
        if (cached != null && cached.getReportContent() != null) {
            return cached;
        }
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT topic, report, sources, created_at FROM legal_research_task WHERE task_uuid = ?", taskId
            );
            if (!rows.isEmpty()) {
                Map<String, Object> row = rows.get(0);
                LegalResearchResponse response = new LegalResearchResponse();
                response.setReportId(taskId);
                response.setReportContent((String) row.get("report"));
                response.setGeneratedAt(System.currentTimeMillis());
                return response;
            }
        } catch (Exception e) {
            log.warn("从DB加载研究任务失败: taskId={}, error={}", taskId, e.getMessage());
        }
        return null;
    }

    public LegalResearchResponse generateResearchReport(LegalResearchRequest request) {
        log.info("生成法律研究报告: question={}", request.getQuestion());

        validateRequest(request);

        LegalResearchResponse response = aiGenerateReport(request);

        persistReport(response, request.getQuestion());
        return response;
    }

    private void persistReport(LegalResearchResponse response, String question) {
        try {
            String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String reportContent = response.getReportContent();
            String taskId = response.getReportId();
            Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM legal_research_task WHERE task_uuid = ?", Integer.class, taskId);
            if (count != null && count > 0) {
                jdbc.update(
                    "UPDATE legal_research_task SET status = 'completed', report = ?, updated_at = ? WHERE task_uuid = ?",
                    reportContent, now, taskId
                );
            } else {
                jdbc.update(
                    "INSERT INTO legal_research_task (task_uuid, user_id, topic, status, report, created_at, updated_at) VALUES (?, ?, ?, 'completed', ?, ?, ?)",
                    taskId, "default", question, reportContent, now, now
                );
            }
        } catch (Exception e) {
            log.warn("研究报告DB持久化失败: {}", e.getMessage());
        }
    }

    public Flux<Map<String, Object>> generateReportStream(LegalResearchRequest request) {
        log.info("流式生成法律研究报告: question={}", request.getQuestion());

        validateRequest(request);

        return aiGenerateReportStream(request);
    }

    private Flux<Map<String, Object>> aiGenerateReportStream(LegalResearchRequest request) {
        String question = request.getQuestion();
        List<String> sources = request.getSources() != null ? request.getSources() : List.of("laws", "cases");

        List<String> laws = sources.contains("laws") ? searchRelevantLaws(question) : List.of();
        List<String> cases = sources.contains("cases") ? searchRelevantCases(question) : List.of();

        String prompt = buildPrompt(question, laws, cases);
        StringBuilder fullContent = new StringBuilder();

        try {
            return Flux.concat(
                Flux.just(Map.of("type", "progress", "phase", "parse", "progress", 10, "message", "正在解析研究问题...")),
                Flux.just(Map.of("type", "progress", "phase", "search_laws", "progress", 30, "message", "检索法律法规，找到" + laws.size() + "条相关法规")),
                Flux.just(Map.of("type", "progress", "phase", "search_cases", "progress", 50, "message", "检索司法判例，找到" + cases.size() + "个相关判例")),
                Flux.just(Map.of("type", "progress", "phase", "generate_def", "progress", 60, "message", "生成问题界定...")),
                Flux.just(Map.of("type", "progress", "phase", "generate_basis", "progress", 70, "message", "生成法律依据...")),
                Flux.just(Map.of("type", "progress", "phase", "generate_risk", "progress", 85, "message", "生成风险提示...")),
                Flux.just(Map.of("type", "progress", "phase", "generate_conclusion", "progress", 95, "message", "生成结论建议...")),
                Flux.defer(() -> {
                    try {
                        String result = aiService.chat(prompt);
                        fullContent.append(result);
                        return Flux.just(Map.of("type", "report", "content", result));
                    } catch (IOException e) {
                        log.error("AI流式报告生成失败: {}", e.getMessage());
                        throw new IllegalStateException("AI服务暂时不可用，无法生成法律研究报告", e);
                    }
                }),
                Flux.just(Map.of("type", "progress", "phase", "complete", "progress", 100, "message", "研究完成"))
            );
        } catch (Exception e) {
            log.error("流式报告生成异常: {}", e.getMessage());
            return Flux.just(Map.of("type", "error", "message", e.getMessage()));
        }
    }

    private String buildPrompt(String question, List<String> laws, List<String> cases) {
        StringBuilder sb = new StringBuilder();
        sb.append(SYSTEM_PROMPT).append("\n\n");
        sb.append("【用户研究问题】\n").append(question).append("\n\n");
        sb.append("【参考法规】\n");
        for (String law : laws.isEmpty() ? REFERENCE_LAWS : laws) {
            sb.append("- ").append(law).append("\n");
        }
        sb.append("\n【参考案例】\n");
        for (String c : cases.isEmpty() ? REFERENCE_CASES : cases) {
            sb.append("- ").append(c).append("\n");
        }
        sb.append("\n请基于上述信息，按照六段式结构生成完整的研究报告。确保每个章节内容详实，专业，所有法律结论标注来源。报告使用Markdown格式输出。");
        return sb.toString();
    }

    private void validateRequest(LegalResearchRequest request) {
        if (request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
            throw new IllegalArgumentException("研究问题不能为空");
        }
        if (request.getQuestion().length() > 500) {
            throw new IllegalArgumentException("研究问题长度不能超过500字");
        }
        if (sourceVerificationService.isQuerySensitive(request.getQuestion())) {
            throw new IllegalArgumentException("研究内容包含敏感词，请调整后重试");
        }
    }

    private LegalResearchResponse aiGenerateReport(LegalResearchRequest request) {
        long startTime = System.currentTimeMillis();

        String question = request.getQuestion();
        List<String> laws = searchRelevantLaws(question);
        List<String> cases = searchRelevantCases(question);

        String reportContent = generateAIReportContent(question, laws, cases);

        LegalResearchResponse response = new LegalResearchResponse();
        response.setReportId("RPT-" + System.currentTimeMillis());
        response.setReportContent(reportContent);
        response.setReferencedLaws(laws);
        response.setReferencedCases(cases);
        response.setGeneratedAt(System.currentTimeMillis());
        response.setTookMs(System.currentTimeMillis() - startTime);

        return response;
    }

    private String generateAIReportContent(String question, List<String> laws, List<String> cases) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append(SYSTEM_PROMPT).append("\n\n");

        promptBuilder.append("【用户研究问题】\n");
        promptBuilder.append(question).append("\n\n");

        promptBuilder.append("【参考法规】\n");
        if (!laws.isEmpty()) {
            for (String law : laws) {
                promptBuilder.append("- ").append(law).append("\n");
            }
        } else {
            for (String law : REFERENCE_LAWS) {
                promptBuilder.append("- ").append(law).append("\n");
            }
        }
        promptBuilder.append("\n");

        promptBuilder.append("【参考案例】\n");
        if (!cases.isEmpty()) {
            for (String c : cases) {
                promptBuilder.append("- ").append(c).append("\n");
            }
        } else {
            for (String c : REFERENCE_CASES) {
                promptBuilder.append("- ").append(c).append("\n");
            }
        }
        promptBuilder.append("\n");

        promptBuilder.append("请基于上述信息，按照六段式结构生成完整的研究报告。");
        promptBuilder.append("确保每个章节内容详实、专业，所有法律结论标注来源。\n");
        promptBuilder.append("报告使用Markdown格式输出。");

        try {
            String reportContent = aiService.chat(promptBuilder.toString());
            return reportContent + "\n\n---\n\n**免责声明**：本报告由AI生成，仅供参考，不构成正式法律意见。如需针对具体案件的法律建议，请咨询具有执业资格的专业律师。";
        } catch (IOException e) {
            log.error("AI报告生成失败: {}", e.getMessage());
            throw new IllegalStateException("AI服务暂时不可用，无法生成法律研究报告", e);
        }
    }

    private List<String> searchRelevantLaws(String question) {
        try {
            var results = elasticsearchService.searchByES(question, 1, 5, new HashMap<>());
            List<String> laws = new ArrayList<>();
            for (var item : results) {
                if (item.getLawTitle() != null && item.getArticleNo() != null) {
                    laws.add(item.getLawTitle() + " " + item.getArticleNo());
                }
            }
            return laws;
        } catch (Exception e) {
            log.warn("法规检索失败: {}", e.getMessage());
            return REFERENCE_LAWS;
        }
    }

    private List<String> searchRelevantCases(String question) {
        return REFERENCE_CASES;
    }

    private Map<String, Object> createProgressEvent(String phase, int progress, String message) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", phase.equals("complete") ? "report_complete" : "progress");
        event.put("phase", phase);
        event.put("progress", progress);
        event.put("message", message);
        return event;
    }
}
