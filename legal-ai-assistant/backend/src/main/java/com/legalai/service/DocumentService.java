package com.legalai.service;

import com.legalai.dto.*;
import com.legalai.util.IdGenerator;
import com.legalai.util.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DocumentService {
    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

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

        TEMPLATES.put("labor_contract", new DocumentTemplate(
            "劳动合同",
            "劳动人事",
            true,
            Arrays.asList("employerName", "employeeName", "workContent", "salary", "startDate"),
            Arrays.asList("《中华人民共和国劳动合同法》第十条"),
            "labor_contract"
        ));

        TEMPLATES.put("confidentiality_agreement", new DocumentTemplate(
            "保密协议",
            "劳动人事",
            true,
            Arrays.asList("companyName", "employeeName", "confidentialPeriod", "breachPenalty"),
            Arrays.asList("《中华人民共和国劳动合同法》第二十三条"),
            "confidentiality_agreement"
        ));

        TEMPLATES.put("lawyer_letter", new DocumentTemplate(
            "律师函",
            "商业函件",
            true,
            Arrays.asList("senderName", "recipientName", "factDescription", "legalBasis"),
            Arrays.asList("《中华人民共和国民法典》第五百七十七条"),
            "lawyer_letter"
        ));

        TEMPLATES.put("payment_demand", new DocumentTemplate(
            "催款函",
            "商业函件",
            true,
            Arrays.asList("creditorName", "debtorName", "amount", "overdueDays"),
            Arrays.asList("《中华人民共和国民法典》第六百七十六条"),
            "payment_demand"
        ));
    }

    public DocumentDraftResponse draftDocument(DocumentDraftRequest request) {
        log.info("文书起草请求: templateCode={}", request.getTemplateCode());

        validateRequest(request);

        DocumentTemplate template = TEMPLATES.get(request.getTemplateCode());
        if (template == null) {
            throw new IllegalArgumentException("不支持的文书模板: " + request.getTemplateCode());
        }

        String content = generateDocumentContent(template, request);
        String riskPrompt = generateRiskPrompt(template, request);
        String disclaimer = generateDisclaimer(template, request.getCaseData());

        DocumentDraftResponse response = new DocumentDraftResponse();
        response.setDocumentContent(content);
        response.setRiskPrompt(riskPrompt);
        response.setDisclaimer(disclaimer);
        response.setReferencedLaws(template.referencedLaws);

        return response;
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

    private void validateRequest(DocumentDraftRequest request) {
        if (request.getTemplateCode() == null || request.getTemplateCode().isEmpty()) {
            throw new IllegalArgumentException("模板代码不能为空");
        }
    }

    private String generateDocumentContent(DocumentTemplate template, DocumentDraftRequest request) {
        StringBuilder sb = new StringBuilder();
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));

        switch (template.code) {
            case "civil_petition" -> sb.append(generateCivilPetition(request));
            case "civil_defense" -> sb.append(generateCivilDefense(request));
            case "civil_appeal" -> sb.append(generateCivilAppeal(request));
            case "labor_contract" -> sb.append(generateLaborContract(request));
            case "confidentiality_agreement" -> sb.append(generateConfidentialityAgreement(request));
            case "lawyer_letter" -> sb.append(generateLawyerLetter(request));
            case "payment_demand" -> sb.append(generatePaymentDemand(request));
            default -> sb.append("通用法律文书\n\n文书内容...");
        }

        return sb.toString();
    }

    private String generateCivilPetition(DocumentDraftRequest request) {
        DocumentDraftRequest.DocumentData data = request.getCaseData();
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));

        return String.format("""
            民事起诉状

            原告：%s，住%s。

            被告：%s，住%s。

            诉讼请求：
            1. 判令被告支付欠款人民币%s元；
            2. 判令被告支付利息人民币%s元；
            3. 判令被告承担本案诉讼费用。

            事实与理由：
            被告因%s，于%s向原告借款人民币%s元，约定于三个月内归还。
            借款到期后，被告未按约定归还欠款，经原告多次催要，被告仍拒不归还。
            为维护原告合法权益，特向贵院提起诉讼，请求依法支持原告的诉讼请求。

            此致
            %s人民法院

            具状人：%s
            %s
            """,
            data != null && data.getPlaintiffName() != null ? data.getPlaintiffName() : "李四",
            data != null && data.getPlaintiffAddress() != null ? data.getPlaintiffAddress() : "北京市朝阳区",
            data != null && data.getDefendantName() != null ? data.getDefendantName() : "王五",
            data != null && data.getDefendantAddress() != null ? data.getDefendantAddress() : "上海市浦东新区",
            data != null && data.getClaimAmount() != null ? data.getClaimAmount() : "100000",
            "按实际计算",
            data != null && data.getClaimDescription() != null ? data.getClaimDescription() : "借款合同纠纷",
            date,
            data != null && data.getClaimAmount() != null ? data.getClaimAmount() : "100000",
            data != null && data.getCourtName() != null ? data.getCourtName() : "北京市朝阳区",
            data != null && data.getPlaintiffName() != null ? data.getPlaintiffName() : "李四",
            date
        );
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