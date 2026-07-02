package com.legalai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class LawFavoriteService {

    @Autowired
    private JdbcTemplate jdbc;

    private static final String INSERT_FAVORITE = """
        INSERT IGNORE INTO law_favorite (user_id, law_uuid, law_title) VALUES (?, ?, ?)
    """;

    private static final String DELETE_FAVORITE = """
        DELETE FROM law_favorite WHERE user_id = ? AND law_uuid = ?
    """;

    private static final String SELECT_FAVORITES = """
        SELECT law_uuid, law_title, created_at FROM law_favorite WHERE user_id = ? ORDER BY created_at DESC
    """;

    private static final String CHECK_FAVORITE = """
        SELECT COUNT(*) FROM law_favorite WHERE user_id = ? AND law_uuid = ?
    """;

    private static final String SELECT_TOKEN_SQL = """
        SELECT user_id, username FROM auth_tokens WHERE token = ? AND expire_at > NOW()
    """;

    public void addFavorite(String token, String lawUuid, String lawTitle) {
        String userId = getUserIdFromToken(token);
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }

        int updated = jdbc.update(INSERT_FAVORITE, userId, lawUuid, lawTitle);
        if (updated > 0) {
            log.info("用户 {} 收藏法规 {}", userId, lawUuid);
        }
    }

    public void removeFavorite(String token, String lawUuid) {
        String userId = getUserIdFromToken(token);
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }

        jdbc.update(DELETE_FAVORITE, userId, lawUuid);
        log.info("用户 {} 取消收藏法规 {}", userId, lawUuid);
    }

    public List<Map<String, Object>> listFavorites(String token) {
        String userId = getUserIdFromToken(token);
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }

        return jdbc.queryForList(SELECT_FAVORITES, userId);
    }

    public boolean isFavorited(String token, String lawUuid) {
        String userId = getUserIdFromToken(token);
        if (userId == null) {
            return false;
        }

        Integer count = jdbc.queryForObject(CHECK_FAVORITE, Integer.class, userId, lawUuid);
        return count != null && count > 0;
    }

    private String getUserIdFromToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        try {
            var rows = jdbc.queryForList(SELECT_TOKEN_SQL, token);
            if (!rows.isEmpty()) {
                return (String) rows.get(0).get("user_id");
            }
        } catch (Exception e) {
            log.debug("Token validation failed: {}", e.getMessage());
        }

        return null;
    }
}
