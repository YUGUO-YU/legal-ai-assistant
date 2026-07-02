package com.legalai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalai.dto.*;
import com.legalai.llm.LLMClient;
import com.legalai.util.IdGenerator;
import com.legalai.util.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DocumentService {
    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final Map<String, DocumentTemplate> TEMPLATES = new HashMap<>();

    static {
        TEMPLATES.put("civil_petition", new DocumentTemplate(
            "民事起诉状",
            "民事诉讼",
            true,
            Arrays.asList("plaintiffName", "defendantName", "claimAmount", "facts"),
            Arrays.asList("《中华人民共和国民事诉讼法》第一百二十二条"),
            "civil_petition"
        ));

        TEMPLATES.put("civil_defense", new DocumentTemplate(
            "民事答辩状",
            "民事诉讼",
            false,
            Arrays.asList("defendantName", "plaintiffName", "facts"),
            Arrays.asList("《中华人民共和国民事诉讼法》第一百二十八条"),
            "civil_defense"
        ));

        TEMPLATES.put("civil_appeal", new DocumentTemplate(
            "民事上诉状",
            "民事诉讼",
            false,
            Arrays.asList("appellantName", "appelleeName", "originalJudgment", "appealReasons"),
            Arrays.asList("《中华人民共和国民事诉讼法》第一百七十条"),
            "civil_appeal"
        ));

        TEMPLATES.put("civil_property_preservation", new DocumentTemplate(
            "财产保全申请书",
            "民事诉讼",
            true,
            Arrays.asList("applicantName", "respondentName", "preservationAmount", "preservationReason"),
            Arrays.asList("《中华人民共和国民事诉讼法》第一百条"),
            "civil_property_preservation"
        ));

        TEMPLATES.put("civil_execution", new DocumentTemplate(
            "强制执行申请书",
            "民事诉讼",
            true,
            Arrays.asList("applicantName", "respondentName", "executionAmount", "originalJudgment"),
            Arrays.asList("《中华人民共和国民事诉讼法》第二百三十六条"),
            "civil_execution"
        ));

        TEMPLATES.put("labor_contract", new DocumentTemplate(
            "劳动合同",
            "劳动人事",
            true,
            Arrays.asList("employerName", "employeeName", "workContent", "salary", "startDate"),
            Arrays.asList("《中华人民共和国劳动合同法》第十条"),
            "labor_contract"
        ));

        TEMPLATES.put("labor_confidentiality", new DocumentTemplate(
            "保密协议",
            "劳动人事",
            true,
            Arrays.asList("companyName", "employeeName", "confidentialPeriod", "breachPenalty"),
            Arrays.asList("《中华人民共和国劳动合同法》第二十三条"),
            "labor_confidentiality"
        ));

        TEMPLATES.put("labor_non_compete", new DocumentTemplate(
            "竞业限制协议",
            "劳动人事",
            true,
            Arrays.asList("companyName", "employeeName", "nonCompetePeriod", "compensation", "breachPenalty"),
            Arrays.asList("《中华人民共和国劳动合同法》第二十三条"),
            "labor_non_compete"
        ));

        TEMPLATES.put("labor_termination", new DocumentTemplate(
            "解除劳动合同协议",
            "劳动人事",
            true,
            Arrays.asList("employerName", "employeeName", "terminationReason", "compensation"),
            Arrays.asList("《中华人民共和国劳动合同法》第三十七条"),
            "labor_termination"
        ));

        TEMPLATES.put("labor_arbitration", new DocumentTemplate(
            "劳动仲裁申请书",
            "劳动人事",
            true,
            Arrays.asList("applicantName", "respondentName", "disputeType", "claimAmount"),
            Arrays.asList("《中华人民共和国劳动争议调解仲裁法》第二十八条"),
            "labor_arbitration"
        ));

        TEMPLATES.put("business_lawyer_letter", new DocumentTemplate(
            "律师函",
            "商业函件",
            true,
            Arrays.asList("senderName", "recipientName", "factDescription", "legalBasis"),
            Arrays.asList("《中华人民共和国民法典》第五百七十七条"),
            "business_lawyer_letter"
        ));

        TEMPLATES.put("business_ceo_letter", new DocumentTemplate(
            "CEO函",
            "商业函件",
            true,
            Arrays.asList("senderName", "recipientName", "factDescription", "legalBasis"),
            Arrays.asList("《中华人民共和国民法典》第五百七十七条"),
            "business_ceo_letter"
        ));

        TEMPLATES.put("business_contract_termination", new DocumentTemplate(
            "合同解除通知函",
            "商业函件",
            true,
            Arrays.asList("senderName", "recipientName", "contractNo", "terminationReason"),
            Arrays.asList("《中华人民共和国民法典》第五百六十五条"),
            "business_contract_termination"
        ));

        TEMPLATES.put("business_payment_demand", new DocumentTemplate(
            "催款函",
            "商业函件",
            true,
            Arrays.asList("creditorName", "debtorName", "amount", "overdueDays"),
            Arrays.asList("《中华人民共和国民法典》第六百七十六条"),
            "business_payment_demand"
        ));

        TEMPLATES.put("business_legal_opinion_request", new DocumentTemplate(
            "法律意见书请求函",
            "商业函件",
            true,
            Arrays.asList("senderName", "recipientName", "matterDescription", "deadline"),
            Arrays.asList("《中华人民共和国民法典》第五百三十五条"),
            "business_legal_opinion_request"
        ));

        TEMPLATES.put("ip_trademark_license", new DocumentTemplate(
            "商标许可合同",
            "知识产权",
            true,
            Arrays.asList("licensorName", "licenseeName", "trademarkName", "licensePeriod"),
            Arrays.asList("《中华人民共和国商标法》第四十条"),
            "ip_trademark_license"
        ));

        TEMPLATES.put("ip_software_license", new DocumentTemplate(
            "软件许可协议",
            "知识产权",
            true,
            Arrays.asList("licensorName", "licenseeName", "softwareName", "licenseScope"),
            Arrays.asList("《中华人民共和国著作权法》第二十六条"),
            "ip_software_license"
        ));

        TEMPLATES.put("ip_confidentiality", new DocumentTemplate(
            "商业秘密保密协议",
            "知识产权",
            true,
            Arrays.asList("companyName", "employeeName", "confidentialScope", "breachPenalty"),
            Arrays.asList("《中华人民共和国反不正当竞争法》第九条"),
            "ip_confidentiality"
        ));

        TEMPLATES.put("common_power_of_attorney", new DocumentTemplate(
            "授权委托书",
            "民商事通用",
            true,
            Arrays.asList("principalName", "agentName", "agentIdCard", "delegationScope"),
            Arrays.asList("《中华人民共和国民事诉讼法》第五十九条"),
            "common_power_of_attorney"
        ));
    }

    @Autowired(required = false)
    private LLMClient llmClient;

    @Value("${mock.enabled:true}")
    private boolean mockEnabled;

    public DocumentDraftResponse draftDocument(DocumentDraftRequest request) {
        log.info("文书起草请求: templateCode={}, mock={}", request.getTemplateCode(), mockEnabled);

        validateRequest(request);

        DocumentTemplate template = TEMPLATES.get(request.getTemplateCode());
        if (template == null) {
            throw new IllegalArgumentException("不支持的文书模板: " + request.getTemplateCode());
        }

        String content;
        String contentSource;
        String localContent = generateDocumentContent(template, request);
        
        if (mockEnabled || llmClient == null) {
            content = localContent;
            contentSource = "本地模板生成";
        } else {
            try {
                String enhancedContent = enhanceDocumentWithAI(localContent, template, request);
                if (enhancedContent != null && !enhancedContent.trim().isEmpty()) {
                    content = enhancedContent;
                    contentSource = "AI优化增强";
                } else {
                    content = localContent;
                    contentSource = "本地模板生成";
                }
            } catch (Exception e) {
                log.warn("[Document] AI优化失败，使用本地模板: {}", e.getMessage());
                content = localContent;
                contentSource = "本地模板生成";
            }
        }

        String riskPrompt = generateRiskPrompt(template, request);
        String disclaimer = generateDisclaimer(template, request.getCaseData());

        DocumentDraftResponse response = new DocumentDraftResponse();
        response.setDocumentContent(content);
        response.setRiskPrompt(riskPrompt);
        response.setDisclaimer(disclaimer);
        response.setReferencedLaws(template.referencedLaws);
        response.setContentSource(contentSource);

        return response;
    }

    private String generateDocumentContentByLLM(DocumentTemplate template, DocumentDraftRequest request) throws Exception {
        DocumentDraftRequest.DocumentData data = request.getCaseData();
        StringBuilder facts = new StringBuilder();
        if (data != null) {
            if (notEmpty(data.getPlaintiffName())) facts.append("原告：").append(data.getPlaintiffName()).append("\n");
            if (notEmpty(data.getPlaintiffAddress())) facts.append("原告地址：").append(data.getPlaintiffAddress()).append("\n");
            if (notEmpty(data.getDefendantName())) facts.append("被告：").append(data.getDefendantName()).append("\n");
            if (notEmpty(data.getDefendantAddress())) facts.append("被告地址：").append(data.getDefendantAddress()).append("\n");
            if (data.getClaimAmount() != null) facts.append("诉讼金额：").append(data.getClaimAmount()).append(" 元\n");
            if (notEmpty(data.getClaimDescription())) facts.append("诉讼请求：").append(data.getClaimDescription()).append("\n");
            if (data.getFacts() != null) {
                List<String> list = data.getFacts();
                if (list != null && !list.isEmpty()) facts.append("事实与理由：\n").append(String.join("\n", list)).append("\n");
            }
            if (notEmpty(data.getCourtName())) facts.append("管辖法院：").append(data.getCourtName()).append("\n");
            if (notEmpty(data.getDefendantCompany())) facts.append("被告单位：").append(data.getDefendantCompany()).append("\n");
        }

        String prompt = "你是一名资深中国律师。请根据以下案件信息起草一份「" + template.name + "」。\n"
            + "要求：\n"
            + "1. 严格使用中国大陆法律文书标准格式；\n"
            + "2. 完整包含当事人信息（姓名/地址/单位等）、诉讼请求、事实与理由、此致法院、具状人、日期；\n"
            + "3. 引用相关法条（参考：" + String.join("、", template.referencedLaws) + "）；\n"
            + "4. 语言严谨规范，体现专业法律素养；\n"
            + "5. 直接输出文书正文，不要任何解释或 Markdown 包裹。\n\n"
            + "案件信息：\n" + facts.toString();

        log.info("[Document] 调 LLM 起草 {} 文书，prompt 长度={}", template.name, prompt.length());
        return llmClient.chat(prompt);
    }

    private String enhanceDocumentWithAI(String localContent, DocumentTemplate template, DocumentDraftRequest request) throws Exception {
        DocumentDraftRequest.DocumentData data = request.getCaseData();
        StringBuilder caseInfo = new StringBuilder();
        if (data != null) {
            if (notEmpty(data.getPlaintiffName())) caseInfo.append("原告：").append(data.getPlaintiffName()).append("\n");
            if (notEmpty(data.getPlaintiffAddress())) caseInfo.append("原告地址：").append(data.getPlaintiffAddress()).append("\n");
            if (notEmpty(data.getDefendantName())) caseInfo.append("被告：").append(data.getDefendantName()).append("\n");
            if (notEmpty(data.getDefendantAddress())) caseInfo.append("被告地址：").append(data.getDefendantAddress()).append("\n");
            if (data.getClaimAmount() != null) caseInfo.append("诉讼金额：").append(data.getClaimAmount()).append(" 元\n");
            if (notEmpty(data.getClaimDescription())) caseInfo.append("诉讼请求：").append(data.getClaimDescription()).append("\n");
            if (data.getFacts() != null && !data.getFacts().isEmpty()) caseInfo.append("事实与理由：\n").append(String.join("\n", data.getFacts())).append("\n");
            if (notEmpty(data.getCourtName())) caseInfo.append("管辖法院：").append(data.getCourtName()).append("\n");
        }

        String prompt = "你是一名资深中国法律文书专家。请根据以下本地模板生成的文书和案件信息，对文书进行优化润色。\n\n"
            + "要求：\n"
            + "1. 保持文书基本结构和格式不变；\n"
            + "2. 优化语言表达，使其更加严谨规范；\n"
            + "3. 确保【】占位符被真实信息替换（如已提供）；\n"
            + "4. 引用相关法条（参考：" + String.join("、", template.referencedLaws) + "）；\n"
            + "5. 直接输出优化后的文书正文，不要任何解释或 Markdown 包裹。\n\n"
            + "案件信息：\n" + caseInfo.toString() + "\n\n"
            + "本地模板生成的内容：\n" + localContent;

        log.info("[Document] AI优化文书，prompt 长度={}", prompt.length());
        return llmClient.chat(prompt);
    }

    public List<TemplateInfo> getTemplates() {
        List<TemplateInfo> infos = new ArrayList<>();
        for (Map.Entry<String, DocumentTemplate> entry : TEMPLATES.entrySet()) {
            DocumentTemplate t = entry.getValue();
            TemplateInfo info = new TemplateInfo();
            info.setTemplateCode(entry.getKey());
            info.setTemplateName(t.name);
            info.setCategory(t.category);
            info.setPopular(t.popular);
            infos.add(info);
        }
        return infos;
    }

    public ExtractedInfo extractInfoFromText(String text, String templateCode) {
        log.info("提取信息: text长度={}, templateCode={}, mock={}", text == null ? 0 : text.length(), templateCode, mockEnabled);

        if (text == null || text.trim().isEmpty()) {
            ExtractedInfo empty = new ExtractedInfo();
            empty.setSuccess(false);
            empty.setDataSource("本地正则识别");
            empty.setErrorMessage("待提取的文本为空");
            return empty;
        }

        ExtractedInfo local = extractByRegex(text, templateCode);
        int localFilled = countFilledFields(local);
        log.info("[Document] 本地正则识别填充字段数={}", localFilled);

        if (localFilled >= 5 && hasCoreFields(local)) {
            local.setDataSource("本地正则识别");
            local.setSuccess(true);
            local.setErrorMessage(null);
            return local;
        }

        if (llmClient != null) {
            try {
                ExtractedInfo ai = extractByLLM(text, templateCode);
                int aiFilled = countFilledFields(ai);
                log.info("[Document] LLM 识别填充字段数={}", aiFilled);
                if (ai != null && aiFilled > 0) {
                    ExtractedInfo merged = mergeExtractedInfo(ai, local);
                    merged.setDataSource("AI 智能识别");
                    merged.setSuccess(true);
                    merged.setErrorMessage(null);
                    return merged;
                }
            } catch (Exception e) {
                log.warn("[Document] LLM 提取失败: {}", e.getMessage());
            }
        }

        if (!hasAnyField(local)) {
            local.setSuccess(false);
            if (local.getErrorMessage() == null || local.getErrorMessage().isEmpty()) {
                local.setErrorMessage("未能从文本中识别到任何关键信息，请补充当事人、金额或事实描述后重试");
            }
            local.setDataSource("本地正则识别");
        } else {
            local.setSuccess(true);
            local.setErrorMessage(null);
            local.setDataSource("本地正则识别");
        }
        return local;
    }

    private boolean hasCoreFields(ExtractedInfo info) {
        if (info == null) return false;
        int core = 0;
        if (notEmpty(info.getPlaintiffName())) core++;
        if (notEmpty(info.getDefendantName())) core++;
        if (info.getClaimAmount() != null && info.getClaimAmount().signum() > 0) core++;
        if (notEmpty(info.getFacts())) core++;
        if (notEmpty(info.getClaimDescription())) core++;
        return core >= 2;
    }

    private ExtractedInfo mergeExtractedInfo(ExtractedInfo primary, ExtractedInfo fallback) {
        ExtractedInfo merged = new ExtractedInfo();
        merged.setPlaintiffName(coalesce(primary.getPlaintiffName(), fallback.getPlaintiffName()));
        merged.setPlaintiffAddress(coalesce(primary.getPlaintiffAddress(), fallback.getPlaintiffAddress()));
        merged.setDefendantName(coalesce(primary.getDefendantName(), fallback.getDefendantName()));
        merged.setDefendantAddress(coalesce(primary.getDefendantAddress(), fallback.getDefendantAddress()));
        merged.setClaimAmount(primary.getClaimAmount() != null ? primary.getClaimAmount() : fallback.getClaimAmount());
        merged.setClaimDescription(coalesce(primary.getClaimDescription(), fallback.getClaimDescription()));
        merged.setFacts(coalesce(primary.getFacts(), fallback.getFacts()));
        merged.setCourtName(coalesce(primary.getCourtName(), fallback.getCourtName()));
        merged.setEmployerName(coalesce(primary.getEmployerName(), fallback.getEmployerName()));
        merged.setEmployeeName(coalesce(primary.getEmployeeName(), fallback.getEmployeeName()));
        merged.setWorkContent(coalesce(primary.getWorkContent(), fallback.getWorkContent()));
        merged.setSalary(coalesce(primary.getSalary(), fallback.getSalary()));
        merged.setStartDate(coalesce(primary.getStartDate(), fallback.getStartDate()));
        merged.setDisputeType(coalesce(primary.getDisputeType(), fallback.getDisputeType()));
        merged.setSuccess(true);
        merged.setErrorMessage(null);
        merged.setDataSource(primary.getDataSource() != null ? primary.getDataSource() : "AI 智能识别");
        return merged;
    }

    private String coalesce(String a, String b) {
        if (a == null || a.trim().isEmpty()) return b;
        return a;
    }

    private int countFilledFields(ExtractedInfo info) {
        if (info == null) return 0;
        int c = 0;
        if (notEmpty(info.getPlaintiffName())) c++;
        if (notEmpty(info.getPlaintiffAddress())) c++;
        if (notEmpty(info.getDefendantName())) c++;
        if (notEmpty(info.getDefendantAddress())) c++;
        if (info.getClaimAmount() != null && info.getClaimAmount().signum() > 0) c++;
        if (notEmpty(info.getClaimDescription())) c++;
        if (notEmpty(info.getFacts())) c++;
        if (notEmpty(info.getCourtName())) c++;
        if (notEmpty(info.getEmployerName())) c++;
        if (notEmpty(info.getEmployeeName())) c++;
        if (notEmpty(info.getWorkContent())) c++;
        if (notEmpty(info.getSalary())) c++;
        if (notEmpty(info.getStartDate())) c++;
        if (notEmpty(info.getDisputeType())) c++;
        return c;
    }

    private ExtractedInfo extractByLLM(String text, String templateCode) throws Exception {
        String prompt = "你是法律文本关键信息抽取助手。请从以下案件相关文本中抽取关键字段，以 JSON 格式返回。\n"
            + "JSON 字段（缺失填空字符串或 null）：\n"
            + "plaintiffName（原告/申请人姓名）\n"
            + "plaintiffAddress（原告地址）\n"
            + "defendantName（被告/被申请人姓名/单位名称）\n"
            + "defendantAddress（被告地址）\n"
            + "claimAmount（诉讼金额，数字，单位元，不要带\"元\"字符）\n"
            + "claimDescription（诉讼请求/仲裁请求描述）\n"
            + "facts（事实与理由概述）\n"
            + "courtName（管辖法院）\n"
            + "employerName（用人单位）\n"
            + "employeeName（劳动者/员工姓名）\n"
            + "workContent（工作岗位/工作内容）\n"
            + "salary（月薪/工资数字与单位）\n"
            + "startDate（入职/起始日期）\n"
            + "disputeType（争议类型）\n\n"
            + "仅输出 JSON，不要解释：\n\n"
            + "原文：\n" + text;
        if (templateCode != null && !templateCode.isEmpty()) {
            prompt += "\n\n参考文书类型：" + templateCode;
        }

        String raw = llmClient.chat(prompt);
        log.info("[Document] LLM 抽取原始返回长度={}", raw == null ? 0 : raw.length());
        if (raw == null) return null;

        String json = extractJsonBlock(raw);
        if (json == null) return null;

        JsonNode node = JSON.readTree(json);
        ExtractedInfo info = new ExtractedInfo();
        info.setPlaintiffName(textOrEmpty(node, "plaintiffName"));
        info.setPlaintiffAddress(textOrEmpty(node, "plaintiffAddress"));
        info.setDefendantName(textOrEmpty(node, "defendantName"));
        info.setDefendantAddress(textOrEmpty(node, "defendantAddress"));
        info.setClaimDescription(textOrEmpty(node, "claimDescription"));
        info.setFacts(textOrEmpty(node, "facts"));
        info.setCourtName(textOrEmpty(node, "courtName"));
        info.setEmployerName(textOrEmpty(node, "employerName"));
        info.setEmployeeName(textOrEmpty(node, "employeeName"));
        info.setWorkContent(textOrEmpty(node, "workContent"));
        info.setSalary(textOrEmpty(node, "salary"));
        info.setStartDate(textOrEmpty(node, "startDate"));
        info.setDisputeType(textOrEmpty(node, "disputeType"));
        String amt = textOrEmpty(node, "claimAmount");
        if (!amt.isEmpty()) {
            try {
                info.setClaimAmount(new BigDecimal(amt.replaceAll("[,\\s]", "")));
            } catch (Exception ignore) {
            }
        }
        return info;
    }

    private String extractJsonBlock(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return null;
    }

    private String textOrEmpty(JsonNode node, String field) {
        if (node == null) return "";
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) return "";
        String s = v.asText("").trim();
        return s;
    }

    private boolean notEmpty(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private boolean hasAnyField(ExtractedInfo info) {
        if (info == null) return false;
        return notEmpty(info.getPlaintiffName())
            || notEmpty(info.getPlaintiffAddress())
            || notEmpty(info.getDefendantName())
            || notEmpty(info.getDefendantAddress())
            || notEmpty(info.getClaimDescription())
            || notEmpty(info.getFacts())
            || notEmpty(info.getCourtName())
            || notEmpty(info.getEmployerName())
            || notEmpty(info.getEmployeeName())
            || notEmpty(info.getWorkContent())
            || notEmpty(info.getSalary())
            || notEmpty(info.getStartDate())
            || notEmpty(info.getDisputeType())
            || (info.getClaimAmount() != null && info.getClaimAmount().signum() > 0);
    }

    private ExtractedInfo extractByRegex(String text, String templateCode) {
        ExtractedInfo info = new ExtractedInfo();

        try {
            // 1. 原告姓名 - 多种格式支持
            String plaintiff = extractPartyName(text, "原告");
            if (plaintiff == null) plaintiff = extractPartyName(text, "申请人");
            if (plaintiff == null) plaintiff = extractPartyName(text, "申请执行人");
            if (plaintiff == null) plaintiff = extractPartyName(text, "委托代理人");
            info.setPlaintiffName(plaintiff);

            // 2. 被告姓名 - 多种格式支持
            String defendant = extractPartyName(text, "被告");
            if (defendant == null) defendant = extractPartyName(text, "被申请人");
            if (defendant == null) defendant = extractPartyName(text, "被执行人");
            if (defendant == null) defendant = extractPartyName(text, "被诉人");
            info.setDefendantName(defendant);

            // 3. 原告地址
            String plaintiffAddr = extractAddress(text, "原告");
            if (plaintiffAddr == null) plaintiffAddr = extractAddress(text, "申请人");
            info.setPlaintiffAddress(plaintiffAddr);

            // 4. 被告地址
            String defendantAddr = extractAddress(text, "被告");
            if (defendantAddr == null) defendantAddr = extractAddress(text, "被申请人");
            info.setDefendantAddress(defendantAddr);

            // 5. 法院名称
            String court = extractCourtName(text);
            info.setCourtName(court);

            // 6. 诉讼金额
            BigDecimal amount = parseAmount(text);
            if (amount != null) {
                info.setClaimAmount(amount);
            }

            // 7. 诉讼请求
            String claimDesc = extractClaimDescription(text);
            info.setClaimDescription(claimDesc);

            // 8. 事实与理由
            String facts = extractFacts(text);
            info.setFacts(facts);

            // 9. 用人单位（劳动纠纷）
            String employer = extractPartyName(text, "用人单位");
            if (employer != null) {
                employer = employer.replaceAll("(住所|地址|统一社会信用代码).*$", "").trim();
            }
            info.setEmployerName(employer);

            // 10. 劳动者
            String employee = extractPartyName(text, "劳动者");
            info.setEmployeeName(employee);

            // 11. 工作岗位
            String workContent = extractField(text, "工作岗位", 40);
            if (workContent == null) workContent = extractField(text, "从事", 40);
            info.setWorkContent(workContent);

            // 12. 工资
            String salary = extractField(text, "工资", 20);
            if (salary == null) salary = extractField(text, "月薪", 20);
            if (salary == null) salary = extractField(text, "月工资", 20);
            info.setSalary(salary);

            // 13. 入职日期
            String startDate = extractDate(text);
            info.setStartDate(startDate);

            // 14. 争议类型
            String dispute = extractDisputeType(text);
            info.setDisputeType(dispute);

        } catch (Exception e) {
            log.warn("本地正则提取异常: {}", e.getMessage());
        }

        return info;
    }

    private String extractPartyName(String text, String keyword) {
        if (text == null || keyword == null) return null;
        
        // 找到关键字位置
        int idx = text.indexOf(keyword);
        if (idx < 0) return null;
        
        // 关键字后一段文字（到换行或足够长）
        int start = idx + keyword.length();
        int end = Math.min(start + 80, text.length());
        String segment = text.substring(start, end);
        
        // 移除常见前缀词
        segment = segment.replaceAll("^[：:,，\\s为叫字]+", "");
        
        // 匹配姓名或单位名
        // 中文姓名：2-4个汉字+可选的·+2-4个汉字
        Pattern p = Pattern.compile("^[\\u4e00-\\u9fa5]{2,4}(?:[·•][\\u4e00-\\u9fa5]{2,4})?");
        Matcher m = p.matcher(segment);
        if (m.find()) {
            String name = m.group().trim();
            // 去掉常见后缀
            name = name.replaceAll("(住所|地址|住|男|女|族|身份证号|住址|出生|汉族|公司|有限|有限公司| Corporation).*$", "");
            if (name.length() >= 2) return name;
        }
        
        // 匹配公司名：XX有限公司 / XX公司 / XX集团
        p = Pattern.compile("^[\\u4e00-\\u9fa5A-Za-z0-9（）()\\-·]{4,50}(?:有限公司|公司|集团|企业|合作社|协会)?
                (?:住所|地址|注册地)?");
        m = p.matcher(segment);
        if (m.find()) {
            String name = m.group().trim();
            name = name.replaceAll("(住所|地址|住|注册地|统一社会信用代码).*$", "");
            if (name.length() >= 4) return name;
        }
        
        return null;
    }

    private String extractAddress(String text, String keyword) {
        if (text == null || keyword == null) return null;
        
        int idx = text.indexOf(keyword);
        if (idx < 0) return null;
        
        int start = idx + keyword.length();
        int end = Math.min(start + 150, text.length());
        String segment = text.substring(start, end);
        
        // 移除前缀标点和空格
        segment = segment.replaceAll("^[：:,，\\s为住]+", "");
        
        // 找到第一个换行，截取第一行作为地址
        int lineEnd = segment.indexOf('\n');
        if (lineEnd > 0 && lineEnd < 120) {
            segment = segment.substring(0, lineEnd);
        }
        
        // 截取到常见分隔符
        int commaEnd = segment.indexOf('，');
        if (commaEnd > 10 && commaEnd < 100) {
            segment = segment.substring(0, commaEnd);
        }
        
        // 截取到句号
        int periodEnd = segment.indexOf('。');
        if (periodEnd > 10 && periodEnd < 100) {
            segment = segment.substring(0, periodEnd);
        }
        
        segment = segment.trim();
        
        // 验证是否是有效地址（包含省/市/区/县/路/街/道/镇/乡/号等）
        if (segment.length() >= 6 && (
            segment.contains("省") || segment.contains("市") || segment.contains("区") ||
            segment.contains("县") || segment.contains("路") || segment.contains("街") ||
            segment.contains("道") || segment.contains("镇") || segment.contains("乡") ||
            segment.contains("号") || segment.contains("栋") || segment.contains("楼") ||
            segment.contains("室") || segment.contains("弄")
        )) {
            return segment;
        }
        
        // 如果不包含地址关键词但长度合适也返回
        if (segment.length() >= 8 && segment.length() <= 120) {
            return segment;
        }
        
        return null;
    }

    private String extractCourtName(String text) {
        if (text == null) return null;
        
        // 优先匹配"XX市XX人民法院"格式
        Pattern p = Pattern.compile("([\\u4e00-\\u9fa5]{2,6}人民法院)");
        Matcher m = p.matcher(text);
        if (m.find()) {
            return m.group(1);
        }
        
        // 匹配"XX市XX法院"格式
        p = Pattern.compile("([\\u4e00-\\u9fa5]{2,8}法院)");
        m = p.matcher(text);
        if (m.find()) {
            return m.group(1);
        }
        
        // 匹配"管辖法院：XXX"格式
        p = Pattern.compile("(?:管辖法院|受案法院|起诉至|向|移送至)[\\s：:]*([\\u4e00-\\u9fa5A-Za-z]{2,15}(?:人民法院|法院))");
        m = p.matcher(text);
        if (m.find()) {
            return m.group(1);
        }
        
        return null;
    }

    private String extractClaimDescription(String text) {
        if (text == null) return null;
        
        // 匹配"诉讼请求："或"请求事项："后的内容
        Pattern p = Pattern.compile("(?:诉讼请求|请求事项|仲裁请求|索赔|请求)[：:][\\s\\S]{1,2000}(?=(?:事实|理由|证据|此致|$))");
        Matcher m = p.matcher(text);
        if (m.find()) {
            String result = m.group();
            result = result.replaceAll("^(?:诉讼请求|请求事项|仲裁请求|索赔|请求)[：:]", "").trim();
            if (result.length() > 0) return result;
        }
        
        // 匹配"1. xxx"格式的列表
        p = Pattern.compile("(?:1[、.．][\\s\\S]{5,300}){1,5}");
        m = p.matcher(text);
        if (m.find()) {
            return m.group().trim();
        }
        
        // 匹配"一、xxx"格式
        p = Pattern.compile("(?:一[、.．][\\s\\S]{5,300}){1,5}");
        m = p.matcher(text);
        if (m.find()) {
            return m.group().trim();
        }
        
        return null;
    }

    private String extractFacts(String text) {
        if (text == null) return null;
        
        // 匹配"事实与理由："或"事实："后的内容
        Pattern p = Pattern.compile("(?:事实(?:与理由)?|理由|案情|案件事实)[：:][\\s\\S]{10,5000}(?=(?:此致|具状|落款|申请人|$))");
        Matcher m = p.matcher(text);
        if (m.find()) {
            String result = m.group();
            result = result.replaceAll("^(?:事实(?:与理由)?|理由|案情|案件事实)[：:]", "").trim();
            if (result.length() > 0) return result;
        }
        
        return null;
    }

    private String extractField(String text, String keyword, int maxLen) {
        if (text == null || keyword == null) return null;
        
        int idx = text.indexOf(keyword);
        if (idx < 0) return null;
        
        int start = idx + keyword.length();
        int end = Math.min(start + maxLen, text.length());
        String segment = text.substring(start, end);
        
        // 移除前缀
        segment = segment.replaceAll("^[：:,，\\s为是]+", "");
        
        // 截取到换行或足够长度
        int lineEnd = segment.indexOf('\n');
        if (lineEnd > 0) {
            segment = segment.substring(0, lineEnd);
        }
        
        segment = segment.trim();
        if (segment.length() >= 2) {
            return segment;
        }
        
        return null;
    }

    private String extractDate(String text) {
        if (text == null) return null;
        
        // 多种日期格式
        String[] datePatterns = {
            "(\\d{4}年\\d{1,2}月\\d{1,2}日?)",
            "(\\d{4}[年\\-/.]\\d{1,2}[月\\-/.]\\d{1,2}[日]?)",
            "(\\d{8})"
        };
        
        for (String p : datePatterns) {
            Pattern pattern = Pattern.compile(p);
            Matcher m = pattern.matcher(text);
            if (m.find()) {
                String date = m.group(1);
                date = date.replaceAll("\\s+", "");
                return date;
            }
        }
        
        return null;
    }

    private String extractDisputeType(String text) {
        if (text == null) return null;
        
        String[] disputes = {
            "劳动争议", "工伤赔偿", "工资争议", "劳动合同纠纷", "社会保险纠纷",
            "经济补偿金纠纷", "违法解除劳动合同", "民事纠纷", "合同纠纷",
            "房屋租赁纠纷", "借款合同纠纷", "买卖合同纠纷", "租赁合同纠纷",
            "服务合同纠纷", "委托合同纠纷", "赠与合同纠纷", "承揽合同纠纷",
            "建设工程合同纠纷", "运输合同纠纷", "技术合同纠纷", "知识产权纠纷"
        };
        
        for (String dispute : disputes) {
            if (text.contains(dispute)) {
                return dispute;
            }
        }
        
        return null;
    }

    private String firstMatch(String text, String... patterns) {
        if (text == null) return null;
        for (String p : patterns) {
            if (p == null) continue;
            try {
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(p);
                java.util.regex.Matcher m = pattern.matcher(text);
                if (m.find()) {
                    String hit = m.group(1);
                    if (hit != null) return hit;
                }
            } catch (Exception ignore) {
            }
        }
        return null;
    }

    private BigDecimal parseAmount(String text) {
        if (text == null || text.isEmpty()) return null;
        String[] patterns = new String[] {
            "(?:金额|诉请金额|诉讼请求金额|标的额|欠款|借款|本金|货款|赔偿金额)[为于约]?\\s*[人民币]?\\s*([\\d,，\\.]{1,15})(?:\\s*元)?",
            "([\\d,，\\.]{1,15})\\s*万元",
            "([\\d,，\\.]{1,15})\\s*元"
        };
        for (String p : patterns) {
            try {
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(p);
                java.util.regex.Matcher m = pattern.matcher(text);
                if (m.find()) {
                    String num = m.group(1);
                    if (num == null) continue;
                    num = num.replaceAll("[,，\\s]", "");
                    if (num.isEmpty()) continue;
                    BigDecimal bd = new BigDecimal(num);
                    if (p.contains("万元")) {
                        bd = bd.multiply(new BigDecimal("10000"));
                    }
                    if (bd.signum() > 0) return bd;
                }
            } catch (Exception ignore) {
            }
        }
        return null;
    }

    private String cleanName(String s) {
        if (s == null) return "";
        s = s.replaceAll("[\\s　]+", "").trim();
        if (s.length() > 30) s = s.substring(0, 30);
        return s;
    }

    private String cleanAddress(String s) {
        if (s == null) return "";
        s = s.replaceAll("\\s+", " ").trim();
        if (s.length() > 100) s = s.substring(0, 100);
        return s;
    }

    private String cleanCourt(String s) {
        if (s == null) return "";
        s = s.replaceAll("\\s+", "").trim();
        if (s.length() > 40) s = s.substring(0, 40);
        return s;
    }

    private void validateRequest(DocumentDraftRequest request) {
        if (request.getTemplateCode() == null || request.getTemplateCode().isEmpty()) {
            throw new IllegalArgumentException("模板代码不能为空");
        }
    }

    private String generateDocumentContent(DocumentTemplate template, DocumentDraftRequest request) {
        StringBuilder sb = new StringBuilder();

        switch (template.code) {
            case "civil_petition" -> sb.append(generateCivilPetition(request));
            case "civil_defense" -> sb.append(generateCivilDefense(request));
            case "civil_appeal" -> sb.append(generateCivilAppeal(request));
            case "civil_property_preservation" -> sb.append(generatePropertyPreservation(request));
            case "civil_execution" -> sb.append(generateExecution(request));
            case "labor_contract" -> sb.append(generateLaborContract(request));
            case "labor_confidentiality" -> sb.append(generateConfidentialityAgreement(request));
            case "labor_non_compete" -> sb.append(generateNonCompete(request));
            case "labor_termination" -> sb.append(generateTermination(request));
            case "labor_arbitration" -> sb.append(generateArbitration(request));
            case "business_lawyer_letter" -> sb.append(generateLawyerLetter(request));
            case "business_ceo_letter" -> sb.append(generateCEOLetter(request));
            case "business_contract_termination" -> sb.append(generateContractTermination(request));
            case "business_payment_demand" -> sb.append(generatePaymentDemand(request));
            case "business_legal_opinion_request" -> sb.append(generateLegalOpinionRequest(request));
            case "ip_trademark_license" -> sb.append(generateTrademarkLicense(request));
            case "ip_software_license" -> sb.append(generateSoftwareLicense(request));
            case "ip_confidentiality" -> sb.append(generateIPConfidentiality(request));
            case "common_power_of_attorney" -> sb.append(generatePowerOfAttorney(request));
            default -> sb.append("通用法律文书\n\n文书内容...");
        }

        return sb.toString();
    }

    private String generateCivilPetition(DocumentDraftRequest request) {
        DocumentDraftRequest.DocumentData data = request.getCaseData();
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));

        String plaintiffName = data != null && data.getPlaintiffName() != null ? data.getPlaintiffName() : "【请填写原告姓名】";
        String plaintiffAddress = data != null && data.getPlaintiffAddress() != null ? data.getPlaintiffAddress() : "【请填写原告地址】";
        String defendantName = data != null && data.getDefendantName() != null ? data.getDefendantName() : "【请填写被告姓名】";
        String defendantAddress = data != null && data.getDefendantAddress() != null ? data.getDefendantAddress() : "【请填写被告地址】";
        String claimAmount = data != null && data.getClaimAmount() != null ? String.valueOf(data.getClaimAmount()) : "【请填写诉讼金额】";
        String claimDescription = data != null && data.getClaimDescription() != null ? data.getClaimDescription() : "【请填写诉讼请求描述】";
        String facts = data != null && data.getFacts() != null ? String.join("\n", data.getFacts()) : "【请填写案件事实】";
        String courtName = data != null && data.getCourtName() != null ? data.getCourtName() : "【请填写管辖法院】";

        StringBuilder sb = new StringBuilder();
        sb.append("民事起诉状\n\n");
        sb.append("原告：").append(plaintiffName).append("，住").append(plaintiffAddress).append("。\n\n");
        sb.append("被告：").append(defendantName).append("，住").append(defendantAddress).append("。\n\n");
        sb.append("诉讼请求：\n");
        sb.append("1. ").append(claimDescription).append("\n");
        sb.append("2. 判令被告支付利息（按实际计算）；\n");
        sb.append("3. 判令被告承担本案诉讼费用。\n\n");
        sb.append("事实与理由：\n");
        sb.append(facts).append("\n\n");
        sb.append("此致\n");
        sb.append(courtName).append("\n\n");
        sb.append("具状人：").append(plaintiffName).append("\n");
        sb.append(date);

        return sb.toString();
    }

    private String generateCivilDefense(DocumentDraftRequest request) {
        return """
            民事答辩状

            答辩人（被告）：王五，住上海市浦东新区。

            答辩人就被答辩人李四诉答辩人借款合同纠纷一案，提出答辩如下：

            一、原告所述与事实不符。答辩人并未向原告借款，而是双方之间存在其他经济往来。

            二、原告诉讼请求缺乏事实和法律依据，请求法院依法驳回。

            此致
            北京市朝阳区人民法院

            答辩人：王五
            """ + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
    }

    private String generateCivilAppeal(DocumentDraftRequest request) {
        return """
            民事上诉状

            上诉人（原审被告）：王五，住上海市浦东新区。

            被上诉人（原审原告）：李四，住北京市朝阳区。

            上诉人不服北京市朝阳区人民法院（2024）朝民初1234号民事判决，现提起上诉。

            上诉请求：
            1. 撤销原审判决，改判驳回原告的全部诉讼请求；
            2. 本案上诉费用由被上诉人承担。

            上诉理由：
            一、原审法院认定事实不清，适用法律错误。
            二、上诉人已归还全部欠款，原审判决认定未归还是错误的。

            此致
            北京市第二中级人民法院

            上诉人：王五
            """ + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
    }

    private String generateLaborContract(DocumentDraftRequest request) {
        DocumentDraftRequest.DocumentData data = request.getCaseData();
        return String.format("""
            劳动合同

            甲方（用人单位）：%s
            乙方（劳动者）：%s

            根据《中华人民共和国劳动合同法》及相关法律法规，甲乙双方经平等自愿、协商一致，订立本合同。

            一、合同期限
            本合同期限为三年，自%s起至止。

            二、工作内容
            乙方同意在甲方担任工作。

            三、工作时间和休息休假
            乙方执行标准工时制。

            四、劳动报酬
            乙方月工资为人民币元。

            五、社会保险
            甲方按法律规定为乙方缴纳社会保险。

            六、合同变更、解除和终止
            双方应当按照法律规定变更、解除和终止本合同。

            七、其他约定
            （空白）

            甲方（盖章）：          乙方（签名）：
            日期：%s              日期：
            """,
            data != null && data.getDefendantCompany() != null ? data.getDefendantCompany() : "某有限公司",
            data != null && data.getPlaintiffName() != null ? data.getPlaintiffName() : "张三",
            LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")),
            LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"))
        );
    }

    private String generateConfidentialityAgreement(DocumentDraftRequest request) {
        return """
            保密协议

            甲方（用人单位）：某有限公司
            乙方（劳动者）：张三

            甲乙双方经平等协商，就乙方在甲方工作期间知悉的商业秘密保密事宜达成如下协议：

            一、保密范围
            乙方同意对在工作期间知悉的甲方商业秘密予以保密。

            二、保密义务
            乙方不得向任何第三方披露甲方的商业秘密。

            三、保密期限
            乙方离职后年内仍应遵守保密义务。

            四、违约责任
            乙方违反本协议约定的，应当向甲方支付违约金人民币万元。

            五、本协议自双方签字之日起生效。

            甲方（盖章）：          乙方（签名）：
            """ + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
    }

    private String generateLawyerLetter(DocumentDraftRequest request) {
        return """
            律师函

            致：XXX有限公司

            北京市XX律师事务所受XXX委托，就贵司拖欠货款事宜，特函告如下：

            一、基本事实
            委托人与贵司于2024年1月签订购销合同，约定贵司向委托人采购货物。

            二、欠款情况
            截至本函发出之日，贵司尚欠委托人货款人民币50万元。

            三、法律分析
            贵司的行为已构成违约，应当承担违约责任。

            四、本律师建议
            请贵司在收到本函后个工作日内与委托人联系，否则委托人将依法采取法律行动。

            特此函告

            北京市XX律师事务所
            律师：张XX
            """ + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
    }

    private String generatePaymentDemand(DocumentDraftRequest request) {
        return """
            催款函

            致：XXX有限公司

            贵司与我方于2024年1月签订购销合同，合同金额人民币100万元。

            根据合同约定，贵司应于2024年6月30日前支付全部货款。

            截至本函发出之日，我方尚未收到贵司的任何款项。

            请贵司在收到本函后5个工作日内支付全部欠款，否则我方将依法追究贵司的违约责任。

            联系方式：XXX
            """ + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
    }

    private String generatePropertyPreservation(DocumentDraftRequest request) {
        return """
            财产保全申请书

            申请人：李四，住北京市朝阳区。

            被申请人：王五，住上海市浦东新区。

            请求事项：
            请求冻结被申请人银行存款人民币10万元或查封其同等价值的其他财产。

            事实与理由：
            申请人与被申请人因借款合同纠纷拟向贵院提起诉讼。为保障将来判决的执行，申请人请求贵院依法对被申请人的财产采取保全措施。

            此致
            北京市朝阳区人民法院

            申请人：李四
            """ + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
    }

    private String generateExecution(DocumentDraftRequest request) {
        return """
            强制执行申请书

            申请人：李四，住北京市朝阳区。

            被申请人：王五，住上海市浦东新区。

            请求事项：
            请求强制被申请人支付欠款人民币10万元及利息。

            事实与理由：
            贵院于2023年12月作出（2023）朝民初字第1234号民事判决，判令被申请人支付申请人欠款10万元。判决生效后，被申请人未按判决履行义务。

            此致
            北京市朝阳区人民法院

            申请人：李四
            """ + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
    }

    private String generateNonCompete(DocumentDraftRequest request) {
        return """
            竞业限制协议

            甲方（用人单位）：XXX有限公司
            乙方（劳动者）：XXX

            一、竞业限制期限
            乙方在离职后两年内不得从事与甲方业务竞争的工作。

            二、竞业限制补偿
            甲方按月向乙方支付竞业限制补偿金，标准为乙方离职前十二个月平均工资的30%。

            三、违约责任
            如乙方违反本协议，应向甲方支付违约金，金额为已收取竞业限制补偿金的三倍。

            四、其他
            本协议自双方签字或盖章之日起生效。

            甲方：XXX有限公司    乙方：XXX
            """ + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
    }

    private String generateTermination(DocumentDraftRequest request) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
        return "解除劳动合同协议\n\n" +
            "甲方（用人单位）：XXX有限公司\n" +
            "乙方（劳动者）：XXX\n\n" +
            "甲乙双方协商一致，同意于" + date + "解除劳动合同。\n\n" +
            "双方确认：\n" +
            "1. 甲方支付乙方经济补偿金人民币X万元。\n" +
            "2. 甲方为乙方缴纳社会保险至本月底。\n" +
            "3. 乙方应在离职前完成工作交接。\n\n" +
            "本协议自双方签字或盖章之日起生效。\n\n" +
            "甲方：XXX有限公司    乙方：XXX\n" +
            date;
    }

    private String generateArbitration(DocumentDraftRequest request) {
        return """
            劳动仲裁申请书

            申请人：XXX，住XXX。

            被申请人：XXX有限公司，住XXX。

            请求事项：
            1. 请求被申请人支付拖欠工资人民币X元。
            2. 请求被申请人支付经济补偿金人民币X元。

            事实与理由：
            申请人于2020年1月入职被申请人处，从事XXX工作。2024年6月，被申请人开始拖欠工资，至今已累计拖欠三个月。

            此致
            XXX劳动争议仲裁委员会

            申请人：XXX
            """ + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
    }

    private String generateCEOLetter(DocumentDraftRequest request) {
        return """
            CEO函

            致：XXX有限公司 管理层

            鉴于我司与贵司的合作项目长期未能按计划推进，严重影响双方利益，我作为CEO正式函告：

            请贵司在收到本函后7个工作日内提出切实可行的解决方案。

            如未能在此期限内得到满意答复，我司将不得不采取进一步法律行动维护自身权益。

            [公司名称]
            [CEO姓名]
            """ + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
    }

    private String generateContractTermination(DocumentDraftRequest request) {
        return """
            合同解除通知函

            致：XXX有限公司

            我方与贵司于2024年1月签订的《购销合同》（合同编号：XXX），因贵司严重违约，我方依据《民法典》第五百六十五条规定，正式通知贵司解除该合同。

            解除原因：
            贵司未按合同约定履行交货义务，且经我方多次催告后仍未履行。

            请贵司在收到本通知后10日内返还已支付的预付款项。

            [公司名称]
            """ + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
    }

    private String generateLegalOpinionRequest(DocumentDraftRequest request) {
        return """
            法律意见书请求函

            致：XXX律师事务所

            我司拟就以下事项请求贵所指派律师出具法律意见书：

            事项描述：
            [详细描述需要法律意见的事项]

            请贵所在收到本函后15个工作日内出具书面法律意见书。

            [公司名称]
            联系方式：XXX
            """ + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
    }

    private String generateTrademarkLicense(DocumentDraftRequest request) {
        return """
            商标许可合同

            许可人（甲方）：XXX有限公司
            被许可人（乙方）：XXX

            一、许可范围
            甲方许可乙方使用甲方注册的XXX商标（注册号：XXX）。

            二、许可期限
            自本协议签订之日起X年。

            三、许可费用
            乙方应按年向甲方支付商标许可使用费人民币X万元。

            四、违约责任
            任何一方违反本协议，应承担违约责任并赔偿对方因此遭受的损失。

            甲方：XXX有限公司    乙方：XXX
            """ + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
    }

    private String generateSoftwareLicense(DocumentDraftRequest request) {
        return """
            软件许可协议

            许可人（甲方）：XXX有限公司
            被许可人（乙方）：XXX

            一、许可范围
            甲方许可乙方使用甲方开发的XXX软件。

            二、许可类型
            本许可为非独占性许可，乙方不得转让或许可第三方使用。

            三、许可费用
            乙方应向甲方支付软件许可使用费人民币X万元。

            四、知识产权
            本软件及相关文档的知识产权归甲方所有。

            甲方：XXX有限公司    乙方：XXX
            """ + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
    }

    private String generateIPConfidentiality(DocumentDraftRequest request) {
        return """
            商业秘密保密协议

            甲方（用人单位）：XXX有限公司
            乙方（劳动者）：XXX

            一、保密范围
            乙方同意对甲方的商业秘密（包括但不限于技术信息、经营信息）予以保密。

            二、保密义务
            乙方在任职期间及离职后两年内，不得披露、使用甲方的商业秘密。

            三、违约责任
            如乙方违反本协议，应向甲方支付违约金人民币X万元，并赔偿甲方因此遭受的全部损失。

            甲方：XXX有限公司    乙方：XXX
            """ + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
    }

    private String generatePowerOfAttorney(DocumentDraftRequest request) {
        return """
            授权委托书

            委托人：XXX，住XXX，身份证号：XXX。

            受托人：XXX，住XXX，身份证号：XXX。

            委托事项：
            委托人就与XXX纠纷一案，授权受托人作为委托人的代理人，代表委托人参加诉讼。

            代理权限：
            1. 代为承认、变更、放弃诉讼请求；
            2. 代为进行和解；
            3. 代为签收法律文书；
            4. 代为申请执行。

            委托期限：自本委托书签署之日起至本案终结之日止。

            委托人：XXX（签字）
            """ + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
    }

    private String generateRiskPrompt(DocumentTemplate template, DocumentDraftRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("【风险提示】\n\n");

        if (request.getCaseData() != null && request.getCaseData().getClaimAmount() != null) {
            BigDecimal amount = request.getCaseData().getClaimAmount();
            if (amount.compareTo(new BigDecimal("50000")) > 0) {
                sb.append("1. 涉及金额较大，建议委托专业律师代理。\n");
            }
        }

        sb.append("2. 诉讼时效：民事案件的诉讼时效为三年，请注意及时主张权利。\n");
        sb.append("3. 证据保全：建议保留好相关合同、付款凭证、往来函件等证据材料。\n");
        sb.append("4. 管辖法院：根据被告住所地或合同约定确定管辖法院。\n");

        return sb.toString();
    }

    private String generateDisclaimer(DocumentTemplate template, DocumentDraftRequest.DocumentData data) {
        String base = "本法律文书由AI辅助生成，仅供参考。";

        if (data != null && data.getClaimAmount() != null) {
            BigDecimal amount = data.getClaimAmount();
            if (amount.compareTo(new BigDecimal("50000")) > 0) {
                base += "鉴于本文书涉及金额较大，强烈建议您咨询专业律师。";
            }
        }

        Set<String> personalTypes = Set.of("divorce", "inheritance", "labor_dispute");
        if (personalTypes.contains(template.code)) {
            base += "鉴于本文书涉及人身权益，强烈建议您咨询专业律师。";
        }

        return base + "\n使用前请务必由具有执业资格的律师进行审核和修改。";
    }

    private static class DocumentTemplate {
        String name;
        String category;
        boolean popular;
        List<String> requiredFields;
        List<String> referencedLaws;
        String code;

        public DocumentTemplate(String name, String category, boolean popular,
                List<String> requiredFields, List<String> referencedLaws, String code) {
            this.name = name;
            this.category = category;
            this.popular = popular;
            this.requiredFields = requiredFields;
            this.referencedLaws = referencedLaws;
            this.code = code;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public boolean isPopular() { return popular; }
        public void setPopular(boolean popular) { this.popular = popular; }
        public List<String> getRequiredFields() { return requiredFields; }
        public void setRequiredFields(List<String> requiredFields) { this.requiredFields = requiredFields; }
        public List<String> getReferencedLaws() { return referencedLaws; }
        public void setReferencedLaws(List<String> referencedLaws) { this.referencedLaws = referencedLaws; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }

    public static class TemplateInfo {
        private String templateCode;
        private String templateName;
        private String category;
        private boolean popular;

        public String getTemplateCode() { return templateCode; }
        public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }
        public String getTemplateName() { return templateName; }
        public void setTemplateName(String templateName) { this.templateName = templateName; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public boolean isPopular() { return popular; }
        public void setPopular(boolean popular) { this.popular = popular; }
    }
}