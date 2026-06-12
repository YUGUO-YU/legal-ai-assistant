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
        String token = "mock_token_test123";
        assertDoesNotThrow(() -> authService.logout(token));
    }

    @Test
    void testGetUserInfo() {
        LoginResponse.UserInfo userInfo = authService.getUserInfo("any_token");

        assertNotNull(userInfo);
        assertEquals(1L, userInfo.getUserId());
        assertEquals("demo_user", userInfo.getUsername());
        assertEquals("法律用户", userInfo.getNickname());
        assertEquals("demo@example.com", userInfo.getEmail());
        assertEquals("lawyer", userInfo.getRole());
    }
}
