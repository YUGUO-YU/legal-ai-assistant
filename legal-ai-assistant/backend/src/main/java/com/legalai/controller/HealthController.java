package com.legalai.controller;

import com.legalai.llm.LLMClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.List;
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

    @Autowired
    private DataSource dataSource;

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
        result.put("model", llmClient.getModel());
        result.put("baseUrl", llmClient.getBaseUrl());
        result.put("timestamp", System.currentTimeMillis());

        if (!llmClient.isApiKeyConfigured()) {
            result.put("status", "offline");
            result.put("message", "未配置 API 密钥，请到后台管理添加");
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

    @GetMapping("/announcements")
    public Map<String, Object> publicAnnouncements(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int pageSize) {
        Map<String, Object> result = new LinkedHashMap<>();
        try (Connection conn = dataSource.getConnection()) {
            int offset = Math.max(0, (page - 1) * pageSize);

            String countSql = "SELECT COUNT(*) FROM sys_announcement WHERE status = 1 AND (expired_at IS NULL OR expired_at > NOW())";
            PreparedStatement countStmt = conn.prepareStatement(countSql);
            ResultSet countRs = countStmt.executeQuery();
            int total = countRs.next() ? countRs.getInt(1) : 0;
            countStmt.close();

            String sql = "SELECT id, title, content, type, priority, published_at, created_at FROM sys_announcement WHERE status = 1 AND (expired_at IS NULL OR expired_at > NOW()) ORDER BY priority DESC, published_at DESC LIMIT ? OFFSET ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, pageSize);
            stmt.setInt(2, offset);
            var rows = stmt.executeQuery();
            List<Map<String, Object>> list = new java.util.ArrayList<>();
            while (rows.next()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", rows.getLong("id"));
                item.put("title", rows.getString("title"));
                item.put("content", rows.getString("content"));
                item.put("type", rows.getInt("type"));
                item.put("priority", rows.getInt("priority"));
                item.put("published_at", rows.getTimestamp("published_at") != null ? rows.getTimestamp("published_at").toString() : null);
                item.put("created_at", rows.getTimestamp("created_at") != null ? rows.getTimestamp("created_at").toString() : null);
                list.add(item);
            }
            stmt.close();
            result.put("list", list);
            result.put("total", total);
            result.put("page", page);
            result.put("pageSize", pageSize);
            result.put("source", "public");
        } catch (Exception e) {
            log.warn("获取公告失败: {}", e.getMessage());
            result.put("list", java.util.Collections.emptyList());
            result.put("total", 0);
        }
        return result;
    }
}
