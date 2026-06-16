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