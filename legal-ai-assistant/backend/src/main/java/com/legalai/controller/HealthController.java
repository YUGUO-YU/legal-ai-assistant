package com.legalai.controller;

import com.legalai.llm.LLMClient;
import com.legalai.service.CacheService;
import com.legalai.service.CacheWarmingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
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
@Tag(name = "系统健康", description = "系统健康检查、AI服务状态、公告查询等公共接口")
public class HealthController {
    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    @Autowired
    private LLMClient llmClient;

    @Autowired
    private DataSource dataSource;

    @Value("${redis.enabled:false}")
    private boolean redisEnabled;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired(required = false)
    private CacheWarmingService cacheWarmingService;

    @Operation(summary = "服务健康检查", description = "检查后端服务是否正常运行")
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "UP");
        result.put("service", "legal-ai-assistant");
        result.put("version", "1.0.0");
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    @Operation(summary = "AI服务状态", description = "检查AI服务是否可用，返回online/offline状态")
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

    @Operation(summary = "查询公告列表", description = "分页查询当前有效的系统公告")
    @GetMapping("/announcements")
    public Map<String, Object> publicAnnouncements(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "5") int pageSize) {
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

    @Operation(summary = "缓存状态", description = "获取Redis缓存服务状态和缓存指标")
    @GetMapping("/cache/status")
    public Map<String, Object> cacheStatus() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("redisEnabled", redisEnabled);

        if (!redisEnabled || redisTemplate == null) {
            result.put("status", "disabled");
            return result;
        }

        try {
            redisTemplate.opsForValue().get("__cache_check__");
            result.put("status", "connected");

            if (cacheWarmingService != null) {
                result.put("metrics", cacheWarmingService.getCacheMetrics());
            }

            Long dbSize = redisTemplate.getConnectionFactory().getConnection().dbSize();
            result.put("dbSize", dbSize);
        } catch (Exception e) {
            log.warn("Redis状态检查失败: {}", e.getMessage());
            result.put("status", "error");
            result.put("message", e.getMessage());
        }

        return result;
    }

    @Operation(summary = "触发缓存预热", description = "手动触发缓存预热")
    @GetMapping("/cache/warm")
    public Map<String, Object> triggerCacheWarm(
            @Parameter(description = "管理员密钥") @RequestParam(required = false) String adminKey) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (cacheWarmingService == null) {
            result.put("success", false);
            result.put("message", "缓存预热服务不可用");
            return result;
        }

        try {
            cacheWarmingService.warmCache();
            result.put("success", true);
            result.put("message", "缓存预热已触发");
            result.put("metrics", cacheWarmingService.getCacheMetrics());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }
}
