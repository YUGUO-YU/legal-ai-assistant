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
import java.util.regex.Pattern;
import java.util.regex.Matcher;

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

    @Value("${mock.enabled:false}")
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
        String result = llmClient.chat(prompt);
        return cleanDocumentContent(result);
    }

    private String cleanDocumentContent(String content) {
        if (content == null || content.isEmpty()) return content;
        
        content = content.trim();
        
        // 移除 Markdown 代码块标记
        content = content.replaceAll("^```(?:json|text|)?\\s*", "");
        content = content.replaceAll("\\s*```$", "");
        
        // 移除常见的AI解释性前缀
        String[] prefixesToRemove = {
            "以下是文书正文：",
            "以下是生成的文书：",
            "文书正文如下：",
            "根据您的要求，生成如下文书：",
            "根据案件信息，生成如下法律文书：",
            "【文书正文】",
            "【生成结果】",
            "生成结果如下：",
            "文书内容如下：",
            "以下是法律文书："
        };
        for (String prefix : prefixesToRemove) {
            if (content.startsWith(prefix)) {
                content = content.substring(prefix.length()).trim();
            }
        }
        
        // 移除开头的解释性文字
        content = content.replaceAll("^(?:根据|按照|依据|为了)[^，,\n]{0,50}(?:如下|生成|输出|提供)[：:]", "");
        
        // 移除末尾的解释性文字
        content = content.replaceAll("(?:以上|上述)[^。\\n]{0,50}(?:文书|内容|结果)[。]?\\s*$", "");
        
        return content.trim();
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
        if (notEmpty(info.getPlaintiffName()) || (info.getPlaintiffNames() != null && !info.getPlaintiffNames().isEmpty())) core++;
        if (notEmpty(info.getDefendantName()) || (info.getDefendantNames() != null && !info.getDefendantNames().isEmpty())) core++;
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
        merged.setUnifiedSocialCreditCode(coalesce(primary.getUnifiedSocialCreditCode(), fallback.getUnifiedSocialCreditCode()));
        merged.setLegalRepresentative(coalesce(primary.getLegalRepresentative(), fallback.getLegalRepresentative()));
        merged.setBirthDate(coalesce(primary.getBirthDate(), fallback.getBirthDate()));
        merged.setAge(primary.getAge() != null ? primary.getAge() : fallback.getAge());
        merged.setPlaintiffNames(mergePartyLists(primary.getPlaintiffNames(), fallback.getPlaintiffNames()));
        merged.setDefendantNames(mergePartyLists(primary.getDefendantNames(), fallback.getDefendantNames()));
        merged.setSuccess(true);
        merged.setErrorMessage(null);
        merged.setDataSource(primary.getDataSource() != null ? primary.getDataSource() : "AI 智能识别");
        return merged;
    }

    private <T> List<T> mergePartyLists(List<T> a, List<T> b) {
        if (a == null || a.isEmpty()) return b;
        if (b == null || b.isEmpty()) return a;
        Set<T> merged = new LinkedHashSet<>(a);
        merged.addAll(b);
        return new ArrayList<>(merged);
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
        if (info.getPlaintiffNames() != null && !info.getPlaintiffNames().isEmpty()) c += info.getPlaintiffNames().size() - 1;
        if (info.getDefendantNames() != null && !info.getDefendantNames().isEmpty()) c += info.getDefendantNames().size() - 1;
        if (notEmpty(info.getSalary())) c++;
        if (notEmpty(info.getStartDate())) c++;
        if (notEmpty(info.getDisputeType())) c++;
        return c;
    }

    private ExtractedInfo extractByLLM(String text, String templateCode) throws Exception {
        String prompt = "你是法律文本关键信息抽取助手。请从以下案件文本中严格抽取各字段，注意字段边界不要混淆。\n"
            + "【重要规则】\n"
            + "1. claimDescription（诉讼请求）只包含具体的请求内容，如'归还借款本金X元''支付货款X元''解除劳动合同'，不包含任何事实描述\n"
            + "2. facts（事实与理由）只包含客观发生的事情描述，不包含诉讼请求\n"
            + "3. 地址只提取省市区街道门牌号等地理信息，不包含当事人姓名或代码\n"
            + "4. 统一社会信用代码是18位字符，如91110000XXXXXXXX\n\n"
            + "JSON 字段（缺失填空字符串或 null）：\n"
            + "plaintiffName（原告/申请人姓名/单位名称）\n"
            + "plaintiffAddress（原告地址，仅地理地址）\n"
            + "defendantName（被告/被申请人姓名/单位名称）\n"
            + "defendantAddress（被告地址，仅地理地址）\n"
            + "claimAmount（诉讼金额，数字，单位元，不要带\"元\"字符）\n"
            + "claimDescription（诉讼请求，仅请求内容，不含事实与理由）\n"
            + "facts（事实与理由，仅客观事实描述，不含诉讼请求）\n"
            + "courtName（管辖法院名称）\n"
            + "unifiedSocialCreditCode（企业统一社会信用代码，18位）\n"
            + "legalRepresentative（法定代表人姓名）\n"
            + "employerName（用人单位名称）\n"
            + "employeeName（劳动者姓名）\n"
            + "workContent（工作岗位/工作内容）\n"
            + "salary（月薪/工资金额与单位）\n"
            + "startDate（入职/起始日期）\n"
            + "disputeType（争议类型）\n"
            + "birthDate（出生日期，如1990年1月1日）\n"
            + "age（年龄，整数）\n"
            + "plaintiffNames（多个原告姓名，JSON数组格式，如[\"张三\",\"李四\"]）\n"
            + "defendantNames（多个被告姓名，JSON数组格式，如[\"王五\",\"赵六\"]）\n\n"
            + "仅输出 JSON，不要任何解释：\n\n"
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
        info.setBirthDate(textOrEmpty(node, "birthDate"));
        String ageStr = textOrEmpty(node, "age");
        if (!ageStr.isEmpty()) {
            try {
                info.setAge(Integer.parseInt(ageStr.replaceAll("[^0-9]", "")));
            } catch (Exception ignore) {}
        }
        // 解析多当事人
        JsonNode pNamesNode = node.get("plaintiffNames");
        if (pNamesNode != null && pNamesNode.isArray()) {
            List<String> pNames = new ArrayList<>();
            for (JsonNode n : pNamesNode) {
                String name = n.asText("").trim();
                if (!name.isEmpty()) pNames.add(name);
            }
            if (!pNames.isEmpty()) info.setPlaintiffNames(pNames);
        }
        JsonNode dNamesNode = node.get("defendantNames");
        if (dNamesNode != null && dNamesNode.isArray()) {
            List<String> dNames = new ArrayList<>();
            for (JsonNode n : dNamesNode) {
                String name = n.asText("").trim();
                if (!name.isEmpty()) dNames.add(name);
            }
            if (!dNames.isEmpty()) info.setDefendantNames(dNames);
        }
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
            || (info.getClaimAmount() != null && info.getClaimAmount().signum() > 0)
            || (info.getPlaintiffNames() != null && !info.getPlaintiffNames().isEmpty())
            || (info.getDefendantNames() != null && !info.getDefendantNames().isEmpty());
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

            // 2. 多当事人提取
            List<String> allPlaintiffs = extractAllParties(text, "原告");
            if (!allPlaintiffs.isEmpty()) info.setPlaintiffNames(allPlaintiffs);

            // 3. 被告姓名 - 多种格式支持
            String defendant = extractPartyName(text, "被告");
            if (defendant == null) defendant = extractPartyName(text, "被申请人");
            if (defendant == null) defendant = extractPartyName(text, "被执行人");
            if (defendant == null) defendant = extractPartyName(text, "被诉人");
            // 上诉案件
            if (defendant == null) defendant = extractPartyName(text, "上诉人");
            info.setDefendantName(defendant);

            // 4. 多被告提取
            List<String> allDefendants = extractAllParties(text, "被告");
            if (!allDefendants.isEmpty()) info.setDefendantNames(allDefendants);

            // 5. 原告地址
            String plaintiffAddr = extractAddress(text, "原告");
            if (plaintiffAddr == null) plaintiffAddr = extractAddress(text, "申请人");
            info.setPlaintiffAddress(plaintiffAddr);

            // 6. 被告地址
            String defendantAddr = extractAddress(text, "被告");
            if (defendantAddr == null) defendantAddr = extractAddress(text, "被申请人");
            info.setDefendantAddress(defendantAddr);

            // 7. 法院名称
            String court = extractCourtName(text);
            info.setCourtName(court);

            // 8. 诉讼金额
            BigDecimal amount = parseAmount(text);
            if (amount != null) {
                info.setClaimAmount(amount);
            }

            // 10. 诉讼请求
            String claimDesc = extractClaimDescription(text);
            info.setClaimDescription(claimDesc);

            // 11. 事实与理由
            String facts = extractFacts(text);
            info.setFacts(facts);

            // 12. 用人单位（劳动纠纷）
            String employer = extractPartyName(text, "用人单位");
            if (employer != null) {
                employer = employer.replaceAll("(住所|地址|统一社会信用代码).*$", "").trim();
            }
            info.setEmployerName(employer);

            // 13. 劳动者
            String employee = extractPartyName(text, "劳动者");
            info.setEmployeeName(employee);

            // 14. 工作岗位
            String workContent = extractField(text, "工作岗位", 40);
            if (workContent == null) workContent = extractField(text, "从事", 40);
            info.setWorkContent(workContent);

            // 15. 工资
            String salary = extractField(text, "工资", 20);
            if (salary == null) salary = extractField(text, "月薪", 20);
            if (salary == null) salary = extractField(text, "月工资", 20);
            info.setSalary(salary);

            // 16. 入职日期
            String startDate = extractDate(text);
            info.setStartDate(startDate);

            // 17. 争议类型
            String dispute = extractDisputeType(text);
            info.setDisputeType(dispute);

            // 18. 案件类型
            String caseType = extractCaseType(text);
            info.setCaseType(caseType);

            // 19. 原告电话
            String plaintiffPhone = extractPhone(text, "原告");
            info.setPlaintiffPhone(plaintiffPhone);

            // 20. 被告电话
            String defendantPhone = extractPhone(text, "被告");
            info.setDefendantPhone(defendantPhone);

            // 21. 原告身份证号
            String plaintiffIdCard = extractIdCard(text, "原告");
            info.setPlaintiffIdCard(plaintiffIdCard);

            // 22. 被告身份证号
            String defendantIdCard = extractIdCard(text, "被告");
            info.setDefendantIdCard(defendantIdCard);

            // 23. 诉讼依据
            String claimBasis = extractClaimBasis(text);
            info.setClaimBasis(claimBasis);

            // 24. 证据
            String evidence = extractEvidence(text);
            info.setEvidence(evidence);

            // 25. 统一社会信用代码
            String creditCode = extractUnifiedSocialCreditCode(text);
            info.setUnifiedSocialCreditCode(creditCode);

            // 26. 法定代表人
            String legalRep = extractLegalRepresentative(text);
            info.setLegalRepresentative(legalRep);

            // 27. 职务
            String position = extractPosition(text);
            info.setPosition(position);

            // 28. 住所地
            String residence = extractResidenceAddress(text);
            info.setResidenceAddress(residence);

            // 29. 出生日期
            String birthDate = extractBirthDate(text, "原告");
            if (birthDate == null) birthDate = extractBirthDate(text, "申请人");
            if (birthDate == null) birthDate = extractBirthDate(text, "被告");
            info.setBirthDate(birthDate);

            // 30. 年龄
            Integer age = extractAge(text, "原告");
            if (age == null) age = extractAge(text, "申请人");
            if (age == null) age = extractAge(text, "被告");
            info.setAge(age);

        } catch (Exception e) {
            log.warn("本地正则提取异常: {}", e.getMessage());
        }

        return info;
    }

    private String extractPartyName(String text, String keyword) {
        if (text == null || keyword == null) return null;

        int idx = text.indexOf(keyword);
        if (idx < 0) return null;

        int start = idx + keyword.length();
        int end = Math.min(start + 400, text.length());
        String segment = text.substring(start, end);

        segment = segment.replaceAll("^[：:,，\\s为叫字]+", "");

        String cleanedSegment = segment;

        // 预处理：移除各类干扰内容
        cleanedSegment = cleanedSegment.replaceAll("(?i)统一社会信用代码[^\\n，,]{0,50}", "");
        cleanedSegment = cleanedSegment.replaceAll("(?i)住所[：:]\\s*[^\\n，,]{5,150}", "");
        cleanedSegment = cleanedSegment.replaceAll("(?i)地址[^\\n，,]{5,150}", "");
        cleanedSegment = cleanedSegment.replaceAll("(?i)身份证[号]?[^\\n，,]{10,30}", "");
        cleanedSegment = cleanedSegment.replaceAll("(?i)出生[日期][^\\n，,]{5,20}", "");
        cleanedSegment = cleanedSegment.replaceAll("(?i)电话[^\\n]{5,20}", "");
        cleanedSegment = cleanedSegment.replaceAll("(?i)邮编[^\\n]{5,10}", "");
        cleanedSegment = cleanedSegment.replaceAll("(?i)性别[男女人妖]", "");
        cleanedSegment = cleanedSegment.replaceAll("(?i)民族[\\u4e00-\\u9fa5]{2,6}", "");
        cleanedSegment = cleanedSegment.replaceAll("(?i)法定代表人[^\\n，,]{2,20}", "");

        // 取第一行
        int lineEnd = cleanedSegment.indexOf('\n');
        if (lineEnd > 0 && lineEnd < 250) {
            cleanedSegment = cleanedSegment.substring(0, lineEnd);
        }
        // 取第一句（分号）
        int semicolonEnd = cleanedSegment.indexOf('；');
        if (semicolonEnd > 2 && semicolonEnd < 120) {
            cleanedSegment = cleanedSegment.substring(0, semicolonEnd);
        }
        // 取第一段（逗号，但不要太短）
        int commaEnd = cleanedSegment.indexOf('，');
        if (commaEnd > 10 && commaEnd < 150) {
            cleanedSegment = cleanedSegment.substring(0, commaEnd);
        }
        // 取顿号分隔的第一人称（多人时只取第一个）
        int pauseEnd = cleanedSegment.indexOf('、');
        if (pauseEnd > 2 && pauseEnd < 120) {
            cleanedSegment = cleanedSegment.substring(0, pauseEnd);
        }

        cleanedSegment = cleanedSegment.trim();

        // 1. 优先匹配公司名：XX有限公司 / XX公司 / XX集团（被告可能是公司企业）
        Pattern p = Pattern.compile("^[\\u4e00-\\u9fa5A-Za-z0-9（）()\\-·]{2,35}(?:有限公司|公司|集团|企业|合作社|协会|中心|学校|医院|银行|酒店|宾馆|商场|工厂|作坊|饭店|酒楼|超市|市场|工作室|事务所|农场|牧场|矿山|港口|码头|仓库|物流|贸易|科技|电子|机械|化工|纺织|服装|食品|医药|美容|健身|投资|资产|基金|保险|信托|租赁|维修|装修|建材|木材|金属|矿产|能源|电力|燃气|水务|环保|农业|林业|渔业|畜牧|兽医)");
        Matcher m = p.matcher(cleanedSegment);
        if (m.find()) {
            String name = m.group().trim();
            name = name.replaceAll("(住所|地址|注册地|统一社会信用代码)[^\\n，,，、；;]*", "").trim();
            if (name.length() >= 4) return name;
        }

        // 2. 匹配自然人姓名：2-4个汉字
        p = Pattern.compile("^[\\u4e00-\\u9fa5]{2,4}(?:[·•][\\u4e00-\\u9fa5]{2,4})?");
        m = p.matcher(cleanedSegment);
        if (m.find()) {
            String name = m.group().trim();
            name = name.replaceAll("(男|女|族|有限责任公司)?$", "").trim();
            if (name.length() >= 2 && !isInstitutionalWord(name)) {
                return name;
            }
        }

        // 3. 匹配"XX（性别/民族/职务）"格式的自然人
        p = Pattern.compile("^[\\u4e00-\\u9fa5]{2,5}(?=[（\\(][^）\\)]*$)");
        m = p.matcher(cleanedSegment);
        if (m.find()) {
            String name = m.group().trim();
            if (name.length() >= 2 && !isInstitutionalWord(name)) {
                return name;
            }
        }

        // 4. 匹配法定代表人：先找标注再提取姓名
        p = Pattern.compile("(?:法定代表人|法人代表|负责人|经营者)[：:\\s]*([\\u4e00-\\u9fa5]{2,6})");
        m = p.matcher(segment);
        if (m.find()) {
            String name = m.group(1).trim();
            if (name.length() >= 2 && !isInstitutionalWord(name)) {
                return name;
            }
        }

        return null;
    }

    private List<String> extractAllParties(String text, String roleLabel) {
        if (text == null || roleLabel == null) return Collections.emptyList();

        List<String> parties = new ArrayList<>();
        String[] roleVariants = {
            roleLabel,
            roleLabel + "一",
            roleLabel + "二",
            roleLabel + "三",
            roleLabel + "四",
            roleLabel + "：",
            roleLabel + ":",
            roleLabel + "人"
        };

        for (String variant : roleVariants) {
            int idx = text.indexOf(variant);
            if (idx >= 0) {
                int start = idx + variant.length();
                int segEnd = Math.min(start + 600, text.length());
                String segment = text.substring(start, segEnd);

                // 预处理
                segment = segment.replaceAll("^[：:,，\\s]+", "");

                // 分段处理：换行、分号、顿号
                String[] parts = segment.split("[\n；;]");
                for (String part : parts) {
                    part = part.trim();
                    if (part.isEmpty()) continue;

                    // 取顿号分隔的每一项
                    String[] multiParts = part.split("、");
                    for (String mp : multiParts) {
                        mp = mp.trim();
                        if (mp.isEmpty()) continue;

                        // 预处理：移除干扰内容
                        mp = mp.replaceAll("(?i)统一社会信用代码[^\\n，,]{0,50}", "");
                        mp = mp.replaceAll("(?i)住所[：:]\\s*[^\\n，,]{5,150}", "");
                        mp = mp.replaceAll("(?i)地址[^\\n，,]{5,150}", "");
                        mp = mp.replaceAll("(?i)身份证[号]?[^\\n，,]{10,30}", "");
                        mp = mp.replaceAll("(?i)出生[日期][^\\n，,]{5,20}", "");
                        mp = mp.replaceAll("(?i)电话[^\\n]{5,20}", "");
                        mp = mp.replaceAll("(?i)邮编[^\\n]{5,10}", "");
                        mp = mp.replaceAll("(?i)民族[\\u4e00-\\u9fa5]{2,6}", "");
                        mp = mp.replaceAll("(?i)法定代表人[^\\n，,]{2,20}", "");

                        // 去掉常见后缀和标注
                        mp = mp.replaceAll("^[：:,，\\s为叫字]+", "");

                        // 截取到下一个角色标签之前
                        for (String rv : roleVariants) {
                            int rvIdx = mp.indexOf(rv);
                            if (rvIdx > 2) {
                                mp = mp.substring(0, rvIdx).trim();
                            }
                        }

                        String name = extractSingleName(mp);
                        if (name != null && !name.isEmpty() && !parties.contains(name)) {
                            parties.add(name);
                        }
                    }
                }
                break; // 找到第一个变体就停止
            }
        }
        return parties;
    }

    private String extractSingleName(String text) {
        if (text == null || text.trim().isEmpty()) return null;

        String cleaned = text.trim();

        // 截取到第一个非名称字符
        int commaIdx = cleaned.indexOf('，');
        int semicolonIdx = cleaned.indexOf('；');
        int newlineIdx = cleaned.indexOf('\n');
        int bracketIdx = cleaned.indexOf('（');

        int cutIdx = cleaned.length();
        if (bracketIdx > 0 && bracketIdx < cutIdx) cutIdx = bracketIdx;
        if (commaIdx > 0 && commaIdx < cutIdx) cutIdx = commaIdx;
        if (semicolonIdx > 0 && semicolonIdx < cutIdx) cutIdx = semicolonIdx;
        if (newlineIdx > 0 && newlineIdx < cutIdx) cutIdx = newlineIdx;

        cleaned = cleaned.substring(0, cutIdx).trim();

        // 优先匹配公司名（被告可能是公司企业）
        Pattern p = Pattern.compile("^[\\u4e00-\\u9fa5A-Za-z0-9（）()\\-·]{2,35}(?:有限公司|公司|集团|企业|合作社|协会|中心|学校|医院|银行|酒店|宾馆|商场|工厂|作坊|饭店|酒楼|超市|市场)");
        Matcher m = p.matcher(cleaned);
        if (m.find()) {
            String name = m.group().trim();
            name = name.replaceAll("(住所|地址|注册地|统一社会信用代码)[^\\n，,，、；;]*", "").trim();
            if (name.length() >= 4) return name;
        }

        // 匹配自然人姓名
        p = Pattern.compile("^[\\u4e00-\\u9fa5]{2,4}(?:[·•][\\u4e00-\\u9fa5]{2,4})?");
        m = p.matcher(cleaned);
        if (m.find()) {
            String name = m.group().trim();
            name = name.replaceAll("(男|女|族|有限责任公司)$", "").trim();
            if (name.length() >= 2 && !isInstitutionalWord(name)) {
                return name;
            }
        }

        return null;
    }

    private boolean isInstitutionalWord(String s) {
        String[] words = {"法院", "检察院", "公安局", "司法局", "人民政府", "委员会", "办公室", "厅", "局", "部", "公司", "集团"};
        for (String w : words) {
            if (s.contains(w)) return true;
        }
        return false;
    }

    private String extractAddress(String text, String keyword) {
        if (text == null || keyword == null) return null;

        int idx = text.indexOf(keyword);
        if (idx < 0) return null;

        int start = idx + keyword.length();
        int end = Math.min(start + 500, text.length());
        String segment = text.substring(start, end);

        segment = segment.replaceAll("^[：:,，\\s为住]+", "");

        // 移除各类干扰内容（包括姓名、电话、身份证等个人信息）
        segment = segment.replaceAll("(?i)统一社会信用代码[^\\n]{10,30}", "");
        segment = segment.replaceAll("(?i)身份证[号]?[^\\n]{10,30}", "");
        segment = segment.replaceAll("(?i)法定代表人[^\\n]{2,20}", "");
        segment = segment.replaceAll("(?i)电话[^\\n]{5,20}", "");
        segment = segment.replaceAll("(?i)邮编[^\\n]{5,10}", "");
        segment = segment.replaceAll("(?i)联系人[^\\n]{2,20}", "");
        segment = segment.replaceAll("(?i)电子邮箱[^\\n]{5,30}", "");

        // 移除姓名+分隔符模式（如"张三，"、"李某："等）
        segment = segment.replaceAll("^[\\u4e00-\\u9fa5]{2,4}[：:,，\\s]+", "");
        // 移除"男/女"性别标注
        segment = segment.replaceAll("^[男女]\\s*[，,：:\\s]+", "");
        // 移除民族标注
        segment = segment.replaceAll("^[\\u4e00-\\u9fa5]{2,6}族[，,：:\\s]*", "");

        // OCR纠错：处理常见字符混淆
        segment = fixOcrChars(segment);

        // 策略1：合并连续的多行地址（跨行地址）
        String merged = mergeMultiLineAddress(segment);
        if (merged != null) return merged;

        // 策略2：截取第一行/第一段
        String firstLine = getFirstLine(segment);
        if (isValidAddress(firstLine)) return firstLine;

        // 策略3：尝试整段
        if (isValidAddress(segment.trim())) return segment.trim();

        return null;
    }

    private Map<String, String> parseStructuredAddress(String address) {
        Map<String, String> result = new LinkedHashMap<>();
        if (address == null || address.isEmpty()) return result;

        String addr = address.trim();

        // 省/自治区/直辖市
        String province = matchRegex(addr, "(?:([^省\s]+省)|([^省\s]+自治区)|(北京市)|(天津市)|(上海市)|(重庆市)|(香港)|(澳门))");
        if (province != null) {
            result.put("province", province);
            addr = addr.substring(addr.indexOf(province) + province.length());
        }

        // 市/地区/自治州
        String city = matchRegex(addr, "([^省\s]{2,10}?(?:市|地区|自治州|盟|特别行政区))");
        if (city != null) {
            result.put("city", city);
            int cityIdx = addr.indexOf(city);
            if (cityIdx >= 0) addr = addr.substring(cityIdx + city.length());
        }

        // 区/县/县级市
        String district = matchRegex(addr, "([^市\s]{2,10}?(?:区|县|市|旗|林|特区))");
        if (district != null) {
            result.put("district", district);
            int distIdx = addr.indexOf(district);
            if (distIdx >= 0) addr = addr.substring(distIdx + district.length());
        }

        // 街道/路/巷/道
        String street = matchRegex(addr, "([\\u4e00-\\u9fa5]+(?:街道|路|巷|道|街|镇|乡))");
        if (street != null) {
            result.put("street", street);
            int streetIdx = addr.indexOf(street);
            if (streetIdx >= 0) addr = addr.substring(streetIdx + street.length());
        }

        // 门牌号/栋/单元/室
        String number = matchRegex(addr, "([\\d零一二三四五六七八九十百]+(?:号|栋|幢|单元|层|室|弄))");
        if (number != null) {
            result.put("number", number);
        }

        // 剩余未解析的部分
        String remaining = addr.trim();
        if (!remaining.isEmpty() && result.isEmpty()) {
            result.put("raw", address);
        } else if (!remaining.isEmpty()) {
            result.put("detail", remaining);
        }

        return result;
    }

    private String matchRegex(String text, String regex) {
        if (text == null || regex == null) return null;
        try {
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(text);
            if (m.find()) {
                return m.group(1) != null ? m.group(1) : m.group();
            }
        } catch (Exception ignore) {}
        return null;
    }

    private String fixOcrChars(String text) {
        if (text == null) return null;
        // 全角数字转半角（地址中常见全角数字）
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= '０' && c <= '９') {
                sb.append((char)(c - '０' + '0'));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String mergeMultiLineAddress(String segment) {
        if (segment == null || segment.length() < 10) return null;

        StringBuilder sb = new StringBuilder();
        String[] lines = segment.split("\\n");
        int validLines = 0;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            // 如果这行包含地址关键词或者是前几行的延续，合并
            if (validLines == 0 || containsAddressKeyword(line) || line.matches("[\\u4e00-\\u9fa5]+[路街巷道号栋楼室弄]")) {
                if (sb.length() > 0 && validLines > 0) sb.append(" ");
                sb.append(line);
                validLines++;
                if (validLines >= 3) break; // 最多合并3行
            }
        }

        String merged = sb.toString().trim();
        if (merged.length() >= 8 && containsAddressKeyword(merged)) {
            // 截取到第一个分句标记
            int periodIdx = merged.indexOf('。');
            if (periodIdx > 5) merged = merged.substring(0, periodIdx);
            return merged;
        }
        return null;
    }

    private boolean containsAddressKeyword(String text) {
        if (text == null) return false;
        String[] keywords = {"省", "市", "区", "县", "镇", "乡", "村", "路", "街", "巷", "道", "号", "栋", "楼", "室", "弄", "幢", "单元", "层"};
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    private String getFirstLine(String segment) {
        // 优先取第一个换行之前的
        int lineEnd = segment.indexOf('\n');
        if (lineEnd > 3 && lineEnd < 200) {
            return segment.substring(0, lineEnd).trim();
        }
        // 否则取到第一个分隔符
        int commaEnd = segment.indexOf('，');
        int semicolonEnd = segment.indexOf('；');
        int periodEnd = segment.indexOf('。');
        int end = segment.length();
        if (commaEnd > 3 && commaEnd < end) end = commaEnd;
        if (semicolonEnd > 3 && semicolonEnd < end) end = semicolonEnd;
        if (periodEnd > 3 && periodEnd < end) end = periodEnd;
        return segment.substring(0, end).trim();
    }

    private boolean isValidAddress(String text) {
        if (text == null || text.length() < 6 || text.length() > 200) return false;
        // 必须包含地址关键词
        if (!containsAddressKeyword(text)) return false;
        // 排除明显不是地址的内容（过多数字/字母）
        if (text.replaceAll("[\\u4e00-\\u9fa5]", "").length() > text.length() * 0.4) return false;
        // 排除包含"公司""法院"等机构词
        if (text.matches(".*[公司法院检察院公安局].*")) return false;
        return true;
    }

    private String extractCourtName(String text) {
        if (text == null) return null;

        // 按优先级从高到低匹配各种法院格式
        String[] patterns = {
            // 1. 带"管辖""受案""起诉至""向""移送"语义的标注行（最精确）
            "(?:管辖法院|受案法院|起诉至|诉至|向法院|移送至|立案法院|管辖|管辖地)[：:\\s]*([\\u4e00-\\u9fa5]{2,10}(?:人民法院|中级法院|高级法院|专门法院|铁路法院|军事法院|法院))",
            // 2. 此致后面的法院（最常见）- 精确匹配
            "此致\\s*([\\u4e00-\\u9fa5]{2,12}(?:人民法院|中级法院|高级法院|专门法院|铁路法院|军事法院|法院))",
            // 3. 具状人/落款后面的法院
            "(?:具状人|起诉人|上诉人|答辩人|申请人|被告)[\\s\\S]{0,80}此致\\s*([\\u4e00-\\u9fa5]{2,12}(?:人民法院|中级法院|高级法院|专门法院|铁路法院|军事法院|法院))",
            // 4. XX高级人民法院（省级）
            "([\\u4e00-\\u9fa5]{2,8}高级人民法院)",
            // 5. XX省XX市人民法院（完整省市名）
            "([\\u4e00-\\u9fa5]{2,10}人民法院)",
            // 6. XX市XX人民法院（市级）
            "([\\u4e00-\\u9fa5]{2,8}人民法院)",
            // 7. XX市XX中级人民法院
            "([\\u4e00-\\u9fa5]{2,6}中级人民法院)",
            // 8. 知识产权法院、互联网法院、金融法院等专门法院
            "(?:北京互联网法院|上海互联网法院|广州互联网法院|杭州互联网法院|成都互联网法院|武汉互联网法院|西安互联网法院|长沙互联网法院|郑州互联网法院|南京互联网法院|重庆互联网法院|深圳互联网法院|天津互联网法院|北京知识产权法院|广州知识产权法院|上海知识产权法院|成都知识产权法院|深圳知识产权法院|上海金融法院|北京金融法院|深圳金融法院|重庆金融法院|成都金融法院|广州金融法院|杭州金融法院|宁波金融法院|温州金融法院|苏州金融法院|南京金融法院|武汉金融法院|西安金融法院|青岛金融法院|大连金融法院|厦门金融法院|福州金融法院|济南金融法院|沈阳金融法院|哈尔滨金融法院|长春金融法院|南昌金融法院|长沙金融法院|昆明金融法院|贵阳金融法院|太原金融法院|石家庄金融法院|呼和浩特金融法院|乌鲁木齐金融法院|兰州金融法院|西宁金融法院|银川金融法院|拉萨金融法院|海口金融法院|三亚金融法院|珠海金融法院|佛山金融法院|东莞金融法院|中山金融法院|惠州金融法院|江门金融法院|肇庆金融法院|汕头金融法院|揭阳金融法院|潮州金融法院|汕尾金融法院|河源金融法院|梅州金融法院|韶关金融法院|清远金融法院|湛江金融法院|茂名金融法院|阳江金融法院|云浮金融法院|北海金融法院|钦州金融法院|贵港金融法院|玉林金融法院|百色金融法院|河池金融法院|来宾金融法院|崇左金融法院|防城港金融法院|梧州金融法院|贺州金融法院|柳州金融法院|桂林金融法院|南宁金融法院|贵州金融法院|毕节金融法院|遵义金融法院|六盘水金融法院|安顺金融法院|黔南金融法院|黔东南金融法院|黔西南金融法院|铜仁金融法院|四川金融法院|成都金融法院|绵阳金融法院|自贡金融法院|攀枝花金融法院|泸州金融法院|德阳金融法院|广元金融法院|遂宁金融法院|内江金融法院|乐山金融法院|南充金融法院|眉山金融法院|宜宾金融法院|广安金融法院|达州金融法院|雅安金融法院|巴中金融法院|资阳金融法院|阿坝金融法院|甘孜金融法院|凉山金融法院|云南金融法院|昆明金融法院|曲靖金融法院|玉溪金融法院|保山金融法院|昭通金融法院|丽江金融法院|普洱金融法院|临沧金融法院|楚雄金融法院|红河金融法院|文山金融法院|西双版纳金融法院|大理金融法院|德宏金融法院|怒江金融法院|迪庆金融法院|陕西金融法院|西安金融法院|宝鸡金融法院|咸阳金融法院|铜川金融法院|渭南金融法院|延安金融法院|汉中金融法院|榆林金融法院|安康金融法院|商洛金融法院|新疆金融法院|乌鲁木齐金融法院|克拉玛依金融法院|吐鲁番金融法院|哈密金融法院|昌吉金融法院|博尔塔拉金融法院|巴音郭楞金融法院|阿克苏金融法院|克孜勒苏金融法院|喀什金融法院|和田金融法院|伊犁金融法院|塔城金融法院|阿勒泰金融法院|石河子金融法院|阿拉尔金融法院|图木舒克金融法院|五家渠金融法院|北屯金融法院|铁门关金融法院|双河金融法院|可克达拉金融法院|昆玉金融法院|胡杨河金融法院|青海金融法院|西宁金融法院|海东金融法院|海北金融法院|黄南金融法院|海南金融法院|果洛金融法院|玉树金融法院|海西金融法院|格尔木金融法院|德令哈金融法院|茫崖金融法院|格尔木金融法院|甘肃金融法院|兰州金融法院|嘉峪关金融法院|金昌金融法院|白银金融法院|天水金融法院|武威金融法院|张掖金融法院|平凉金融法院|酒泉金融法院|庆阳金融法院|定西金融法院|陇南金融法院|临夏金融法院|甘南金融法院|宁夏金融法院|银川金融法院|石嘴山金融法院|吴忠金融法院|固原金融法院|中卫金融法院|内蒙古金融法院|呼和浩特金融法院|包头金融法院|乌海金融法院|赤峰金融法院|通辽金融法院|鄂尔多斯金融法院|呼伦贝尔金融法院|巴彦淖尔金融法院|乌兰察布金融法院|兴安金融法院|锡林郭勒金融法院|阿拉善金融法院|广西金融法院|南宁金融法院|柳州金融法院|桂林金融法院|梧州金融法院|北海金融法院|防城港金融法院|钦州金融法院|贵港金融法院|玉林金融法院|百色金融法院|河池金融法院|来宾金融法院|崇左金融法院|西藏金融法院|拉萨金融法院|日喀则金融法院|昌都金融法院|林芝金融法院|山南金融法院|那曲金融法院|阿里金融法院|宁夏金融法院|银川金融法院|石嘴山金融法院|吴忠金融法院|固原金融法院|中卫金融法院|海南金融法院|海口金融法院|三亚金融法院|三沙金融法院|儋州金融法院|北京金融法院|上海金融法院|天津金融法院|重庆金融法院)",
            // 9. XX市XX法院
            "([\\u4e00-\\u9fa5]{2,12}法院)",
            // 10. 专门的法院名称
            "(?:北京互联网法院|上海金融法院|北京知识产权法院|广州知识产权法院|深圳破产法院|成都铁路法院|军事法院|海事法院|森林法院|上海知识产权法院|成都知识产权法院|杭州知识产权法院|武汉知识产权法院|南京知识产权法院|长沙知识产权法院|西安知识产权法院|郑州知识产权法院|天津知识产权法院|大连知识产权法院|沈阳知识产权法院|长春知识产权法院|哈尔滨知识产权法院|南昌知识产权法院|合肥知识产权法院|福州知识产权法院|济南知识产权法院|青岛知识产权法院|石家庄知识产权法院|太原知识产权法院|呼和浩特知识产权法院|南宁知识产权法院|贵阳知识产权法院|昆明知识产权法院|兰州知识产权法院|西宁知识产权法院|银川知识产权法院|乌鲁木齐知识产权法院|拉萨知识产权法院|海口知识产权法院|宁波知识产权法院|厦门知识产权法院|深圳前海合作区人民法院|深圳前海合作区法院|广州互联网法院|北京互联网法院|上海互联网法院|成都互联网法院|杭州互联网法院|武汉互联网法院|广州互联网法院|南京互联网法院|西安互联网法院|郑州互联网法院|天津互联网法院|重庆互联网法院|沈阳互联网法院|长春互联网法院|哈尔滨互联网法院|济南互联网法院|青岛互联网法院|长沙互联网法院|昆明互联网法院|贵阳互联网法院|南宁互联网法院|福州互联网法院|南昌互联网法院|合肥互联网法院|太原互联网法院|呼和浩特互联网法院|兰州互联网法院|西宁互联网法院|银川互联网法院|乌鲁木齐互联网法院|拉萨互联网法院|海口互联网法院|苏州互联网法院|宁波互联网法院|无锡互联网法院|佛山互联网法院|东莞互联网法院|珠海互联网法院|中山互联网法院|惠州互联网法院|江门互联网法院|肇庆互联网法院|汕头互联网法院|徐州互联网法院|南通互联网法院|常州互联网法院|台州互联网法院|温州互联网法院|嘉兴互联网法院|金华互联网法院|湖州互联网法院|绍兴互联网法院|丽水互联网法院|衢州互联网法院|舟山互联网法院|泰州互联网法院|镇江互联网法院|扬州互联网法院|盐城互联网法院|淮安互联网法院|连云港互联网法院|宿迁互联网法院|徐州铁路法院|南京铁路法院|杭州铁路法院|武汉铁路法院|西安铁路法院|成都铁路法院|重庆铁路法院|太原铁路法院|郑州铁路法院|济南铁路法院|上海铁路法院|北京铁路法院|广州铁路法院|湖南铁路法院|广西铁路法院|云南铁路法院|贵州铁路法院|福州铁路法院|南昌铁路法院|合肥铁路法院|哈尔滨铁路法院|沈阳铁路法院|吉林铁路法院|呼和浩特铁路法院|兰州铁路法院|乌鲁木齐铁路法院|青藏铁路法院|海军法院|空军法院|火箭军法院|军事法院)",
            // 11. 破产法院
            "(?:深圳破产法院|北京破产法院|上海破产法院|广州破产法院|杭州破产法院|重庆破产法院|天津破产法院|成都破产法院|南京破产法院|武汉破产法院|西安破产法院|长沙破产法院|郑州破产法院|济南破产法院|青岛破产法院|沈阳破产法院|大连破产法院|哈尔滨破产法院|长春破产法院|福州破产法院|南昌破产法院|合肥破产法院|贵阳破产法院|昆明破产法院|南宁破产法院|太原破产法院|石家庄破产法院|呼和浩特破产法院|兰州破产法院|西宁破产法院|银川破产法院|乌鲁木齐破产法院|拉萨破产法院|海口破产法院|宁波破产法院|厦门破产法院|深圳破产法院|苏州破产法院|东莞破产法院|佛山破产法院|珠海破产法院|中山破产法院|惠州破产法院|江门破产法院|烟台破产法院|潍坊破产法院|威海破产法院|淄博破产法院|临沂破产法院|济宁破产法院|泰安破产法院|东营破产法院|日照破产法院|滨州破产法院|德州破产法院|聊城破产法院|菏泽破产法院|枣庄破产法院|莱芜破产法院|日照破产法院)",
            // 12. 自贸区法院
            "(?:上海自贸区法院|广东自贸区法院|天津自贸区法院|福建自贸区法院|辽宁自贸区法院|浙江自贸区法院|河南自贸区法院|湖北自贸区法院|重庆自贸区法院|四川自贸区法院|陕西自贸区法院|海南自贸区法院|上海自贸区人民法院|广东自贸区人民法院|天津自贸区人民法院|福建自贸区人民法院|辽宁自贸区人民法院|浙江自贸区人民法院|河南自贸区人民法院|湖北自贸区人民法院|重庆自贸区人民法院|四川自贸区人民法院|陕西自贸区人民法院|海南自贸区人民法院|北京自贸区法院|北京自贸区人民法院|上海浦东新区人民法院|上海自贸区法庭|上海自贸区法院|上海金融法院|上海知识产权法院|上海互联网法院)"
        };

        for (String pStr : patterns) {
            try {
                Pattern p = Pattern.compile(pStr);
                Matcher m = p.matcher(text);
                if (m.find()) {
                    String court = m.group(m.groupCount()).trim();
                    if (court.length() >= 4 && !court.matches(".*[区县市省路街巷]$") && !court.matches(".*[屯村乡]$")) {
                        return court;
                    }
                }
            } catch (Exception ignore) {
            }
        }

        // 从文书末尾提取（此致到文件结尾的区域）
        String endSection = extractEndSection(text);
        if (endSection != null) {
            Pattern p = Pattern.compile("([\\u4e00-\\u9fa5]{2,10}(?:人民法院|中级法院|高级法院|专门法院|铁路法院|法院))");
            Matcher m = p.matcher(endSection);
            if (m.find()) {
                String court = m.group(1).trim();
                if (court.length() >= 4) return court;
            }
        }

        return null;
    }

    private String extractEndSection(String text) {
        if (text == null || text.length() < 100) return null;
        // 提取最后500个字符
        int start = Math.max(0, text.length() - 500);
        return text.substring(start);
    }

    private String extractClaimDescription(String text) {
        if (text == null) return null;

        String[] markers = {"诉讼请求", "请求事项", "仲裁请求", "索赔", "请求", "诉讼请求如下"};
        String[] stoppers = {"事实与理由", "事实和理由", "事实", "理由", "证据", "此致", "具状人", "落款"};

        for (String marker : markers) {
            int markerIdx = text.indexOf(marker);
            if (markerIdx < 0) continue;

            int start = markerIdx + marker.length();
            if (start >= text.length()) continue;

            // 在文本剩余部分找截止边界
            int bestEnd = text.length();
            for (String stopper : stoppers) {
                int stopIdx = text.indexOf(stopper, start);
                if (stopIdx > start && stopIdx < bestEnd) {
                    bestEnd = stopIdx;
                }
            }

            // 跳过到第一个换行（跳过可能的空行或标题行）
            while (start < bestEnd && (text.charAt(start) == '\n' || text.charAt(start) == '\r' || text.charAt(start) == ' ' || text.charAt(start) == '　')) {
                start++;
            }

            String result = text.substring(start, bestEnd).trim();
            result = result.replaceAll("^(?::|：|\\s)*", "").trim();
            result = result.replaceAll("\\n\\s*", "\n").trim();

            // 限制最大长度，防止无限扩展
            if (result.length() >= 5 && result.length() <= 3000) {
                return result;
            }
        }

        // 匹配列表格式 1. xxx 或 一、xxx
        Pattern p = Pattern.compile("(?:1[、.．][^\\n]{5,200}(?:\\n|$)){1,10}");
        Matcher m = p.matcher(text);
        if (m.find()) {
            String result = m.group().trim();
            if (result.length() >= 5) return result;
        }

        p = Pattern.compile("(?:[一二三四五六七八九十][、.．][^\\n]{5,200}(?:\\n|$)){1,10}");
        m = p.matcher(text);
        if (m.find()) {
            String result = m.group().trim();
            if (result.length() >= 5) return result;
        }

        return null;
    }

    private List<Map<String, String>> extractClaimItems(String text) {
        if (text == null) return Collections.emptyList();

        List<Map<String, String>> items = new ArrayList<>();
        String claimText = extractClaimDescription(text);
        if (claimText == null || claimText.isEmpty()) return items;

        // 分割每一项诉讼请求
        String[] lines = claimText.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // 去掉编号
            String content = line.replaceAll("^[1１零一二三四五六七八九十百千万0-9[（(][0-9）)\\]]+[、.．:：\\s]+", "").trim();
            if (content.length() < 3) continue;

            Map<String, String> item = new LinkedHashMap<>();
            item.put("description", content);

            // 分类：金钱/行为/解除/其他
            String category = categorizeClaim(content);
            item.put("category", category);

            // 提取金额（如果有）
            String amountStr = extractAmountFromClaim(content);
            if (amountStr != null) {
                item.put("amount", amountStr);
            }

            items.add(item);
        }

        return items;
    }

    private String categorizeClaim(String claim) {
        if (claim == null) return "其他";
        String lower = claim.toLowerCase();
        if (lower.contains("归还") || lower.contains("返还") || lower.contains("支付") || lower.contains("赔偿") || lower.contains("货款") || lower.contains("欠款") || lower.contains("借款") || lower.contains("本金") || lower.contains("利息")) {
            return "金钱";
        }
        if (lower.contains("解除") || lower.contains("撤销") || lower.contains("确认") || lower.contains("无效")) {
            return "行为";
        }
        if (lower.contains("腾退") || lower.contains("交还") || lower.contains("搬迁") || lower.contains("迁出") || lower.contains("搬离")) {
            return "腾退";
        }
        if (lower.contains("过户") || lower.contains("登记") || lower.contains("办理") || lower.contains("变更")) {
            return "登记";
        }
        return "其他";
    }

    private String extractAmountFromClaim(String claim) {
        if (claim == null) return null;
        Pattern p = Pattern.compile("([\\d,.，]+)\\s*(?:元|万元|亿|人民币|美元|港币)");
        Matcher m = p.matcher(claim);
        if (m.find()) {
            return m.group().trim();
        }
        // 单独的数字+元格式
        p = Pattern.compile("([\\d,.，]+)\\s*元");
        m = p.matcher(claim);
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
            // 清理证据部分（证据跟在事实后面）
            result = cleanEvidenceSection(result);
            if (result.length() > 0) return result;
        }

        return null;
    }

    private String cleanEvidenceSection(String facts) {
        if (facts == null) return null;
        // 如果事实中包含"证据"关键词，截取之前的内容
        int evidenceIdx = facts.indexOf("证据");
        if (evidenceIdx > 10) {
            // 检查是否是独立的证据章节（通常在事实之后）
            String beforeEvidence = facts.substring(0, evidenceIdx);
            // 如果证据前面是句号或换行，说明是事实描述的结束
            if (evidenceIdx > 0 && (beforeEvidence.endsWith("。") || beforeEvidence.endsWith("\n"))) {
                return beforeEvidence.trim();
            }
        }
        // 移除证据编号列表（如"1."、"2."开头的证据描述）
        String cleaned = facts.replaceAll("(?m)^[1１][、.．]\\s*证据[^\\n]{0,50}\\n?", "");
        cleaned = cleaned.replaceAll("(?m)^[一二三四五六七八九十]+[、.．]\\s*证据[^\\n]{0,50}\\n?", "");
        return cleaned.trim();
    }

    private List<Map<String, String>> extractFactsWithSegments(String text) {
        if (text == null) return Collections.emptyList();
        List<Map<String, String>> segments = new ArrayList<>();

        String facts = extractFacts(text);
        if (facts == null || facts.isEmpty()) return segments;

        // 按时间/行为分段
        String[] lines = facts.split("\n");
        StringBuilder currentSegment = new StringBuilder();
        String currentType = "general";

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            String segmentType = detectFactSegmentType(line);
            if (!segmentType.equals(currentType) && currentSegment.length() > 0) {
                Map<String, String> seg = new LinkedHashMap<>();
                seg.put("type", currentType);
                seg.put("content", currentSegment.toString().trim());
                segments.add(seg);
                currentSegment = new StringBuilder();
            }
            currentType = segmentType;
            if (currentSegment.length() > 0) currentSegment.append("\n");
            currentSegment.append(line);
        }

        if (currentSegment.length() > 0) {
            Map<String, String> seg = new LinkedHashMap<>();
            seg.put("type", currentType);
            seg.put("content", currentSegment.toString().trim());
            segments.add(seg);
        }

        return segments;
    }

    private String detectFactSegmentType(String line) {
        if (line == null) return "general";
        // 时间标记
        if (line.matches(".*(?:19|20)\\d{2}年\\d{1,2}月.*") || line.matches(".*\\d{4}[-/.]\\d{1,2}[-/.]\\d{1,2}.*")) {
            return "time";
        }
        // 合同关系
        if (line.contains("签订") || line.contains("合同") || line.contains("协议")) {
            return "contract";
        }
        // 违约行为
        if (line.contains("违约") || line.contains("违反") || line.contains("未按") || line.contains("逾期")) {
            return "breach";
        }
        // 损失
        if (line.contains("损失") || line.contains("造成") || line.contains("致使") || line.contains("导致")) {
            return "damage";
        }
        // 协商
        if (line.contains("协商") || line.contains("调解") || line.contains("催告") || line.contains("通知")) {
            return "negotiation";
        }
        return "general";
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

        // 按优先级匹配各种日期格式
        String[] datePatterns = {
            // 1. YYYY年MM月DD日（中文标准格式）
            "(\\d{4}年\\d{1,2}月\\d{1,2}日?)",
            // 2. YYYY年MM月
            "(\\d{4}年\\d{1,2}月)",
            // 3. YYYY-MM-DD / YYYY/MM/DD / YYYY.MM.DD（标准数字格式）
            "(\\d{4}[年\\-/.]\\d{1,2}[月\\-/.]\\d{1,2}[日]?)",
            // 4. YYYYMMDD（8位数字格式）
            "(\\d{8})",
            // 5. 中文日期：二零二三年一月一日
            "([零一二三四五六七八九]{4}年[零一二三四五六七八九十]{1,2}月[零一二三四五六七八九十]{1,3}日?)",
            // 6. 相对日期：近日/上月/去年等（返回相对描述）
            "([近当往]日?[几上下半\\w]+)",
            // 7. 日期范围：2023年1月至2023年12月
            "(\\d{4}年\\d{1,2}月至\\d{4}年\\d{1,2}月)"
        };

        for (String p : datePatterns) {
            try {
                Pattern pattern = Pattern.compile(p);
                Matcher m = pattern.matcher(text);
                if (m.find()) {
                    String date = m.group(1);
                    date = date.replaceAll("\\s+", "");
                    // 标准化中文数字日期
                    date = normalizeChineseDate(date);
                    if (isValidDate(date)) {
                        return date;
                    }
                }
            } catch (Exception ignore) {}
        }

        return null;
    }

    private String normalizeChineseDate(String date) {
        if (date == null) return null;
        // 转换中文数字到阿拉伯数字
        String[][] chineseDigits = {
            {"零", "0"}, {"一", "1"}, {"二", "2"}, {"三", "3"}, {"四", "4"},
            {"五", "5"}, {"六", "6"}, {"七", "7"}, {"八", "8"}, {"九", "9"}
        };
        for (String[] pair : chineseDigits) {
            date = date.replace(pair[0], pair[1]);
        }
        // 处理十的特殊情况
        date = date.replaceAll("(?<=(\\d))十(?=\\d)", "1");
        date = date.replaceAll("(?<=(\\d))十$", "10");
        date = date.replaceAll("^十(?=\\d)", "1");
        return date;
    }

    private boolean isValidDate(String date) {
        if (date == null || date.isEmpty()) return false;
        // 移除年月日等字符，只保留数字
        String digits = date.replaceAll("[^0-9]", "");
        if (digits.length() < 4) return false;
        // 基本范围检查：年应该在1900-2100之间
        try {
            int year = Integer.parseInt(digits.substring(0, 4));
            if (year < 1900 || year > 2100) return false;
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private String extractBirthDate(String text, String keyword) {
        if (text == null) return null;

        int idx = text.indexOf(keyword);
        if (idx < 0) return null;

        int start = idx;
        int segEnd = Math.min(idx + 300, text.length());
        String segment = text.substring(start, segEnd);

        // 出生日期：YYYY年MM月DD日 / YYYY-MM-DD / YYYYMMDD
        Pattern p = Pattern.compile("(?:出生(?:日期|地)?)[：:\\s]*(?:19|20)(\\d{2}[年\\-/.]\\d{1,2}[月\\-/.]\\d{1,2}[日]?)");
        Matcher m = p.matcher(segment);
        if (m.find()) return m.group(1).replaceAll("\\s", "");

        p = Pattern.compile("(?:19|20)\\d{2}(?:年\\d{1,2}月\\d{1,2}日?|\\d{2}[\\-/.]\\d{1,2}[\\-/.]\\d{1,2})");
        m = p.matcher(segment);
        if (m.find()) return m.group().replaceAll("\\s", "");

        return null;
    }

    private Integer extractAge(String text, String keyword) {
        if (text == null) return null;

        int idx = text.indexOf(keyword);
        if (idx < 0) return null;

        int segEnd = Math.min(idx + 200, text.length());
        String segment = text.substring(idx, segEnd);

        // 年龄：XX岁 / XX周岁 / 年龄XX
        Pattern p = Pattern.compile("(?:年龄|岁数)[：:\\s]*(\\d{1,3})(?:岁|周岁)");
        Matcher m = p.matcher(segment);
        if (m.find()) {
            try {
                int age = Integer.parseInt(m.group(1));
                if (age > 0 && age < 120) return age;
            } catch (Exception ignore) {}
        }

        // 从身份证号反推出生年份
        p = Pattern.compile("\\b\\d{6}(19|20)(\\d{2})\\d{8}[\\dXx]\\b");
        m = p.matcher(segment);
        if (m.find()) {
            int birthYear = Integer.parseInt(m.group(1) + m.group(2));
            int age = java.time.Year.now().getValue() - birthYear;
            if (age > 0 && age < 120) return age;
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

    private String extractCaseType(String text) {
        if (text == null) return null;
        
        String[] caseTypes = {
            "民事", "刑事", "行政", "商事", "知识产权", "海事", "铁路", "军事",
            "民事诉讼", "刑事诉讼", "行政诉讼", "民事纠纷", "合同纠纷", "侵权纠纷",
            "物权纠纷", "债权纠纷", "婚姻家庭", "继承纠纷", "劳动纠纷", "人事纠纷"
        };
        
        for (String type : caseTypes) {
            if (text.contains(type)) {
                return type;
            }
        }
        
        return null;
    }

    private String extractPhone(String text, String keyword) {
        if (text == null || keyword == null) return null;
        
        int idx = text.indexOf(keyword);
        if (idx < 0) return null;
        
        int start = idx;
        int end = Math.min(start + 200, text.length());
        String segment = text.substring(start, end);
        
        // 匹配手机号：1开头的11位数字
        Pattern p = Pattern.compile("(?:电话|手机|联系电话|联系号码)[：:]?\\s*((?:1[3-9]\\d{9}))");
        Matcher m = p.matcher(segment);
        if (m.find()) {
            return m.group(1);
        }
        
        // 匹配固定电话：区号-号码
        p = Pattern.compile("(?:电话|手机|联系电话)[：:]?\\s*((?:0\\d{2,3}[-]?)?\\d{7,8})");
        m = p.matcher(segment);
        if (m.find()) {
            return m.group(1);
        }
        
        return null;
    }

    private String extractIdCard(String text, String keyword) {
        if (text == null || keyword == null) return null;
        
        int idx = text.indexOf(keyword);
        if (idx < 0) return null;
        
        int start = idx;
        int end = Math.min(start + 250, text.length());
        String segment = text.substring(start, end);
        
        // 匹配身份证号：15位或18位
        Pattern p = Pattern.compile("(?:身份证(?:号)?|证件号)[：:]?\\s*([1-9]\\d{5}(?:\\d{2}[0-9X]){2}[0-9X])");
        Matcher m = p.matcher(segment);
        if (m.find()) {
            return m.group(1);
        }
        
        // 直接在文本中查找身份证号格式
        p = Pattern.compile("\\b([1-9]\\d{5}\\d{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[12]\\d|3[01])\\d{3}[0-9X])\\b");
        m = p.matcher(segment);
        if (m.find()) {
            return m.group(1);
        }
        
        return null;
    }

    private String extractClaimBasis(String text) {
        if (text == null) return null;
        
        // 匹配"依据"或"根据"相关法条
        Pattern p = Pattern.compile("(?:依据|根据)[：:]?\\s*《?([^《》，,。；\\n]{5,100})》?");
        Matcher m = p.matcher(text);
        if (m.find()) {
            String basis = m.group(1).trim();
            // 清理多余内容
            basis = basis.replaceAll("\\s+", " ");
            if (basis.length() >= 5) {
                return basis;
            }
        }
        
        // 匹配法条引用格式
        p = Pattern.compile("《([^《》]{2,30})法》第(\\d+)条");
        StringBuilder laws = new StringBuilder();
        m = p.matcher(text);
        while (m.find()) {
            if (laws.length() > 0) laws.append("、");
            laws.append(m.group(1)).append("第").append(m.group(2)).append("条");
        }
        if (laws.length() > 0) {
            return laws.toString();
        }
        
        return null;
    }

    private String extractEvidence(String text) {
        if (text == null) return null;
        
        // 匹配"证据"部分
        Pattern p = Pattern.compile("(?:证据|证据材料|证据清单)[：:]?[\\s\\S]{0,50}(?=(?:事实|理由|此致|$))[\\s\\S]{10,1000}");
        Matcher m = p.matcher(text);
        if (m.find()) {
            String evidence = m.group().trim();
            // 清理格式
            evidence = evidence.replaceAll("^(?:1[、.．]|一[、.．])", "").trim();
            if (evidence.length() >= 5) {
                return evidence;
            }
        }
        
        // 匹配证据列表
        p = Pattern.compile("(?:1[、.．][\\s\\S]{5,200}){1,5}");
        m = p.matcher(text);
        if (m.find()) {
            String evidence = m.group().trim();
            if (evidence.length() >= 10) {
                return evidence;
            }
        }
        
        return null;
    }

    private String extractUnifiedSocialCreditCode(String text) {
        if (text == null) return null;

        // 统一社会信用代码正则：第一位非0，第二位只能是特定字母，后面14位数字，最后一位是校验位（数字或X）
        String CODE_PATTERN = "(1[1-5]|2[12]|3[1-3]|4[1-6]|5[12]|6[1-4]|7[1-4]|8[1-3]|9[1])[1-9A-HJ-NPQRTUWXY]\\d{6}[1-9A-HJ-NPQRTUWXY][0-9A-Z]";

        // 1. 带标注的格式
        Pattern p = Pattern.compile("(?:统一社会信用代码)[：:\\s]*(" + CODE_PATTERN + ")", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        if (m.find()) {
            String code = m.group(1).trim().toUpperCase();
            String validated = validateAndFixCreditCode(code);
            if (validated != null) return validated;
        }

        // 2. OCR容错扫描：允许O代替0、I代替1等
        String ocrFixed = fixOcrCreditCode(text);
        if (!ocrFixed.equals(text)) {
            p = Pattern.compile("\\b(" + CODE_PATTERN + ")\\b", Pattern.CASE_INSENSITIVE);
            m = p.matcher(ocrFixed);
            while (m.find()) {
                String code = m.group(1).trim().toUpperCase();
                String validated = validateAndFixCreditCode(code);
                if (validated != null) return validated;
            }
        }

        // 3. 直接全文扫描18位严格格式
        p = Pattern.compile("\\b(" + CODE_PATTERN + ")\\b", Pattern.CASE_INSENSITIVE);
        m = p.matcher(text);
        if (m.find()) {
            String code = m.group(1).trim().toUpperCase();
            String validated = validateAndFixCreditCode(code);
            if (validated != null) return validated;
        }

        // 4. 宽松匹配：冒号或空格后的18位
        p = Pattern.compile("[：:\\s](" + CODE_PATTERN + ")", Pattern.CASE_INSENSITIVE);
        m = p.matcher(text);
        if (m.find()) {
            String code = m.group(1).trim().toUpperCase();
            String validated = validateAndFixCreditCode(code);
            if (validated != null) return validated;
        }

        return null;
    }

    private String fixOcrCreditCode(String text) {
        if (text == null) return null;
        // 常见OCR错误：O->0, I->1, l->1, S->5, Z->2, Q->0
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c == 'O' || c == 'o') sb.append('0');
            else if (c == 'I' || c == 'l') sb.append('1');
            else if (c == 'S' || c == 's') sb.append('5');
            else if (c == 'Z' || c == 'z') sb.append('2');
            else sb.append(c);
        }
        return sb.toString();
    }

    private String validateAndFixCreditCode(String code) {
        if (code == null || code.length() != 18) return null;
        code = code.toUpperCase();

        // 检查字符集是否在允许范围内
        String allowed = "0123456789ABCDEFGHJKLMNPQRTUWXY";
        for (char c : code.toCharArray()) {
            if (allowed.indexOf(c) < 0) return null;
        }

        // 校验位验证
        int[] wi = {1, 3, 9, 27, 19, 26, 16, 17, 20, 29, 25, 13, 8, 24, 10, 30, 28};
        String baseCode = "0123456789ABCDEFGHJKLMNPQRTUWXY";
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            int idx = baseCode.indexOf(code.charAt(i));
            if (idx < 0) return null;
            sum += idx * wi[i];
        }
        int mod = sum % 31;
        char expected = baseCode.charAt((31 - mod) % 31);
        char actual = code.charAt(17);

        // 校验位不严格匹配时，若OCR可能出错则尝试修复
        if (expected != actual) {
            // 如果实际是X而期望不是，或实际是0而期望不是，可能OCR问题，尝试接受
            if ((actual == 'X' && expected != 'X') || actual == '0') {
                // 校验位可能有问题，但不拒绝，接受原值
            } else {
                return null; // 校验失败
            }
        }
        return code;
    }

    private String extractLegalRepresentative(String text) {
        if (text == null) return null;
        
        // 匹配"法定代表人：XXX"或"法人代表：XXX"或"法定代表人：XXX（职务）"
        Pattern p = Pattern.compile("(?:法定代表人|法人代表|法人|负责人)[：:]\\s*([\\u4e00-\\u9fa5]{2,6})(?:[（\\(][\\u4e00-\\u9fa5]{2,10}[）\\)])?");
        Matcher m = p.matcher(text);
        if (m.find()) {
            return m.group(1);
        }
        
        return null;
    }

    private String extractPosition(String text) {
        if (text == null) return null;
        
        // 匹配"职务：XXX"或"职位：XXX"或"担任职务：XXX"
        Pattern p = Pattern.compile("(?:职务|职位|担任职务|岗位)[：:]\\s*([\\u4e00-\\u9fa5A-Za-z0-9]{2,20})");
        Matcher m = p.matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        
        // 从法定代表人后面的括号中提取职务
        p = Pattern.compile("(?:法定代表人|法人代表|法人)[：:]\\s*[\\u4e00-\\u9fa5]{2,6}[（\\(]([\\u4e00-\\u9fa5]{2,10})[）\\)]");
        m = p.matcher(text);
        if (m.find()) {
            return m.group(1);
        }
        
        return null;
    }

    private String extractResidenceAddress(String text) {
        if (text == null) return null;
        
        // 匹配"住所地：XXX"或"住所：XXX"或"地址：XXX"
        Pattern p = Pattern.compile("(?:住所地|住所|注册地址|经营地址)[：:]\\s*([\\u4e00-\\u9fa5A-Za-z0-9省市区县路街道号弄室栋楼\\-，,]{10,150})");
        Matcher m = p.matcher(text);
        if (m.find()) {
            String addr = m.group(1).trim();
            // 截取到换行或特定分隔符
            int lineEnd = addr.indexOf('\n');
            if (lineEnd > 0) addr = addr.substring(0, lineEnd);
            int commaEnd = addr.indexOf('，');
            if (commaEnd > 10 && commaEnd < 100) addr = addr.substring(0, commaEnd);
            return addr.trim();
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

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= '０' && c <= '９') {
                sb.append((char)(c - '０' + '0'));
            } else {
                sb.append(c);
            }
        }
        text = sb.toString();

        text = text.replace("，", ".").replace(",", ".");

        // 策略1：处理金额区间 10万-20万 / 10-20万元，取第一个数
        Pattern rangeP = Pattern.compile("[万一亿]+(?:[\\d][\u4e00-\u9fa5]*-)?[\\d]+[\u4e00-\u9fa5]*");
        Matcher rangeM = rangeP.matcher(text);
        if (rangeM.find()) {
            String range = rangeM.group();
            int dashIdx = range.indexOf('-');
            if (dashIdx > 0) {
                String first = range.substring(0, dashIdx);
                BigDecimal bd = parseChineseNumber(first);
                if (bd != null && bd.signum() > 0) return bd;
            }
        }

        // 策略2：处理金额范围 "10万元至20万元" 格式
        Pattern range2P = Pattern.compile("([\\d.]+)(?:万元|万|元)(?:至|-)([\\d.]+)(?:万元|万|元)");
        Matcher range2M = range2P.matcher(text);
        if (range2M.find()) {
            try {
                String first = range2M.group(1).replaceAll("[^\\d.]", "");
                if (!first.isEmpty()) {
                    BigDecimal bd = new BigDecimal(first);
                    if (text.contains("万元") || text.contains("万")) bd = bd.multiply(new BigDecimal("10000"));
                    if (bd.signum() > 0) return bd;
                }
            } catch (Exception ignore) {}
        }

        // 策略3：处理中文大写数字（拾、贰、叁等）
        BigDecimal chineseResult = parseChineseNumber(text);
        if (chineseResult != null && chineseResult.signum() > 0) {
            return chineseResult;
        }

        // 策略4：标准数字模式
        String[] patterns = new String[] {
            // 金额标注 + 数字 + 元
            "(?:金额|诉请金额|诉讼请求金额|标的额|欠款|借款|本金|货款|赔偿金额|索赔)[为于约是]?\\s*[人民币]?\\s*([\\d.]{1,20})(?:\\s*元)?",
            // 数字 + 万元/万
            "([\\d.]{1,15})\\s*(?:万元|万)",
            // 数字 + 亿元/亿
            "([\\d.]{1,15})\\s*(?:亿元|亿)",
            // 数字 + 元
            "([\\d.]{1,15})\\s*元",
            // "约"或"约计" + 数字
            "(?:约|约计|大约)[\\s:：]*([\\d.]{1,15})",
            // "余元"格式：3万余元（取3万）
            "([\\d.]+)(?:余)万元",
            // 仅数字（作为兜底）
            "\\b([\\d.]{1,15})\\b"
        };

        for (String p : patterns) {
            try {
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(p);
                java.util.regex.Matcher m = pattern.matcher(text);
                if (m.find()) {
                    String num = m.group(1).trim();
                    num = num.replaceAll("[^\\d.]", "");
                    if (num.isEmpty() || num.equals(".")) continue;
                    BigDecimal bd = new BigDecimal(num);
                    if (bd == null || bd.signum() <= 0) continue;
                    if (p.contains("万元") || (m.group().contains("万") && !m.group().contains("亿元"))) {
                        bd = bd.multiply(new BigDecimal("10000"));
                    } else if (p.contains("亿元") || m.group().contains("亿元") || m.group().contains("亿")) {
                        bd = bd.multiply(new BigDecimal("100000000"));
                    }
                    if (bd.signum() > 0 && bd.compareTo(new BigDecimal("100000000000")) <= 0) {
                        return bd;
                    }
                }
            } catch (Exception ignore) {}
        }
        return null;
    }

    private BigDecimal parseChineseNumber(String text) {
        if (text == null || text.isEmpty()) return null;
        // 中文数字映射
        String[] chineseDigits = {"零", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十", "百", "千", "万", "亿"};
        int[] values = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 100, 1000, 10000, 100000000};

        String numText = text.trim();
        // 提取数字部分
        Pattern p = Pattern.compile("[一二三四五六七八九十百千万亿零〇0-9]{2,15}[个章节]?(?:元|万元|万|亿元|亿)?");
        Matcher m = p.matcher(numText);
        if (!m.find()) return null;

        String chineseNum = m.group();
        chineseNum = chineseNum.replaceAll("[个章节]", "");

        long result = 0;
        long temp = 0;
        long unit = 1;
        boolean hasUnit = false;

        for (int i = chineseNum.length() - 1; i >= 0; i--) {
            char c = chineseNum.charAt(i);
            int idx = -1;
            for (int j = 0; j < chineseDigits.length; j++) {
                if (String.valueOf(c).equals(chineseDigits[j])) {
                    idx = j;
                    break;
                }
            }
            if (idx >= 10) { // 是单位（十百千万亿）
                unit = values[idx];
                hasUnit = true;
                if (idx == 10) unit = 10;
                else if (idx == 11) unit = 100;
                else if (idx == 12) unit = 1000;
                else if (idx == 13) unit = 10000;
                else if (idx == 14) unit = 100000000;
                result += (temp == 0 ? 1 : temp) * unit;
                temp = 0;
                hasUnit = false;
            } else if (idx >= 0 && idx <= 9) {
                if (unit == 1 && !hasUnit) {
                    temp = temp * 10 + values[idx];
                } else {
                    result += values[idx] * unit;
                    unit = 1;
                }
            }
        }
        result += temp * unit;

        if (result > 0) {
            // 检查是否有万元/亿元后缀
            if (text.contains("亿元")) result *= 100000000;
            else if (text.contains("万")) result *= 10000;
            return new BigDecimal(result);
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