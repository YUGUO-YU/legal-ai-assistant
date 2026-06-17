package com.legalai.service;

import com.legalai.dto.LoginRequest;
import com.legalai.dto.LoginResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final long TOKEN_EXPIRE_MS = 7 * 24 * 3600 * 1000L;
    private static final long REFRESH_TOKEN_EXPIRE_MS = 30 * 24 * 3600 * 1000L;

    private final Map<String, TokenInfo> tokenStore = new ConcurrentHashMap<>();

    public LoginResponse login(LoginRequest request) {
        log.info("用户登录请求: username={}", request.getUsername());

        String accessToken = generateMockToken();
        String refreshToken = generateMockToken();

        TokenInfo tokenInfo = new TokenInfo();
        tokenInfo.setUserId(1L);
        tokenInfo.setUsername(request.getUsername());
        tokenInfo.setExpireTime(System.currentTimeMillis() + TOKEN_EXPIRE_MS);
        tokenInfo.setRefreshExpireTime(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRE_MS);
        tokenStore.put(accessToken, tokenInfo);

        LoginResponse response = new LoginResponse();
        response.setToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpireTime(tokenInfo.getExpireTime());

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        userInfo.setUserId(1L);
        userInfo.setUsername(request.getUsername());
        userInfo.setNickname("法律用户");
        userInfo.setEmail(request.getUsername() + "@example.com");
        userInfo.setAvatar("");
        userInfo.setRole("lawyer");
        response.setUserInfo(userInfo);

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
        private String username;
        private long expireTime;
        private long refreshExpireTime;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public long getExpireTime() { return expireTime; }
        public void setExpireTime(long expireTime) { this.expireTime = expireTime; }
        public long getRefreshExpireTime() { return refreshExpireTime; }
        public void setRefreshExpireTime(long refreshExpireTime) { this.refreshExpireTime = refreshExpireTime; }
    }
}