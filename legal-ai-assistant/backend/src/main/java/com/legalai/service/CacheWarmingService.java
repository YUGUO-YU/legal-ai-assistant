package com.legalai.service;

import com.legalai.admin.service.AdminDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class CacheWarmingService {

    private static final Logger log = LoggerFactory.getLogger(CacheWarmingService.class);

    private static final String STATS_CACHE_KEY = "system:stats";
    private static final String HOT_LAWS_KEY = "system:hot_laws";
    private static final String DICT_CACHE_PREFIX = "system:dict:";
    private static final Duration STATS_TTL = Duration.ofMinutes(5);
    private static final Duration HOT_DATA_TTL = Duration.ofMinutes(15);

    @Value("${redis.enabled:false}")
    private boolean redisEnabled;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired(required = false)
    private AdminDataService adminDataService;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Map<String, Long> cacheMetrics = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        if (redisEnabled) {
            log.info("缓存预热服务已启动");
            scheduleWarmingTask();
        }
    }

    private void scheduleWarmingTask() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                warmCache();
            } catch (Exception e) {
                log.warn("缓存预热失败: {}", e.getMessage());
            }
        }, 1, 15, TimeUnit.MINUTES);
    }

    public void warmCache() {
        log.debug("开始缓存预热...");
        long startTime = System.currentTimeMillis();

        try {
            warmStatistics();
            warmHotData();
            warmDictionaries();
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("缓存预热完成，耗时: {}ms", duration);
        } catch (Exception e) {
            log.warn("缓存预热异常: {}", e.getMessage());
        }
    }

    private void warmStatistics() {
        if (redisTemplate == null || adminDataService == null) {
            return;
        }
        try {
            Map<String, Object> stats = adminDataService.stats();
            if (stats != null) {
                redisTemplate.opsForValue().set(STATS_CACHE_KEY, stats, STATS_TTL);
                incrementMetric("stats");
            }
        } catch (Exception e) {
            log.debug("统计缓存预热失败: {}", e.getMessage());
        }
    }

    private void warmHotData() {
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(HOT_LAWS_KEY, getDefaultHotLaws(), HOT_DATA_TTL);
            incrementMetric("hot_laws");
        } catch (Exception e) {
            log.debug("热点数据缓存预热失败: {}", e.getMessage());
        }
    }

    private void warmDictionaries() {
        if (redisTemplate == null) {
            return;
        }
        try {
            Set<String> dictTypes = Set.of("status", "gender", "law_category", "case_type");
            for (String type : dictTypes) {
                String key = DICT_CACHE_PREFIX + type;
                if (redisTemplate.hasKey(key) == Boolean.FALSE) {
                    redisTemplate.opsForValue().set(key, getDefaultDict(type), Duration.ofHours(24));
                    incrementMetric("dict:" + type);
                }
            }
        } catch (Exception e) {
            log.debug("字典缓存预热失败: {}", e.getMessage());
        }
    }

    public Map<String, Object> getCachedStats() {
        if (!redisEnabled || redisTemplate == null) {
            return null;
        }
        try {
            Object cached = redisTemplate.opsForValue().get(STATS_CACHE_KEY);
            if (cached != null) {
                incrementMetric("stats_hit");
                return (Map<String, Object>) cached;
            }
        } catch (Exception e) {
            log.debug("获取缓存统计失败: {}", e.getMessage());
        }
        return null;
    }

    public Object getCachedHotLaws() {
        if (!redisEnabled || redisTemplate == null) {
            return null;
        }
        try {
            Object cached = redisTemplate.opsForValue().get(HOT_LAWS_KEY);
            if (cached != null) {
                incrementMetric("hot_laws_hit");
                return cached;
            }
        } catch (Exception e) {
            log.debug("获取热点法规缓存失败: {}", e.getMessage());
        }
        return null;
    }

    public void invalidateStatsCache() {
        if (!redisEnabled || redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.delete(STATS_CACHE_KEY);
            log.debug("统计缓存已清除");
        } catch (Exception e) {
            log.warn("清除统计缓存失败: {}", e.getMessage());
        }
    }

    public void invalidateHotDataCache() {
        if (!redisEnabled || redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.delete(HOT_LAWS_KEY);
            log.debug("热点数据缓存已清除");
        } catch (Exception e) {
            log.warn("清除热点数据缓存失败: {}", e.getMessage());
        }
    }

    private void incrementMetric(String key) {
        cacheMetrics.merge(key, 1L, Long::sum);
    }

    public Map<String, Long> getCacheMetrics() {
        return new ConcurrentHashMap<>(cacheMetrics);
    }

    public void clearCacheMetrics() {
        cacheMetrics.clear();
    }

    private Map<String, Object> getDefaultHotLaws() {
        return Map.of(
            "laws", List.of(
                Map.of("name", "中华人民共和国宪法", "count", 15234),
                Map.of("name", "中华人民共和国民法典", "count", 12456),
                Map.of("name", "中华人民共和国刑法", "count", 9876),
                Map.of("name", "中华人民共和国劳动法", "count", 7654),
                Map.of("name", "中华人民共和国合同法", "count", 6543)
            ),
            "updatedAt", System.currentTimeMillis()
        );
    }

    private List<Map<String, String>> getDefaultDict(String type) {
        return switch (type) {
            case "status" -> List.of(
                Map.of("value", "active", "label", "激活"),
                Map.of("value", "inactive", "label", "未激活"),
                Map.of("value", "deleted", "label", "已删除")
            );
            case "gender" -> List.of(
                Map.of("value", "male", "label", "男"),
                Map.of("value", "female", "label", "女"),
                Map.of("value", "unknown", "label", "未知")
            );
            default -> List.of();
        };
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
