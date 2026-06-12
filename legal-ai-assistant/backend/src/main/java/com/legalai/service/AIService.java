package com.legalai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
public class AIService {

    private static final Logger log = LoggerFactory.getLogger(AIService.class);

    @Value("${ai.minimax.api-url}")
    private String apiUrl;

    @Value("${ai.minimax.api-key}")
    private String apiKey;

    @Value("${ai.minimax.model}")
    private String model;

    @Value("${ai.minimax.timeout:120}")
    private int timeout;

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
        log.info("调用MiniMax API: model={}, prompt长度={}", model, prompt.length());

        String json = String.format("""
            {
                "model": "%s",
                "messages": [
                    {"role": "user", "content": "%s"}
                ],
                "stream": false
            }
            """, model, prompt.replace("\"", "\\\""));

        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(apiUrl + "/text/chatcompletion_v2")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("MiniMax API调用失败: code={}, message={}", response.code(), response.message());
                throw new IOException("API调用失败: " + response.code());
            }

            String responseBody = response.body().string();
            log.debug("MiniMax API响应: {}", responseBody);

            return parseResponse(responseBody);
        }
    }

    private String parseResponse(String responseBody) {
        try {
            JsonNode json = objectMapper.readTree(responseBody);
            JsonNode choices = json.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                return choices.get(0).get("message").get("content").asText();
            }
            return "抱歉，AI服务暂时无法响应，请稍后重试。";
        } catch (Exception e) {
            log.error("解析MiniMax响应失败: {}", e.getMessage());
            return "抱歉，AI服务暂时无法响应，请稍后重试。";
        }
    }
}
