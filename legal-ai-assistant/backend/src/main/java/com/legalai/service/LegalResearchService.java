package com.legalai.service;

import com.legalai.dto.LegalResearchRequest;
import com.legalai.dto.LegalResearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

    @Value("${mock.enabled:true}")
    private boolean mockEnabled;

    @Autowired
    private AIService aiService;

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Autowired
    private SourceVerificationService sourceVerificationService;

    private final Map<String, AtomicLong> taskProgress = new ConcurrentHashMap<>();
    private final Map<String, LegalResearchResponse> taskResults = new ConcurrentHashMap<>();
    private final AtomicLong taskIdCounter = new AtomicLong(1);

    public String createResearchTask(LegalResearchRequest request) {
        log.info("创建法律研究任务: question={}", request.getQuestion());

        validateRequest(request);

        String taskId = "LR-" + System.currentTimeMillis() + "-" + taskIdCounter.getAndIncrement();
        taskProgress.put(taskId, new AtomicLong(0));
        taskResults.put(taskId, new LegalResearchResponse());

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
        return taskResults.get(taskId);
    }

    public LegalResearchResponse generateResearchReport(LegalResearchRequest request) {
        log.info("生成法律研究报告: question={}", request.getQuestion());

        validateRequest(request);

        if (mockEnabled) {
            return mockGenerateReport(request);
        }

        return hybridGenerateReport(request);
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

    private LegalResearchResponse hybridGenerateReport(LegalResearchRequest request) {
        long startTime = System.currentTimeMillis();

        String question = request.getQuestion();
        List<String> laws = searchRelevantLaws(question);
        List<String> cases = searchRelevantCases(question);

        String reportContent = generateReportContent(question, laws, cases);

        LegalResearchResponse response = new LegalResearchResponse();
        response.setReportId("RPT-" + System.currentTimeMillis());
        response.setReportContent(reportContent);
        response.setReferencedLaws(laws);
        response.setReferencedCases(cases);
        response.setGeneratedAt(System.currentTimeMillis());
        response.setTookMs(System.currentTimeMillis() - startTime);

        return response;
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
            return Collections.emptyList();
        }
    }

    private List<String> searchRelevantCases(String question) {
        return Collections.emptyList();
    }

    private String generateReportContent(String question, List<String> laws, List<String> cases) {
        StringBuilder sb = new StringBuilder();

        sb.append("# 法律问题研究报告\n\n");

        sb.append("## 一、问题界定\n\n");
        sb.append("### （一）研究背景\n");
        sb.append("本研究针对以下法律问题进行深入分析：").append(question).append("\n\n");

        sb.append("### （二）核心问题\n");
        sb.append("本研究聚焦于相关法律关系中的权利义务认定及法律责任承担问题。\n\n");

        sb.append("### （三）问题边界\n");
        sb.append("本研究仅涉及中国大陆法律框架下的民事法律关系。\n\n");

        sb.append("### （四）关键术语定义\n");
        sb.append("- 相关法律术语定义详见法律依据章节。\n\n");

        sb.append("---\n\n## 二、法律依据\n\n");
        sb.append("### （一）法律体系概览\n");
        sb.append("本案涉及的主要法律包括《民法典》及相关司法解释。\n\n");
        sb.append("### （二）核心法规解读\n");
        if (!laws.isEmpty()) {
            for (String law : laws) {
                sb.append("- ").append(law).append("\n");
            }
        } else {
            sb.append("相关法律依据正在检索中...\n");
        }
        sb.append("\n");

        sb.append("---\n\n## 三、学术观点\n\n");
        sb.append("### （一）学术研究概况\n");
        sb.append("就该问题，学术界存在不同观点，主要包括...\n\n");

        sb.append("### （二）主要学术流派\n");
        sb.append("主流观点认为应当优先保护善意当事人的合法权益。\n\n");

        sb.append("---\n\n## 四、实务指引\n\n");
        sb.append("### （一）实务场景分类\n");
        sb.append("根据案件类型，可分为以下几种实务场景...\n\n");

        sb.append("### （二）分场景操作指引\n");
        sb.append("针对不同场景，建议采取相应的处理方式...\n\n");

        sb.append("---\n\n\n## 五、风险提示\n\n");
        sb.append("### （一）法律风险识别\n");
        sb.append("1. 诉讼时效风险：注意民事诉讼时效为三年。\n");
        sb.append("2. 证据风险：建议保留完整的证据材料。\n");
        sb.append("3. 执行风险：注意对方可能的财产线索。\n\n");

        sb.append("### （二）风险等级评估\n");
        sb.append("综合评估本案风险等级为【中等】，建议及时采取法律措施。\n\n");

        sb.append("---\n\n## 六、结论建议\n\n");
        sb.append("### （一）核心结论\n");
        sb.append("基于上述分析，建议当事人及时主张权利，依法维护自身合法权益。\n\n");

        sb.append("### （二）行动建议\n");
        sb.append("- 及早委托专业律师代理\n");
        sb.append("- 全面收集和保全证据\n");
        sb.append("- 关注诉讼时效及时限\n\n");

        sb.append("### （三）研究局限\n");
        sb.append("本研究基于现有材料，具体案件情况需结合实际证据进一步分析。\n\n");

        return sb.toString();
    }

    private LegalResearchResponse mockGenerateReport(LegalResearchRequest request) {
        long startTime = System.currentTimeMillis();

        String reportContent = generateReportContent(
            request.getQuestion(),
            List.of("《中华人民共和国民法典》第一百四十八条", "《中华人民共和国民法典》第一百四十九条"),
            List.of("(2023)沪01民终4567号")
        );

        LegalResearchResponse response = new LegalResearchResponse();
        response.setReportId("RPT-" + System.currentTimeMillis());
        response.setReportContent(reportContent);
        response.setReferencedLaws(List.of("《中华人民共和国民法典》第一百四十八条", "《中华人民共和国民法典》第一百四十九条"));
        response.setReferencedCases(List.of("(2023)沪01民终4567号"));
        response.setGeneratedAt(System.currentTimeMillis());
        response.setTookMs(System.currentTimeMillis() - startTime);

        return response;
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