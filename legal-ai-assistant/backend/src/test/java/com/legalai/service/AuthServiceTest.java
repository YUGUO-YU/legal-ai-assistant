package com.legalai.service;

import com.legalai.dto.LoginRequest;
import com.legalai.dto.LoginResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Test
    void testLogin_Success() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertTrue(response.getToken().startsWith("mock_token_"));
        assertNotNull(response.getRefreshToken());
        assertNotNull(response.getUserInfo());
        assertEquals("testuser", response.getUserInfo().getUsername());
        assertEquals("法律用户", response.getUserInfo().getNickname());
        assertEquals("testuser@example.com", response.getUserInfo().getEmail());
        assertEquals("lawyer", response.getUserInfo().getRole());
        assertTrue(response.getExpireTime() > System.currentTimeMillis());
    }

    @Test
    void testLogin_TokenFormat() {
        LoginRequest request = new LoginRequest();
        request.setUsername("demo");
        request.setPassword("demo123");

        LoginResponse response = authService.login(request);

        assertNotNull(response.getToken());
        assertTrue(response.getToken().length() >= 40);
    }

    @Test
    void testLogout() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        LoginResponse loginResponse = authService.login(request);
        assertDoesNotThrow(() -> authService.logout(loginResponse.getToken()));
    }

    @Test
    void testGetUserInfo_ValidToken() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        LoginResponse loginResponse = authService.login(request);
        LoginResponse.UserInfo userInfo = authService.getUserInfo(loginResponse.getToken());

        assertNotNull(userInfo);
        assertEquals(1L, userInfo.getUserId());
        assertEquals("testuser", userInfo.getUsername());
        assertEquals("法律用户", userInfo.getNickname());
        assertEquals("testuser@example.com", userInfo.getEmail());
        assertEquals("lawyer", userInfo.getRole());
    }

    @Test
    void testGetUserInfo_InvalidToken() {
        LoginResponse.UserInfo userInfo = authService.getUserInfo("invalid_token");
        assertNull(userInfo);
    }

    @Test
    void testRefreshToken() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        LoginResponse loginResponse = authService.login(request);
        LoginResponse refreshResponse = authService.refreshToken(loginResponse.getRefreshToken());

        assertNotNull(refreshResponse);
        assertNotNull(refreshResponse.getToken());
        assertTrue(refreshResponse.getExpireTime() > System.currentTimeMillis());
    }
}
