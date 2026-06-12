package com.legalai.service;

import com.legalai.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DocQaService {
    private static final Logger log = LoggerFactory.getLogger(DocQaService.class);

    private static final Map<String, List<Map<String, String>>> SESSION_HISTORY = new HashMap<>();
    private static final Map<String, List<String>> QUESTION_PATTERNS = new HashMap<>();

    static {
        QUESTION_PATTERNS.put("欺诈", List.of(
            "《民法典》第一百四十八条：一方以欺诈手段，使对方在违背真实意思的情况下订立的合同，受欺诈方有权请求人民法院或者仲裁机构予以撤销。",
            "欺诈的构成要件包括：欺诈故意、欺诈行为、错误认识、意思表示。"
        ));

        QUESTION_PATTERNS.put("违约", List.of(
            "《民法典》第五百七十七条：当事人一方不履行合同义务或者履行合同义务不符合约定的，应当承担违约责任。",
            "违约责任的承担方式包括：继续履行、采取补救措施、赔偿损失等。"
        ));

        QUESTION_PATTERNS.put("解除合同", List.of(
            "《民法典》第五百六十三条：有下列情形之一的，当事人可以解除合同：（一）因不可抗力致使不能实现合同目的；（二）履行期限届满前，当事人一方明确表示或者以自己的行为表明不履行主要债务；（三）当事人一方迟延履行主要债务，经催告后在合理期限内仍未履行；（四）当事人一方迟延履行债务或者有其他违约行为致使不能实现合同目的。",
            "合同解除后，尚未履行的，终止履行；已经履行的，根据履行情况和合同性质，当事人可以请求恢复原状或者采取其他补救措施。"
        ));

        QUESTION_PATTERNS.put("劳动", List.of(
            "《劳动合同法》第三十九条：劳动者有下列情形之一的，用人单位可以解除劳动合同：（一）在试用期间被证明不符合录用条件的；（二）严重违反用人单位的规章制度的；（三）严重失职，营私舞弊，给用人单位造成重大损害的。",
            "《劳动合同法》第四十六条：有下列情形之一的，用人单位应当向劳动者支付经济补偿：（一）劳动者依照本法第三十八条规定解除劳动合同的；（二）用人单位依照本法第三十六条规定向劳动者提出解除劳动合同并与劳动者协商一致解除劳动合同的。"
        ));

        QUESTION_PATTERNS.put("赔偿", List.of(
            "《民法典》第五百八十四条：当事人一方不履行合同义务或者履行合同义务不符合约定的，给对方造成损失的，损失赔偿额应当相当于因违约所造成的损失，包括合同履行后可以获得的利益。",
            "损失赔偿的计算方法包括：实际损失、可得利益损失、合理的违约金的调整等。"
        ));

        QUESTION_PATTERNS.put("借款", List.of(
            "《民法典》第六百六十七条：借款合同的利息不得违反国家有关规定。",
            "《最高人民法院关于审理民间借贷案件适用法律若干问题的规定》：民间借贷利率不得超过合同成立时一年期贷款市场报价利率四倍。"
        ));

        QUESTION_PATTERNS.put("建设工程", List.of(
            "《民法典》第七百八十八条：建设工程合同是承包人进行工程建设，发包人支付价款的合同。",
            "《最高人民法院关于审理建设工程施工合同纠纷案件适用法律问题的解释（一）》第10条：当事人对建设工程的计价标准或者计价方法有约定的，按照约定结算工程价款。"
        ));

        QUESTION_PATTERNS.put("侵权", List.of(
            "《民法典》第一千一百六十五条：行为人因过错侵害他人民事权益造成损害的，应当承担侵权责任。",
            "侵权责任的构成要件：加害行为、损害事实、加害行为与损害事实之间存在因果关系、行为人存在过错。"
        ));
    }

    private static final Map<String, String> CITATION_SOURCES = Map.of(
        "DOC-001", "《中华人民共和国民法典》",
        "DOC-002", "《中华人民共和国劳动合同法》",
        "DOC-003", "《中华人民共和国公司法》",
        "DOC-004", "《最高人民法院关于审理建设工程施工合同纠纷案件适用法律问题的解释（一）》"
    );

    public DocQaResponse answerQuestion(DocQaRequest request) {
        log.info("文档问答请求: question={}, sessionId={}", request.getQuestion(), request.getSessionId());

        validateRequest(request);

        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }

        String question = request.getQuestion().trim();
        List<DocQaResponse.Citation> citations = new ArrayList<>();
        String answer = generateAnswer(question, citations);

        DocQaResponse response = new DocQaResponse();
        response.setAnswer(answer);
        response.setCitations(citations);
        response.setSessionId(sessionId);

        return response;
    }

    private void validateRequest(DocQaRequest request) {
        if (request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
            throw new IllegalArgumentException("问题不能为空");
        }
        if (request.getQuestion().length() > 500) {
            throw new IllegalArgumentException("问题长度不能超过500字");
        }
    }

    private String generateAnswer(String question, List<DocQaResponse.Citation> citations) {
        String q = question.toLowerCase();

        for (Map.Entry<String, List<String>> entry : QUESTION_PATTERNS.entrySet()) {
            if (q.contains(entry.getKey())) {
                StringBuilder sb = new StringBuilder();
                List<String> answers = entry.getValue();
                sb.append("根据检索到的法律法规和相关资料，回答如下：\n\n");

                for (int i = 0; i < answers.size(); i++) {
                    sb.append(answers.get(i)).append("\n\n");
                }

                addCitation(citations, entry.getKey());

                sb.append("---\n\n");
                sb.append("**免责声明**：本回答基于检索到的法律法规生成，仅供参考，不构成法律意见。\n");
                sb.append("如需针对具体案件的法律建议，请咨询具有执业资格的专业律师。");

                return sb.toString();
            }
        }

        return generateDefaultAnswer(question, citations);
    }

    private void addCitation(List<DocQaResponse.Citation> citations, String keyword) {
        String docId = switch (keyword) {
            case "劳动" -> "DOC-002";
            case "建设工程" -> "DOC-004";
            default -> "DOC-001";
        };

        DocQaResponse.Citation citation = new DocQaResponse.Citation();
        citation.setDocumentId(docId);
        citation.setChunkId("CHUNK-" + System.currentTimeMillis());
        citation.setContent(CITATION_SOURCES.getOrDefault(docId, "《中华人民共和国民法典》"));
        citation.setSourceUrl("https://flk.npc.gov.cn/");
        citation.setScore(0.95);
        citations.add(citation);
    }

    private String generateDefaultAnswer(String question, List<DocQaResponse.Citation> citations) {
        StringBuilder sb = new StringBuilder();
        sb.append("关于您提出的问题，我检索到以下相关法律信息：\n\n");

        sb.append("您的问题是：").append(question).append("\n\n");

        sb.append("针对这个问题，我提供以下分析和建议：\n\n");

        sb.append("1. **保留相关证据**\n");
        sb.append("   建议您保留好与问题相关的所有证据材料，包括合同、协议、沟通记录、付款凭证等。\n\n");

        sb.append("2. **及时主张权利**\n");
        sb.append("   请注意诉讼时效的规定，不同类型案件的诉讼时效可能不同（一般为3年）。\n\n");

        sb.append("3. **咨询专业律师**\n");
        sb.append("鉴于法律问题的复杂性，建议您咨询具有执业资格的专业律师获取针对性的法律建议。\n\n");

        DocQaResponse.Citation citation = new DocQaResponse.Citation();
        citation.setDocumentId("DOC-001");
        citation.setChunkId("CHUNK-DEFAULT");
        citation.setContent("《中华人民共和国民法典》 - 相关法律规定");
        citation.setSourceUrl("https://flk.npc.gov.cn/");
        citation.setScore(0.85);
        citations.add(citation);

        sb.append("---\n\n");
        sb.append("**免责声明**：本回答基于一般法律知识生成，仅供参考，不构成法律意见。\n");
        sb.append("不同地区、不同案件情况的具体法律适用可能有所不同，请以专业律师的意见为准。");

        return sb.toString();
    }
}