package com.legalai.service;

import com.legalai.dto.LoginRequest;
import com.legalai.dto.LoginResponse;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final long TOKEN_EXPIRE_MS = 7 * 24 * 3600 * 1000L;
    private static final long REFRESH_TOKEN_EXPIRE_MS = 30 * 24 * 3600 * 1000L;

    private final Map<String, TokenInfo> tokenStore = new ConcurrentHashMap<>();
    private final JdbcTemplate jdbc;

    public AuthService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private boolean passwordMatches(String raw, String stored) {
        if (stored == null) return false;
        String hashed = DigestUtils.sha256Hex(raw.getBytes(StandardCharsets.UTF_8));
        return hashed.equalsIgnoreCase(stored);
    }

    public LoginResponse login(LoginRequest request) {
        log.info("用户登录请求: username={}", request.getUsername());

        var rows = jdbc.queryForList(
            "SELECT id, username, password, real_name, email, status FROM frontend_user WHERE username = ?",
            request.getUsername());

        if (rows.isEmpty()) {
            log.warn("用户不存在: {}", request.getUsername());
            throw new RuntimeException("账号或密码错误");
        }

        Map<String, Object> user = rows.get(0);
        Integer status = (Integer) user.get("status");
        if (status == null || status != 1) {
            log.warn("用户已停用: {}", request.getUsername());
            throw new RuntimeException("账号已被停用");
        }

        String dbPassword = (String) user.get("password");
        if (dbPassword == null || !passwordMatches(request.getPassword(), dbPassword)) {
            log.warn("用户密码错误: {}", request.getUsername());
            throw new RuntimeException("账号或密码错误");
        }

        String userId = (String) user.get("id");
        String realName = (String) user.get("real_name");
        String email = (String) user.get("email");

        String accessToken = generateMockToken();
        String refreshToken = generateMockToken();

        TokenInfo tokenInfo = new TokenInfo();
        tokenInfo.setUserIdStr(userId);
        tokenInfo.setUsername(request.getUsername());
        tokenInfo.setExpireTime(System.currentTimeMillis() + TOKEN_EXPIRE_MS);
        tokenInfo.setRefreshExpireTime(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRE_MS);
        tokenStore.put(accessToken, tokenInfo);

        LoginResponse response = new LoginResponse();
        response.setToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpireTime(tokenInfo.getExpireTime());

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        userInfo.setUserIdStr(userId);
        userInfo.setUsername(request.getUsername());
        userInfo.setNickname(realName != null ? realName : "法律用户");
        userInfo.setEmail(email != null ? email : request.getUsername() + "@example.com");
        userInfo.setAvatar("");
        userInfo.setRole("lawyer");
        response.setUserInfo(userInfo);

        log.info("用户登录成功: username={}, userId={}", request.getUsername(), userId);
        return response;
    }

    public LoginResponse adminLogin(LoginRequest request) {
        log.info("管理员登录请求: username={}", request.getUsername());

        var rows = jdbc.queryForList(
            "SELECT id, username, password, real_name, status FROM admin_user WHERE username = ?",
            request.getUsername());

        if (rows.isEmpty()) {
            log.warn("管理员账号不存在: {}", request.getUsername());
            throw new RuntimeException("账号或密码错误");
        }

        Map<String, Object> user = rows.get(0);
        Integer status = (Integer) user.get("status");
        if (status == null || status != 1) {
            log.warn("管理员账号已停用: {}", request.getUsername());
            throw new RuntimeException("账号已被停用，请联系超级管理员");
        }

        String dbPassword = (String) user.get("password");
        if (dbPassword == null || !passwordMatches(request.getPassword(), dbPassword)) {
            log.warn("管理员密码错误: {}", request.getUsername());
            throw new RuntimeException("账号或密码错误");
        }

        Long userId = ((Number) user.get("id")).longValue();
        String realName = (String) user.get("real_name");

        String accessToken = "admin_" + UUID.randomUUID().toString().replace("-", "");
        String refreshToken = "admin_" + UUID.randomUUID().toString().replace("-", "");

        TokenInfo tokenInfo = new TokenInfo();
        tokenInfo.setUserId(userId);
        tokenInfo.setUsername(request.getUsername());
        tokenInfo.setExpireTime(System.currentTimeMillis() + TOKEN_EXPIRE_MS);
        tokenInfo.setRefreshExpireTime(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRE_MS);
        tokenStore.put(accessToken, tokenInfo);

        LoginResponse response = new LoginResponse();
        response.setToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpireTime(tokenInfo.getExpireTime());

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        userInfo.setUserId(userId);
        userInfo.setUsername(request.getUsername());
        userInfo.setNickname(realName != null ? realName : "管理员");
        userInfo.setEmail(request.getUsername() + "@legal-ai.local");
        userInfo.setAvatar("");
        userInfo.setRole("admin");
        response.setUserInfo(userInfo);

        log.info("管理员登录成功: username={}, userId={}", request.getUsername(), userId);
        return response;
    }

    public LoginResponse refreshToken(String refreshToken) {
        log.info("刷新令牌请求");
        LoginResponse response = new LoginResponse();
        response.setToken(generateMockToken());
        response.setRefreshToken(refreshToken);
        response.setExpireTime(System.currentTimeMillis() + TOKEN_EXPIRE_MS);
        return response;
    }

    public void logout(String token) {
        log.info("用户登出: token={}", token);
        tokenStore.remove(token);
    }

    public LoginResponse.UserInfo getUserInfo(String token) {
        TokenInfo tokenInfo = tokenStore.get(token);
        if (tokenInfo == null) {
            log.warn("无效的令牌: {}", token);
            return null;
        }

        if (System.currentTimeMillis() > tokenInfo.getExpireTime()) {
            log.warn("令牌已过期: {}", token);
            tokenStore.remove(token);
            return null;
        }

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        userInfo.setUserId(tokenInfo.getUserId());
        userInfo.setUserIdStr(tokenInfo.getUserIdStr());
        userInfo.setUsername(tokenInfo.getUsername());
        userInfo.setNickname("法律用户");
        userInfo.setEmail(tokenInfo.getUsername() + "@example.com");
        userInfo.setAvatar("");
        userInfo.setRole("lawyer");
        return userInfo;
    }

    private String generateMockToken() {
        return "mock_token_" + UUID.randomUUID().toString().replace("-", "");
    }

    private static class TokenInfo {
        private Long userId;
        private String userIdStr;
        private String username;
        private long expireTime;
        private long refreshExpireTime;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getUserIdStr() { return userIdStr; }
        public void setUserIdStr(String userIdStr) { this.userIdStr = userIdStr; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public long getExpireTime() { return expireTime; }
        public void setExpireTime(long expireTime) { this.expireTime = expireTime; }
        public long getRefreshExpireTime() { return refreshExpireTime; }
        public void setRefreshExpireTime(long refreshExpireTime) { this.refreshExpireTime = refreshExpireTime; }
    }
}