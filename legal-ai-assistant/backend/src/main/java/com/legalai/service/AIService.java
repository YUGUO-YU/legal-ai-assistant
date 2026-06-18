package com.legalai.service;

import com.legalai.llm.LLMClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 业务侧 AI 调用入口。所有方法都委托给 LLMClient，
 * 保留 AIService 这个门面类以便未来加缓存、限流、埋点等。
 */
@Service
public class AIService {

    private static final Logger log = LoggerFactory.getLogger(AIService.class);

    @Autowired
    private LLMClient llmClient;

    public String chat(String prompt) throws IOException {
        return llmClient.chat(prompt);
    }

    public String chatWithMessages(List<Map<String, String>> messages) throws IOException {
        return llmClient.chatWithMessages(messages);
    }

    public String chatWithTools(String prompt, List<Map<String, Object>> tools) throws IOException {
        return llmClient.chatWithTools(prompt, tools);
    }

    public String chatStream(String prompt, Consumer<String> onChunk, Supplier<Boolean> isCancelled) throws IOException {
        return llmClient.chatStream(prompt, onChunk, isCancelled);
    }

    public float[] embedText(String text) throws IOException {
        return llmClient.embedText(text);
    }
}
