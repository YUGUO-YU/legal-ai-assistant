package com.legalai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class AIService {

    private static final Logger log = LoggerFactory.getLogger(AIService.class);

    @Value("${ai.openclaw.url:http://localhost:19001}")
    private String openClawUrl;

    @Value("${ai.openclaw.token:my-secret-token}")
    private String openClawToken;

    @Value("${ai.minimax.model:openclaw}")
    private String model;

    @Value("${ai.minimax.timeout:120}")
    private int timeout;

    @Value("${ai.minimax.embedding-model:embo-01}")
    private String embeddingModel;

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    public AIService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public String chat(String prompt) throws IOException {
        log.info("调用OpenClaw网关: model={}, prompt长度={}", model, prompt.length());

        Map<String, Object> requestBody = Map.of(
            "model", model,
            "messages", List.of(Map.of("role", "user", "content", prompt)),
            "stream", false
        );

        return executeChatRequest(requestBody);
    }

    public String chatWithMessages(List<Map<String, String>> messages) throws IOException {
        log.info("调用OpenClaw网关(多轮对话): 消息数量={}", messages.size());

        Map<String, Object> requestBody = Map.of(
            "model", model,
            "messages", messages,
            "stream", false
        );

        return executeChatRequest(requestBody);
    }

    public String chatWithTools(String prompt, List<Map<String, Object>> tools) throws IOException {
        log.info("调用OpenClaw网关(带工具): model={}, prompt长度={}, tools数量={}", model, prompt.length(), tools != null ? tools.size() : 0);

        List<Map<String, String>> messages = List.of(Map.of("role", "user", "content", prompt));

        Map<String, Object> requestBody = new java.util.HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("stream", false);
        if (tools != null && !tools.isEmpty()) {
            requestBody.put("tools", tools);
        }

        return executeChatRequest(requestBody);
    }

    public String chatStream(String prompt, java.util.function.Consumer<String> onChunk, java.util.function.Supplier<Boolean> isCancelled) throws IOException {
        log.info("调用OpenClaw流式网关: model={}, prompt长度={}", model, prompt.length());

        Map<String, Object> requestBody = Map.of(
            "model", model,
            "messages", List.of(Map.of("role", "user", "content", prompt)),
            "stream", true
        );

        String json = objectMapper.writeValueAsString(requestBody);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(openClawUrl + "/v1/chat/completions")
                .addHeader("Authorization", "Bearer " + openClawToken)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "no body";
                log.error("OpenClaw流式API调用失败: code={}, message={}, body={}", response.code(), response.message(), errorBody);
                throw new IOException("流式API调用失败: " + response.code());
            }

            if (response.body() == null) {
                throw new IOException("响应体为空");
            }

            StringBuilder fullResponse = new StringBuilder();
            String responseBody = response.body().string();

            String[] lines = responseBody.split("\n");
            for (String line : lines) {
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
        } catch (Exception e) {
            log.error("OpenClaw流式API异常: {}", e.getMessage(), e);
            throw new IOException("流式API异常: " + e.getMessage(), e);
        }
    }

    private String executeChatRequest(Map<String, Object> requestBody) throws IOException {
        String json = objectMapper.writeValueAsString(requestBody);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(openClawUrl + "/v1/chat/completions")
                .addHeader("Authorization", "Bearer " + openClawToken)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "no body";
                log.error("OpenClaw API调用失败: code={}, message={}, body={}", response.code(), response.message(), errorBody);
                throw new IOException("API调用失败: " + response.code());
            }

            String responseBody = response.body().string();
            log.info("OpenClaw API响应长度: {}", responseBody.length());

            return parseResponse(responseBody);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            log.error("OpenClaw API异常: {}", e.getMessage(), e);
            throw new IOException("API调用异常: " + e.getMessage(), e);
        }
    }

    public float[] embedText(String text) throws IOException {
        log.info("调用OpenClaw Embedding API: text长度={}", text.length());

        Map<String, Object> requestBody = Map.of(
            "model", "minimax/" + embeddingModel,
            "input", text
        );

        String json = objectMapper.writeValueAsString(requestBody);

        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(openClawUrl + "/v1/embeddings")
                .addHeader("Authorization", "Bearer " + openClawToken)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "no body";
                log.error("OpenClaw Embedding API调用失败: code={}, message={}, body={}", response.code(), response.message(), errorBody);
                throw new IOException("Embedding API调用失败: " + response.code());
            }

            String responseBody = response.body().string();
            return parseEmbeddingResponse(responseBody);
        } catch (Exception e) {
            log.error("OpenClaw Embedding API异常: {}", e.getMessage(), e);
            throw new IOException("Embedding API异常: " + e.getMessage(), e);
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
                log.info("MiniMax解析成功: content长度={}", content.length());
                return content;
            }
            log.warn("MiniMax响应格式异常: choices={}", choices);
            return "抱歉，AI服务暂时无法响应，请稍后重试。";
        } catch (Exception e) {
            log.error("解析MiniMax响应失败: {}", e.getMessage(), e);
            return "抱歉，AI服务暂时无法响应，请稍后重试。";
        }
    }
}
