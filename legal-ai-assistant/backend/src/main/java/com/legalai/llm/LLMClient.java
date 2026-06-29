package com.legalai.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 直接调用 MiniMax-M3 (OpenAI 兼容 Chat Completions 协议) 的客户端。
 *
 * 配置项 (application.yml):
 *   ai.minimax.base-url       默认 https://api.minimax.chat/v1
 *   ai.minimax.api-key        必填
 *   ai.minimax.model          默认 MiniMax-M3
 *   ai.minimax.timeout        秒，默认 120
 *   ai.minimax.embedding-model 默认 embo-01
 */
@Component
public class LLMClient {

    private static final Logger log = LoggerFactory.getLogger(LLMClient.class);

    @Value("${ai.minimax.base-url:https://api.minimax.chat/v1}")
    private String baseUrl;

    @Value("${ai.minimax.api-key:}")
    private String apiKey;

    @Value("${ai.minimax.model:MiniMax-M3}")
    private String model;

    @Value("${ai.minimax.timeout:120}")
    private int timeout;

    @Value("${ai.minimax.embedding-model:embo-01}")
    private String embeddingModel;

    @Value("${ai.minimax.health-endpoint:/models}")
    private String healthEndpoint;

    @Value("${ai.minimax.health-timeout:5}")
    private int healthTimeout;

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    public LLMClient() {
        this.client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(timeout, TimeUnit.SECONDS)
            .writeTimeout(timeout, TimeUnit.SECONDS)
            .build();
        this.objectMapper = new ObjectMapper();
    }

    private OkHttpClient healthClient() {
        return client.newBuilder()
            .connectTimeout(healthTimeout, TimeUnit.SECONDS)
            .readTimeout(healthTimeout, TimeUnit.SECONDS)
            .writeTimeout(healthTimeout, TimeUnit.SECONDS)
            .build();
    }

