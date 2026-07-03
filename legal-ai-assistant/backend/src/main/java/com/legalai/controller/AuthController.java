package com.legalai.controller;

import com.legalai.dto.*;
import com.legalai.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<Void> register(@RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.success(null);
    }

    @PostMapping("/forgot-password")
    public ApiResponse<ForgotPasswordResponse> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        ForgotPasswordResponse response = authService.sendResetCode(request.getUsername());
        return ApiResponse.success(response);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.success(null);
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody ChangePasswordRequest request) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authHeader = authHeader.substring(7);
        }
        authService.changePassword(authHeader, request.getOldPassword(), request.getNewPassword());
        return ApiResponse.success(null);
    }

    @PutMapping("/profile")
    public ApiResponse<Void> updateProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody UpdateProfileRequest request) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authHeader = authHeader.substring(7);
        }
        authService.updateProfile(authHeader, request.getRealName(), request.getEmail(), request.getPhone(), request.getBio());
        return ApiResponse.success(null);
    }

    @PostMapping("/admin/login")
    public ApiResponse<LoginResponse> adminLogin(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ip = httpRequest.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = httpRequest.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = httpRequest.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        request.setIp(ip);
        request.setLoginType("admin");
        LoginResponse response = authService.adminLogin(request);
        return ApiResponse.success(response);
    }

    @PostMapping("/admin/force-logout/{userId}")
    public ApiResponse<Map<String, Object>> adminForceLogout(@PathVariable Long userId) {
        int count = authService.forceLogoutUser(userId);
        return ApiResponse.success(Map.of("ok", true, "message", "已强制下线 " + count + " 个会话"));
    }

    @GetMapping("/admin/lockout-status")
    public ApiResponse<Map<String, Object>> lockoutStatus(@RequestParam String username) {
        boolean locked = authService.isAccountLocked(username);
        long remaining = authService.getLockoutRemainingMs(username);
        return ApiResponse.success(Map.of("locked", locked, "remainingMs", remaining));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ApiResponse.success(response);
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        LoginResponse response = authService.refreshToken(request.getRefreshToken());
        return ApiResponse.success(response);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        authService.logout(token);
        return ApiResponse.success(null);
    }

    @GetMapping("/user-info")
    public ApiResponse<LoginResponse.UserInfo> getUserInfo(
            @RequestHeader(value = "Authorization", required = false) String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        LoginResponse.UserInfo userInfo = authService.getUserInfo(token);
        return ApiResponse.success(userInfo);
    }
}