package com.legalai.service;

import com.legalai.dto.LoginRequest;
import com.legalai.dto.LoginResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock
    private JdbcTemplate jdbc;

    @InjectMocks
    private AuthService authService;

    @SuppressWarnings("unchecked")
    private void setupMockLoginUser(String username, String userId, String password) {
        List<Map<String, Object>> userRow = List.of(Map.of(
            "id", userId,
            "username", username,
            "password", password,
            "real_name", "Test User",
            "email", username + "@test.com",
            "status", 1,
            "approved", 1
        ));
        doReturn(userRow).when(jdbc).queryForList(anyString(), any(Object[].class));
    }

    @Test
    void testLogin_Success() {
        String password = "Test@123";
        String hashed = "8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92";
        setupMockLoginUser("testuser", "u-123456", hashed);

        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("123456");

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertTrue(response.getToken().startsWith("mock_token_"));
        assertNotNull(response.getRefreshToken());
        assertNotNull(response.getUserInfo());
        assertEquals("testuser", response.getUserInfo().getUsername());
        assertEquals("Test User", response.getUserInfo().getNickname());
        assertEquals("lawyer", response.getUserInfo().getRole());
        assertTrue(response.getExpireTime() > System.currentTimeMillis());
    }

    @Test
    void testLogin_TokenFormat() {
        String password = "Test@123";
        String hashed = "8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92";
        setupMockLoginUser("demo", "u-demo", hashed);

        LoginRequest request = new LoginRequest();
        request.setUsername("demo");
        request.setPassword("123456");

        LoginResponse response = authService.login(request);

        assertNotNull(response.getToken());
        assertTrue(response.getToken().length() >= 40);
    }

    @Test
    void testLogout() {
        String password = "Test@123";
        String hashed = "8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92";
        setupMockLoginUser("testuser", "u-123", hashed);

        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("123456");

        LoginResponse loginResponse = authService.login(request);
        assertDoesNotThrow(() -> authService.logout(loginResponse.getToken()));
    }

    @Test
    void testGetUserInfo_ValidToken() {
        String password = "Test@123";
        String hashed = "8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92";
        setupMockLoginUser("testuser", "u-123456", hashed);

        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("123456");

        LoginResponse loginResponse = authService.login(request);
        LoginResponse.UserInfo userInfo = authService.getUserInfo(loginResponse.getToken());

        assertNotNull(userInfo);
        assertEquals("testuser", userInfo.getUsername());
        assertEquals("lawyer", userInfo.getRole());
    }

    @Test
    void testGetUserInfo_InvalidToken() {
        LoginResponse.UserInfo userInfo = authService.getUserInfo("invalid_token");
        assertNull(userInfo);
    }

    @Test
    void testRefreshToken() {
        String password = "Test@123";
        String hashed = "8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92";
        setupMockLoginUser("testuser", "u-123", hashed);

        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("123456");

        LoginResponse loginResponse = authService.login(request);
        LoginResponse refreshResponse = authService.refreshToken(loginResponse.getRefreshToken());

        assertNotNull(refreshResponse);
        assertNotNull(refreshResponse.getToken());
        assertTrue(refreshResponse.getExpireTime() > System.currentTimeMillis());
    }
}
