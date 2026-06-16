package com.legalai.service;

import com.legalai.dto.LegalSearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);

    private static final String SEARCH_CACHE_PREFIX = "legal:search:";
    private static final String ARTICLE_CACHE_PREFIX = "legal:article:";
    private static final Duration SEARCH_CACHE_TTL = Duration.ofMinutes(5);
    private static final Duration ARTICLE_CACHE_TTL = Duration.ofHours(24);

    @Value("${redis.enabled:false}")
    private boolean redisEnabled;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    public void cacheSearchResults(String query, LegalSearchResponse response) {
        if (!redisEnabled || redisTemplate == null) {
            return;
        }
        try {
            String key = SEARCH_CACHE_PREFIX + hashQuery(query);
            redisTemplate.opsForValue().set(key, response, SEARCH_CACHE_TTL);
            log.debug("搜索结果已缓存: query={}", query);
        } catch (Exception e) {
            log.warn("缓存搜索结果失败: {}", e.getMessage());
        }
    }

    public LegalSearchResponse getCachedSearchResults(String query) {
        if (!redisEnabled || redisTemplate == null) {
            return null;
        }
        try {
            String key = SEARCH_CACHE_PREFIX + hashQuery(query);
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                log.debug("命中搜索缓存: query={}", query);
                return (LegalSearchResponse) cached;
            }
        } catch (Exception e) {
            log.warn("获取缓存失败: {}", e.getMessage());
        }
        return null;
    }

    public void cacheArticle(String articleId, LegalSearchResponse.SearchResultItem item) {
        if (!redisEnabled || redisTemplate == null) {
            return;
        }
        try {
            String key = ARTICLE_CACHE_PREFIX + articleId;
            redisTemplate.opsForValue().set(key, item, ARTICLE_CACHE_TTL);
            log.debug("法规详情已缓存: articleId={}", articleId);
        } catch (Exception e) {
            log.warn("缓存法规详情失败: {}", e.getMessage());
        }
    }

    public LegalSearchResponse.SearchResultItem getCachedArticle(String articleId) {
        if (!redisEnabled || redisTemplate == null) {
            return null;
        }
        try {
            String key = ARTICLE_CACHE_PREFIX + articleId;
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                log.debug("命中法规缓存: articleId={}", articleId);
                return (LegalSearchResponse.SearchResultItem) cached;
            }
        } catch (Exception e) {
            log.warn("获取缓存失败: {}", e.getMessage());
        }
        return null;
    }

    public void invalidateSearchCache() {
        if (!redisEnabled || redisTemplate == null) {
            return;
        }
        try {
            var keys = redisTemplate.keys(SEARCH_CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("已清除搜索缓存: {}条", keys.size());
            }
        } catch (Exception e) {
            log.warn("清除搜索缓存失败: {}", e.getMessage());
        }
    }

    public void invalidateArticleCache(String articleId) {
        if (!redisEnabled || redisTemplate == null) {
            return;
        }
        try {
            String key = ARTICLE_CACHE_PREFIX + articleId;
            redisTemplate.delete(key);
            log.debug("已清除法规缓存: articleId={}", articleId);
        } catch (Exception e) {
            log.warn("清除法规缓存失败: {}", e.getMessage());
        }
    }

    public boolean isRedisAvailable() {
        if (!redisEnabled || redisTemplate == null) {
            return false;
        }
        try {
            redisTemplate.opsForValue().get("__ping__");
            return true;
        } catch (Exception e) {
            log.warn("Redis不可用: {}", e.getMessage());
            return false;
        }
    }

    private String hashQuery(String query) {
        return String.valueOf(query.hashCode());
    }
}