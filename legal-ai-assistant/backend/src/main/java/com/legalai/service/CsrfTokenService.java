package com.legalai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class CsrfTokenService {
    private static final Logger log = LoggerFactory.getLogger(CsrfTokenService.class);
    private static final String CSRF_TOKEN_PREFIX = "csrf:";
    private static final long TOKEN_EXPIRE_HOURS = 1;

    @Value("${redis.enabled:false}")
    private boolean redisEnabled;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    public String generateToken(String sessionId) {
        String token = UUID.randomUUID().toString();
        String key = CSRF_TOKEN_PREFIX + sessionId;

        if (redisEnabled && redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(key, token, TOKEN_EXPIRE_HOURS, TimeUnit.HOURS);
                log.debug("CSRF token generated for session: {}", sessionId);
            } catch (Exception e) {
                log.warn("Failed to store CSRF token in Redis: {}", e.getMessage());
            }
        }

        return token;
    }

    public boolean validateToken(String sessionId, String token) {
        if (sessionId == null || token == null) {
            return false;
        }

        String key = CSRF_TOKEN_PREFIX + sessionId;

        if (redisEnabled && redisTemplate != null) {
            try {
                Object storedToken = redisTemplate.opsForValue().get(key);
                if (storedToken != null && token.equals(storedToken.toString())) {
                    log.debug("CSRF token validated for session: {}", sessionId);
                    return true;
                }
            } catch (Exception e) {
                log.warn("Failed to validate CSRF token from Redis: {}", e.getMessage());
            }
        }

        return false;
    }

    public void removeToken(String sessionId) {
        String key = CSRF_TOKEN_PREFIX + sessionId;

        if (redisEnabled && redisTemplate != null) {
            try {
                redisTemplate.delete(key);
                log.debug("CSRF token removed for session: {}", sessionId);
            } catch (Exception e) {
                log.warn("Failed to remove CSRF token from Redis: {}", e.getMessage());
            }
        }
    }
}
