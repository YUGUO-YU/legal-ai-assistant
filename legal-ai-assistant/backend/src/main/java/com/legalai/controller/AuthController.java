package com.legalai.controller;

import com.legalai.dto.*;
import com.legalai.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin
@Tag(name = "认证授权", description = "用户登录、注册、密码重置等认证相关接口")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "用户注册", description = "注册新用户账号")
    @PostMapping("/register")
    public ApiResponse<Void> register(@RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.success(null);
    }

    @Operation(summary = "忘记密码", description = "发送密码重置验证码到用户邮箱")
    @PostMapping("/forgot-password")
    public ApiResponse<ForgotPasswordResponse> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        ForgotPasswordResponse response = authService.sendResetCode(request.getUsername());
        return ApiResponse.success(response);
    }

    @Operation(summary = "重置密码", description = "使用验证码重置用户密码")
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.success(null);
    }

    @Operation(summary = "修改密码", description = "已登录用户修改自己的密码")
    @PutMapping("/password")
    public ApiResponse<Void> changePassword(
            @Parameter(description = "Bearer Token") @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody ChangePasswordRequest request) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authHeader = authHeader.substring(7);
        }
        authService.changePassword(authHeader, request.getOldPassword(), request.getNewPassword());
        return ApiResponse.success(null);
    }

    @Operation(summary = "更新个人资料", description = "已登录用户更新个人资料信息")
    @PutMapping("/profile")
    public ApiResponse<Void> updateProfile(
            @Parameter(description = "Bearer Token") @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody UpdateProfileRequest request) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authHeader = authHeader.substring(7);
        }
        authService.updateProfile(authHeader, request.getRealName(), request.getEmail(), request.getPhone(), request.getBio());
        return ApiResponse.success(null);
    }

    @Operation(summary = "管理员登录", description = "后台管理员登录系统")
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

    @Operation(summary = "强制用户下线", description = "管理员强制指定用户下线所有会话")
    @PostMapping("/admin/force-logout/{userId}")
    public ApiResponse<Map<String, Object>> adminForceLogout(
            @Parameter(description = "用户ID") @PathVariable Long userId) {
        int count = authService.forceLogoutUser(userId);
        return ApiResponse.success(Map.of("ok", true, "message", "已强制下线 " + count + " 个会话"));
    }

    @Operation(summary = "账号锁定状态", description = "查询指定用户账号的锁定状态")
    @GetMapping("/admin/lockout-status")
    public ApiResponse<Map<String, Object>> lockoutStatus(
            @Parameter(description = "用户名") @RequestParam String username) {
        boolean locked = authService.isAccountLocked(username);
        long remaining = authService.getLockoutRemainingMs(username);
        return ApiResponse.success(Map.of("locked", locked, "remainingMs", remaining));
    }

    @Operation(summary = "用户登录", description = "普通用户登录系统")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return ApiResponse.success(response);
        } catch (RuntimeException e) {
            return ApiResponse.error(401, e.getMessage());
        }
    }

    @Operation(summary = "刷新Token", description = "使用刷新令牌获取新的访问令牌")
    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        LoginResponse response = authService.refreshToken(request.getRefreshToken());
        return ApiResponse.success(response);
    }

    @Operation(summary = "用户登出", description = "使当前访问令牌失效")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @Parameter(description = "Bearer Token") @RequestHeader(value = "Authorization", required = false) String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        authService.logout(token);
        return ApiResponse.success(null);
    }

    @Operation(summary = "获取用户信息", description = "获取当前登录用户的详细信息")
    @GetMapping("/user-info")
    public ApiResponse<LoginResponse.UserInfo> getUserInfo(
            @Parameter(description = "Bearer Token") @RequestHeader(value = "Authorization", required = false) String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        LoginResponse.UserInfo userInfo = authService.getUserInfo(token);
        return ApiResponse.success(userInfo);
    }
}