    public String getModel() {
        return model;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * 单轮对话
     */
    public String chat(String prompt) throws IOException {
        log.info("调用 MiniMax: model={}, prompt长度={}", model, prompt == null ? 0 : prompt.length());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt == null ? "" : prompt)));
        requestBody.put("stream", false);

        return executeChatRequest(requestBody);
    }

    /**
     * 多轮对话
     */
    public String chatWithMessages(List<Map<String, String>> messages) throws IOException {
        log.info("调用 MiniMax(多轮): 消息数量={}", messages == null ? 0 : messages.size());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("stream", false);

        return executeChatRequest(requestBody);
    }

    /**
     * 带工具调用的对话
     */
    public String chatWithTools(String prompt, List<Map<String, Object>> tools) throws IOException {
        log.info("调用 MiniMax(带工具): model={}, prompt长度={}, tools数量={}", model,
            prompt == null ? 0 : prompt.length(),
            tools != null ? tools.size() : 0);

        List<Map<String, String>> messages = List.of(Map.of("role", "user", "content", prompt == null ? "" : prompt));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("stream", false);
        if (tools != null && !tools.isEmpty()) {
            requestBody.put("tools", tools);
        }

        return executeChatRequest(requestBody);
    }

    /**
     * 带联网搜索的对话（MiniMax web_search 工具）。
     * 返回 AI 整理后的文本，搜索结果会合并在响应中。
     */
    public String searchWeb(String prompt) throws IOException {
        log.info("调用 MiniMax(联网搜索): model={}, prompt长度={}", model, prompt == null ? 0 : prompt.length());

        List<Map<String, Object>> tools = List.of(Map.of(
            "type", "web_search",
            "web_search", Map.of(
                "search_mode", "outline",
                "enable_brief_search_result", false
            )
        ));

        return chatWithTools(prompt, tools);
    }

    /**
     * 联网搜索 + 结构化输出：先用 web_search 获取信息，再调用一次让 AI 整理为结构化 JSON。
     */
    public String searchAndStructure(String searchPrompt, String structurePrompt) throws IOException {
        log.info("调用 MiniMax(搜索+结构化): 两步流程");

        String searchResult = searchWeb(searchPrompt);
        log.info("联网搜索完成，结果长度={}", searchResult != null ? searchResult.length() : 0);

        List<Map<String, String>> messages = List.of(
            Map.of("role", "system", "content", searchPrompt),
            Map.of("role", "assistant", "content", searchResult != null ? searchResult : ""),
            Map.of("role", "user", "content", structurePrompt)
        );

        return chatWithMessages(messages);
    }

    /**
     * 流式对话
     */
    public String chatStream(String prompt, Consumer<String> onChunk, Supplier<Boolean> isCancelled) throws IOException {
        log.info("调用 MiniMax(流式): model={}, prompt长度={}", model, prompt == null ? 0 : prompt.length());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt == null ? "" : prompt)));
        requestBody.put("stream", true);

        String json = objectMapper.writeValueAsString(requestBody);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

        Request request = new Request.Builder()
            .url(baseUrl + "/chat/completions")
            .addHeader("Authorization", "Bearer " + apiKey)
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "no body";
                log.error("MiniMax 流式调用失败: code={}, message={}, body={}", response.code(), response.message(), errorBody);
                throw new IOException("流式API调用失败: " + response.code() + " " + errorBody);
            }

            if (response.body() == null) {
                throw new IOException("响应体为空");
            }

            StringBuilder fullResponse = new StringBuilder();
            String responseBody = response.body().string();

            String[] lines = responseBody.split("\n");
            for (String line : lines) {
                if (isCancelled != null && Boolean.TRUE.equals(isCancelled.get())) {
                    log.info("流式响应被取消");
                    break;
                }
                if (line.startsWith("data: ")) {
                    String data = line.substring(6);
                    if ("[DONE]".equals(data)) {
                        break;
                    }
                    try {
                        JsonNode jsonNode = objectMapper.readTree(data);
                        JsonNode choices = jsonNode.get("choices");
                        if (choices != null && choices.isArray() && choices.size() > 0) {
                            JsonNode delta = choices.get(0).get("delta");
                            if (delta != null && delta.has("content")) {
                                String content = delta.get("content").asText();
                                fullResponse.append(content);
                                if (onChunk != null) {
                                    onChunk.accept(content);
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.debug("解析流式响应行失败: {}", line);
                    }
                }
            }

            log.info("流式响应完成: 总长度={}", fullResponse.length());
            return fullResponse.toString();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            log.error("MiniMax 流式API异常: {}", e.getMessage(), e);
            throw new IOException("流式API异常: " + e.getMessage(), e);
        }
    }

    public float[] embedText(String text) throws IOException {
        log.info("调用 MiniMax Embedding: text长度={}", text == null ? 0 : text.length());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", embeddingModel);
        requestBody.put("input", text == null ? "" : text);

        String json = objectMapper.writeValueAsString(requestBody);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

        Request request = new Request.Builder()
            .url(baseUrl + "/embeddings")
            .addHeader("Authorization", "Bearer " + apiKey)
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "no body";
                log.error("MiniMax Embedding 失败: code={}, message={}, body={}", response.code(), response.message(), errorBody);
                throw new IOException("Embedding API调用失败: " + response.code());
            }

            String responseBody = response.body().string();
            return parseEmbeddingResponse(responseBody);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            log.error("MiniMax Embedding 异常: {}", e.getMessage(), e);
            throw new IOException("Embedding API异常: " + e.getMessage(), e);
        }
    }

    /**
     * 探测模型是否可访问（用于健康检查）。
     * 严格判定：只有 HTTP 2xx 才算在线；401/403/网络异常/超时都算离线。
     */
    public boolean ping() {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("MiniMax 健康检查失败: api-key 未配置");
            return false;
        }
        try {
            Request request = new Request.Builder()
                .url(baseUrl + healthEndpoint)
                .addHeader("Authorization", "Bearer " + apiKey)
                .get()
                .build();
            try (Response response = healthClient().newCall(request).execute()) {
                boolean ok = response.isSuccessful();
                if (!ok) {
                    log.warn("MiniMax 健康检查失败: code={}, message={}",
                        response.code(), response.message());
                }
                return ok;
            }
        } catch (Exception e) {
            log.warn("MiniMax 健康检查失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 探测第一个可用模型名（用于 ai-status 展示）
     */
    public String firstModel() {
        if (apiKey == null || apiKey.isEmpty()) {
            return model;
        }
        try {
            Request request = new Request.Builder()
                .url(baseUrl + healthEndpoint)
                .addHeader("Authorization", "Bearer " + apiKey)
                .get()
                .build();
            try (Response response = healthClient().newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    return model;
                }
                JsonNode json = objectMapper.readTree(response.body().string());
                JsonNode data = json.get("data");
                if (data != null && data.isArray() && data.size() > 0) {
                    JsonNode first = data.get(0);
                    if (first.has("id")) {
                        return first.get("id").asText();
                    }
                }
            }
        } catch (Exception e) {
            log.debug("获取 MiniMax 模型列表失败: {}", e.getMessage());
        }
        return model;
    }

    /**
     * 健康检查并返回延迟信息
     */
    public Map<String, Object> healthCheck() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("model", model);
        result.put("baseUrl", baseUrl);
        long start = System.currentTimeMillis();
        boolean healthy = ping();
        long latency = System.currentTimeMillis() - start;
        result.put("healthy", healthy);
        result.put("latencyMs", latency);
        result.put("apiKeyConfigured", apiKey != null && !apiKey.isEmpty());
        return result;
    }

    public boolean isApiKeyConfigured() {
        return apiKey != null && !apiKey.isEmpty();
    }

    private String executeChatRequest(Map<String, Object> requestBody) throws IOException {
        String json = objectMapper.writeValueAsString(requestBody);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

        Request request = new Request.Builder()
            .url(baseUrl + "/chat/completions")
            .addHeader("Authorization", "Bearer " + apiKey)
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "no body";
                log.error("MiniMax API调用失败: code={}, message={}, body={}", response.code(), response.message(), errorBody);
                throw new IOException("API调用失败: " + response.code() + " " + errorBody);
            }

            String responseBody = response.body().string();
            log.info("MiniMax API响应长度: {}", responseBody.length());
            return parseResponse(responseBody);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            log.error("MiniMax API异常: {}", e.getMessage(), e);
            throw new IOException("API调用异常: " + e.getMessage(), e);
        }
    }

    private float[] parseEmbeddingResponse(String responseBody) {
        try {
            JsonNode json = objectMapper.readTree(responseBody);
            JsonNode embedding = json.get("data").get(0).get("embedding");
            float[] result = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                result[i] = (float) embedding.get(i).asDouble();
            }
            log.info("Embedding解析成功: 向量维度={}", result.length);
            return result;
        } catch (Exception e) {
            log.error("解析Embedding响应失败: {}", e.getMessage(), e);
            return new float[1536];
        }
    }

    private String parseResponse(String responseBody) {
        try {
            JsonNode json = objectMapper.readTree(responseBody);
            JsonNode choices = json.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                String content = choices.get(0).get("message").get("content").asText();
                log.info("MiniMax 解析成功: content长度={}", content.length());
                return content;
            }
            log.warn("MiniMax 响应格式异常: choices={}", choices);
            return "抱歉，AI服务暂时无法响应，请稍后重试。";
        } catch (Exception e) {
            log.error("解析 MiniMax 响应失败: {}", e.getMessage(), e);
            return "抱歉，AI服务暂时无法响应，请稍后重试。";
        }
    }
}
