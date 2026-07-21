package com.legalai.service;

import com.legalai.dto.*;
import com.legalai.model.ChatMessage;
import com.legalai.repository.ChatMessageMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DocQaService {
    private static final Logger log = LoggerFactory.getLogger(DocQaService.class);

    @Autowired
    private AIService aiService;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired(required = false)
    private ChatMessageMapper chatMessageMapper;

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

        String answer = generateAIAnswer(request.getQuestion(), citations, contextChunks, conversationContext);

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
                emitter.error(new IllegalStateException("AI服务暂时不可用，请稍后重试: " + e.getMessage()));
            }
        });
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
        String kbId = request.getKbId() != null ? request.getKbId().toString() : request.getKnowledgeBaseId();
        if (kbId == null || kbId.isBlank()) {
            return Collections.emptyList();
        }

        List<LegalSearchResponse.SearchResultItem> chunks = knowledgeBaseService.searchInKnowledgeBase(
            kbId,
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
            throw new IllegalStateException("AI服务暂时不可用，请稍后重试: " + e.getMessage());
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

    private void persistMessage(String sessionId, String role, String content, int order) {
        if (chatMessageMapper == null) {
            log.debug("ChatMessageMapper not available, skipping persistence");
            return;
        }
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
        if (chatMessageMapper == null) {
            return Collections.emptyList();
        }
        try {
            return chatMessageMapper.findBySessionUuid(sessionId);
        } catch (Exception e) {
            log.error("获取会话历史失败: sessionId={}, error={}", sessionId, e.getMessage());
            return Collections.emptyList();
        }
    }

    public void clearSessionHistory(String sessionId) {
        if (chatMessageMapper == null) {
            return;
        }
        try {
            chatMessageMapper.deleteBySessionUuid(sessionId);
            log.info("会话历史已清除: sessionId={}", sessionId);
        } catch (Exception e) {
            log.error("清除会话历史失败: sessionId={}, error={}", sessionId, e.getMessage());
        }
    }

    public List<Map<String, Object>> getSessionList(String userId) {
        if (chatMessageMapper == null) {
            return Collections.emptyList();
        }
        try {
            List<Map<String, Object>> sessions = chatMessageMapper.findSessionsByUserId(userId);

            if (!sessions.isEmpty()) {
                List<String> sessionIds = sessions.stream()
                    .map(s -> (String) s.get("sessionId"))
                    .collect(java.util.stream.Collectors.toList());

                List<Map<String, Object>> firstMessages = chatMessageMapper.findFirstMessagesBySessionUuids(sessionIds);
                java.util.Map<String, Map<String, Object>> firstMsgMap = firstMessages.stream()
                    .collect(java.util.stream.Collectors.toMap(
                        m -> (String) m.get("sessionId"),
                        m -> m
                    ));

                for (Map<String, Object> session : sessions) {
                    Map<String, Object> firstMsg = firstMsgMap.get(session.get("sessionId"));
                    if (firstMsg != null) {
                        String title = (String) firstMsg.get("content");
                        if (title != null && title.length() > 30) {
                            title = title.substring(0, 30) + "...";
                        }
                        session.put("title", title);
                        session.put("date", formatDate((java.time.LocalDateTime) session.get("lastMessage")));
                    }
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

    private String formatDate(java.time.LocalDateTime dateTime) {
        if (dateTime == null) return "";
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
