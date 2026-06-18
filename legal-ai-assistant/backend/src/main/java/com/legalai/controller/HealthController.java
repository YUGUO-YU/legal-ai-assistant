package com.legalai.controller;

import com.legalai.llm.LLMClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查 + AI 状态接口。
 * AI 状态严格二值：online / offline。
 */
@RestController
@RequestMapping("/api/v1")
@CrossOrigin
public class HealthController {
    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    @Autowired
    private LLMClient llmClient;

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "UP");
        result.put("service", "legal-ai-assistant");
        result.put("version", "1.0.0");
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    @GetMapping("/ai-status")
    public Map<String, Object> aiStatus() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("service", "minimax");
        result.put("model", llmClient.getModel());
        result.put("baseUrl", llmClient.getBaseUrl());
        result.put("timestamp", System.currentTimeMillis());

        String apiKey = System.getenv("MINIMAX_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            result.put("status", "offline");
            result.put("message", "MINIMAX_API_KEY 未配置");
            return result;
        }

        try {
            if (llmClient.ping()) {
                String firstModel = llmClient.firstModel();
                result.put("model", firstModel != null ? firstModel : llmClient.getModel());
                result.put("status", "online");
                result.put("message", "AI 服务在线");
            } else {
                result.put("status", "offline");
                result.put("message", "AI 服务不可用，请检查 api-key / 网络");
            }
        } catch (Exception e) {
            log.warn("检查 AI 状态异常: {}", e.getMessage());
            result.put("status", "offline");
            result.put("message", "AI 服务连接失败: " + e.getMessage());
        }
        return result;
    }
}
