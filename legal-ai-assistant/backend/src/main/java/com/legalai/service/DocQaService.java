package com.legalai.service;

import com.legalai.dto.*;
import com.legalai.model.ChatMessage;
import com.legalai.repository.ChatMessageMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DocQaService {
    private static final Logger log = LoggerFactory.getLogger(DocQaService.class);

    @Value("${mock.enabled:true}")
    private boolean mockEnabled;

    @Autowired
    private AIService aiService;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

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
        log.info("文档问答请求: question={}, sessionId={}, kbId={}",
            request.getQuestion(), request.getSessionId(), request.getKbId());

        validateRequest(request);

        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }

        List<ChatMessage> history = getSessionHistory(sessionId);
        String conversationContext = buildConversationContext(history);

        persistMessage(sessionId, "user", request.getQuestion(), history.size());

        List<DocQaResponse.Citation> citations = new ArrayList<>();
        List<String> contextChunks = new ArrayList<>();

        if (request.getKbId() != null) {
            contextChunks = retrieveFromKnowledgeBase(request);
        }

        String answer;
        if (mockEnabled) {
            answer = generateMockAnswer(request.getQuestion(), citations, contextChunks, conversationContext);
        } else {
            answer = generateAIAnswer(request.getQuestion(), citations, contextChunks, conversationContext);
        }

        persistMessage(sessionId, "assistant", answer, history.size() + 1);

        DocQaResponse response = new DocQaResponse();
        response.setAnswer(answer);
        response.setCitations(citations);
        response.setSessionId(sessionId);

        return response;
    }

    public Flux<String> answerQuestionStream(DocQaRequest request) {
        log.info("文档问答流式请求: question={}, sessionId={}, kbId={}",
            request.getQuestion(), request.getSessionId(), request.getKbId());

        validateRequest(request);

        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }

        List<ChatMessage> history = getSessionHistory(sessionId);
        String conversationContext = buildConversationContext(history);

        persistMessage(sessionId, "user", request.getQuestion(), history.size());

        List<DocQaResponse.Citation> citations = new ArrayList<>();
        List<String> contextChunks = new ArrayList<>();

        if (request.getKbId() != null) {
            contextChunks = retrieveFromKnowledgeBase(request);
        }

        final String finalSessionId = sessionId;
        final List<String> finalContextChunks = contextChunks;
        final int historySize = history.size();

        if (mockEnabled) {
            String mockAnswer = generateMockAnswer(request.getQuestion(), citations, contextChunks, conversationContext);
            persistMessage(finalSessionId, "assistant", mockAnswer, historySize);

            return Flux.just(mockAnswer);
        } else {
            return Flux.create(emitter -> {
                try {
                    StringBuilder fullAnswer = new StringBuilder();

                    aiService.chatStream(
                        buildPrompt(request.getQuestion(), finalContextChunks, conversationContext),
                        chunk -> {
                            fullAnswer.append(chunk);
                            emitter.next("data: " + chunk + "\n\n");
                        },
                        emitter::isCancelled
                    );

                    emitter.complete();

                    persistMessage(finalSessionId, "assistant", fullAnswer.toString(), historySize);
                } catch (IOException e) {
                    log.error("流式AI服务调用失败: {}", e.getMessage());
                    String fallbackAnswer = generateMockAnswer(request.getQuestion(), citations, finalContextChunks, conversationContext);
                    persistMessage(finalSessionId, "assistant", fallbackAnswer, historySize);
                    emitter.next(fallbackAnswer);
                    emitter.complete();
                }
            });
        }
    }

    private void validateRequest(DocQaRequest request) {
        if (request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
            throw new IllegalArgumentException("问题不能为空");
        }
        if (request.getQuestion().length() > 500) {
            throw new IllegalArgumentException("问题长度不能超过500字");
        }
    }

    private String buildConversationContext(List<ChatMessage> history) {
        if (history.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("【对话历史】\n");
        for (ChatMessage msg : history) {
            String role = "user".equals(msg.getRole()) ? "用户" : "助手";
            sb.append(String.format("%s: %s\n", role, msg.getContent()));
        }
        sb.append("---\n\n");
        return sb.toString();
    }

    private List<String> retrieveFromKnowledgeBase(DocQaRequest request) {
        List<LegalSearchResponse.SearchResultItem> chunks = knowledgeBaseService.searchInKnowledgeBase(
            request.getKbId(),
            request.getQuestion(),
            request.getTopK() != null ? request.getTopK() : 5
        );

        return chunks.stream()
            .map(LegalSearchResponse.SearchResultItem::getContent)
            .collect(Collectors.toList());
    }

    private String generateAIAnswer(String question, List<DocQaResponse.Citation> citations,
                                      List<String> contextChunks, String conversationContext) {
        List<Map<String, String>> messages = buildMessages(question, contextChunks, conversationContext);
        try {
            String answer = aiService.chatWithMessages(messages);
            addCitationsFromChunks(citations, contextChunks);
            return answer + "\n\n---\n\n**免责声明**：本回答由AI生成，仅供参考，不构成法律意见。";
        } catch (IOException e) {
            log.error("AI服务调用失败: {}", e.getMessage());
            return generateMockAnswer(question, citations, contextChunks, conversationContext);
        }
    }

    private List<Map<String, String>> buildMessages(String question, List<String> contextChunks, String conversationContext) {
        List<Map<String, String>> messages = new ArrayList<>();

        messages.add(Map.of(
            "role", "system",
            "content", "你是一个专业的法律助手，专注于中国法律法规的解读。你的任务是根据用户提供的上下文信息（包括对话历史和参考文档）回答用户的问题。\n\n" +
                      "回答要求：\n" +
                      "1. 基于提供的上下文信息给出专业、准确的回答\n" +
                      "2. 如果涉及具体法条，请标注来源（法规名称+条款编号）\n" +
                      "3. 回答应结构清晰，语言严谨\n" +
                      "4. 如上下文信息不足以回答，明确说明\n\n" +
                      "免责声明：你的回答仅供参考，不构成正式法律意见。"
        ));

        if (!conversationContext.isEmpty() || !contextChunks.isEmpty()) {
            StringBuilder context = new StringBuilder();
            if (!conversationContext.isEmpty()) {
                context.append("【对话历史】\n").append(conversationContext);
            }
            if (!contextChunks.isEmpty()) {
                context.append("\n【参考文档】\n");
                for (int i = 0; i < contextChunks.size(); i++) {
                    context.append(String.format("[%d] %s\n\n", i + 1, contextChunks.get(i)));
                }
            }
            messages.add(Map.of("role", "system", "content", context.toString()));
        }

        messages.add(Map.of("role", "user", "content", question));

        return messages;
    }

    private String buildPrompt(String question, List<String> contextChunks, String conversationContext) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个专业的法律助手，专注于中国法律法规的解读。请根据提供的上下文信息回答用户的问题。\n\n");

        if (!conversationContext.isEmpty()) {
            sb.append(conversationContext);
        }

        if (!contextChunks.isEmpty()) {
            sb.append("【参考上下文】\n");
            for (int i = 0; i < contextChunks.size(); i++) {
                sb.append(String.format("[%d] %s\n\n", i + 1, contextChunks.get(i)));
            }
            sb.append("---\n\n");
        }

        sb.append("【用户问题】\n");
        sb.append(question).append("\n\n");

        sb.append("请根据上述上下文给出专业、准确的回答。如果涉及具体法条，请标注来源。");

        return sb.toString();
    }

    private String generateMockAnswer(String question, List<DocQaResponse.Citation> citations,
                                      List<String> contextChunks, String conversationContext) {
        String q = question.toLowerCase();

        for (Map.Entry<String, List<String>> entry : QUESTION_PATTERNS.entrySet()) {
            if (q.contains(entry.getKey())) {
                StringBuilder sb = new StringBuilder();
                List<String> answers = entry.getValue();
                sb.append("根据检索到的法律法规和相关资料，回答如下：\n\n");

                for (int i = 0; i < answers.size(); i++) {
                    sb.append(answers.get(i)).append("\n\n");
                }

                addCitationsFromPatterns(citations, entry.getKey());

                sb.append("---\n\n");
                sb.append("**免责声明**：本回答基于检索到的法律法规生成，仅供参考，不构成法律意见。\n");
                sb.append("如需针对具体案件的法律建议，请咨询具有执业资格的专业律师。");

                return sb.toString();
            }
        }

        return generateDefaultAnswer(question, citations, contextChunks);
    }

    private void addCitationsFromChunks(List<DocQaResponse.Citation> citations, List<String> chunks) {
        for (int i = 0; i < chunks.size(); i++) {
            DocQaResponse.Citation citation = new DocQaResponse.Citation();
            citation.setDocumentId("DOC-KB-" + (i + 1));
            citation.setChunkId("CHUNK-" + System.currentTimeMillis() + "-" + i);
            citation.setContent(chunks.get(i).length() > 200 ? chunks.get(i).substring(0, 200) + "..." : chunks.get(i));
            citation.setSourceUrl("https://flk.npc.gov.cn/");
            citation.setScore(0.85 + (0.1 * (chunks.size() - i - 1) / chunks.size()));
            citations.add(citation);
        }
    }

    private void addCitationsFromPatterns(List<DocQaResponse.Citation> citations, String keyword) {
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

    private String generateDefaultAnswer(String question, List<DocQaResponse.Citation> citations, List<String> contextChunks) {
        StringBuilder sb = new StringBuilder();
        sb.append("关于您提出的问题，我检索到以下相关法律信息：\n\n");

        sb.append("您的问题是：").append(question).append("\n\n");

        if (!contextChunks.isEmpty()) {
            sb.append("【相关法条】\n");
            sb.append(contextChunks.get(0)).append("\n\n");
        }

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

    private void persistMessage(String sessionId, String role, String content, int order) {
        try {
            ChatMessage message = new ChatMessage();
            message.setSessionUuid(sessionId);
            message.setRole(role);
            message.setContent(content);
            message.setOrder(order);
            message.setCreatedAt(LocalDateTime.now());
            chatMessageMapper.insert(message);
            log.debug("消息已持久化: sessionId={}, role={}, order={}", sessionId, role, order);
        } catch (Exception e) {
            log.error("消息持久化失败: sessionId={}, error={}", sessionId, e.getMessage());
        }
    }

    public List<ChatMessage> getSessionHistory(String sessionId) {
        try {
            return chatMessageMapper.findBySessionUuid(sessionId);
        } catch (Exception e) {
            log.error("获取会话历史失败: sessionId={}, error={}", sessionId, e.getMessage());
            return Collections.emptyList();
        }
    }

    public void clearSessionHistory(String sessionId) {
        try {
            chatMessageMapper.deleteBySessionUuid(sessionId);
            log.info("会话历史已清除: sessionId={}", sessionId);
        } catch (Exception e) {
            log.error("清除会话历史失败: sessionId={}, error={}", sessionId, e.getMessage());
        }
    }

    public List<Map<String, Object>> getSessionList(String userId) {
        try {
            List<Map<String, Object>> sessions = chatMessageMapper.findSessionsByUserId(userId);

            for (Map<String, Object> session : sessions) {
                List<ChatMessage> messages = chatMessageMapper.findBySessionUuid((String) session.get("sessionId"));
                if (!messages.isEmpty()) {
                    ChatMessage firstMsg = messages.get(0);
                    String title = firstMsg.getContent();
                    if (title.length() > 30) {
                        title = title.substring(0, 30) + "...";
                    }
                    session.put("title", title);
                    session.put("date", formatDate((java.sql.Timestamp) session.get("lastMessage")));
                }
            }

            return sessions;
        } catch (Exception e) {
            log.error("获取会话列表失败: userId={}, error={}", userId, e.getMessage());
            return Collections.emptyList();
        }
    }

    public Map<String, Object> createSession(String userId) {
        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", UUID.randomUUID().toString());
        result.put("userId", userId);
        result.put("createdAt", LocalDateTime.now().toString());
        return result;
    }

    private String formatDate(java.sql.Timestamp timestamp) {
        if (timestamp == null) return "";
        java.time.LocalDateTime dateTime = timestamp.toLocalDateTime();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("MM月dd日");

        if (dateTime.toLocalDate().equals(now.toLocalDate())) {
            return "今天 " + dateTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        } else if (dateTime.toLocalDate().equals(now.toLocalDate().minusDays(1))) {
            return "昨天 " + dateTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        } else {
            return dateTime.format(formatter);
        }
    }
}
