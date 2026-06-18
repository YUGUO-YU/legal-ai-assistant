package com.legalai.controller;

import com.legalai.llm.LLMClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin
public class HealthController {
    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    @Autowired
    private LLMClient llmClient;

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
        result.put("service", llmClient.getModel());
        result.put("model", llmClient.getModel());
        result.put("baseUrl", llmClient.getBaseUrl());
        result.put("timestamp", System.currentTimeMillis());

        try {
            boolean ok = llmClient.ping();
            if (ok) {
                String firstModel = llmClient.firstModel();
                result.put("status", "online");
                result.put("model", firstModel != null ? firstModel : llmClient.getModel());
                result.put("message", "AI 服务正常运行");
            } else {
                result.put("status", "offline");
                result.put("message", "AI 服务暂时不可用，请检查 ai.minimax.api-key 与 ai.minimax.base-url 配置");
            }
        } catch (Exception e) {
            log.error("检查 AI 状态失败: {}", e.getMessage());
            result.put("status", "error");
            result.put("message", "连接 AI 服务失败: " + e.getMessage());
        }

        return result;
    }
}
