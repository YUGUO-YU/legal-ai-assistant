package com.legalai.service;

import com.legalai.dto.LoginRequest;
import com.legalai.dto.LoginResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    public LoginResponse login(LoginRequest request) {
        log.info("用户登录请求: username={}", request.getUsername());

        LoginResponse response = new LoginResponse();
        response.setToken(generateMockToken());
        response.setExpireTime(System.currentTimeMillis() + 7 * 24 * 3600 * 1000L);

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

    public void logout(String token) {
        log.info("用户登出: token={}", token);
    }

    public LoginResponse.UserInfo getUserInfo(String token) {
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        userInfo.setUserId(1L);
        userInfo.setUsername("demo_user");
        userInfo.setNickname("法律用户");
        userInfo.setEmail("demo@example.com");
        userInfo.setAvatar("");
        userInfo.setRole("lawyer");
        return userInfo;
    }

    private String generateMockToken() {
        return "mock_token_" + UUID.randomUUID().toString().replace("-", "");
    }
}