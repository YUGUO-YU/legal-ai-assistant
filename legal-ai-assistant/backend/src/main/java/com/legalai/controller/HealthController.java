package com.legalai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalai.config.OpenClawHealthCheck;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin
public class HealthController {
    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    @Value("${ai.openclaw.url:http://localhost:19001}")
    private String openClawUrl;

    @Value("${ai.openclaw.token:my-secret-token}")
    private String openClawToken;

    @Autowired(required = false)
    private OpenClawHealthCheck openClawHealthCheck;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "status", "UP",
            "service", "legal-ai-assistant",
            "version", "1.0.0",
            "timestamp", System.currentTimeMillis()
        );
    }

    @GetMapping("/ai-status")
    public Map<String, Object> aiStatus() {
        Map<String, Object> result = new HashMap<>();
        result.put("service", "MiniMax-M3");
        result.put("timestamp", System.currentTimeMillis());

        OpenClawHealthCheck.State state = openClawHealthCheck != null
            ? openClawHealthCheck.getState()
            : OpenClawHealthCheck.State.NOT_STARTED;
        result.put("openclawState", state.name());

        if (openClawHealthCheck != null) {
            result.put("openclawBinary", openClawHealthCheck.getCurrentBinary());
            result.put("openclawLogFile", openClawHealthCheck.getCurrentLogFile());
            String err = openClawHealthCheck.getLastError();
            if (err != null) {
                result.put("openclawError", err);
            }
        }

        try {
            Request request = new Request.Builder()
                    .url(openClawUrl + "/v1/models")
                    .addHeader("Authorization", "Bearer " + openClawToken)
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String body = response.body().string();
                    var node = objectMapper.readTree(body);
                    var models = node.get("data");
                    if (models != null && models.isArray() && models.size() > 0) {
                        String modelName = models.get(0).has("id") ? models.get(0).get("id").asText() : "MiniMax-M3";
                        result.put("status", "online");
                        result.put("model", modelName);
                        result.put("message", "AI 服务正常运行");
                    } else {
                        result.put("status", "degraded");
                        result.put("model", "MiniMax-M3");
                        result.put("message", "AI 服务响应异常");
                    }
                } else {
                    result.put("status", "offline");
                    result.put("model", "MiniMax-M3");
                    result.put("message", "AI 服务暂时不可用");
                }
            }
        } catch (Exception e) {
            log.error("检查 AI 状态失败: {}", e.getMessage());
            result.put("status", "error");
            result.put("model", "MiniMax-M3");
            result.put("message", "连接 AI 服务失败: " + e.getMessage());
        }

        return result;
    }

    @PostMapping("/ai-status/restart")
    public Map<String, Object> restartOpenClaw() {
        Map<String, Object> result = new HashMap<>();
        result.put("timestamp", System.currentTimeMillis());

        if (openClawHealthCheck == null) {
            result.put("success", false);
            result.put("status", "error");
            result.put("message", "OpenClaw 管理组件未启用");
            return result;
        }

        try {
            boolean ok = openClawHealthCheck.restart();
            OpenClawHealthCheck.State state = openClawHealthCheck.getState();
            result.put("success", ok);
            result.put("status", state.name().toLowerCase());
            result.put("openclawState", state.name());
            result.put("message", ok ? "OpenClaw 重启成功" : ("OpenClaw 重启失败: " + (openClawHealthCheck.getLastError() != null ? openClawHealthCheck.getLastError() : "未知原因")));
        } catch (Exception e) {
            log.error("OpenClaw 重启失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("status", "error");
            result.put("message", "重启异常: " + e.getMessage());
        }
        return result;
    }
}
