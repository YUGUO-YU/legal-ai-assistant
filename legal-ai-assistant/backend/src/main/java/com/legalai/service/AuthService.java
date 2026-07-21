package com.legalai.service;

import com.legalai.dto.ForgotPasswordResponse;
import com.legalai.dto.LoginRequest;
import com.legalai.dto.LoginResponse;
import com.legalai.dto.RegisterRequest;
import com.legalai.dto.ResetPasswordRequest;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final long TOKEN_EXPIRE_MS = 7 * 24 * 3600 * 1000L;
    private static final long REFRESH_TOKEN_EXPIRE_MS = 30 * 24 * 3600 * 1000L;
    private static final long RESET_CODE_EXPIRE_MS = 10 * 60 * 1000L;

    private static final String INSERT_TOKEN_SQL = """
        INSERT INTO auth_tokens (token, user_id, username, expire_at) VALUES (?, ?, ?, ?)
    """;
    private static final String DELETE_TOKEN_SQL = "DELETE FROM auth_tokens WHERE token = ?";
    private static final String SELECT_TOKEN_SQL = """
        SELECT user_id, username FROM auth_tokens WHERE token = ? AND expire_at > NOW()
    """;
    private static final String DELETE_EXPIRED_TOKENS_SQL = "DELETE FROM auth_tokens WHERE expire_at < NOW()";

    private final Map<String, TokenInfo> tokenStore = new ConcurrentHashMap<>();
    private final Map<String, LoginFailureInfo> loginFailures = new ConcurrentHashMap<>();
    private final JdbcTemplate jdbc;
    private final Random random = new Random();

    private static final int MAX_LOGIN_FAILURES = 5;
    private static final long LOCKOUT_DURATION_MS = 5 * 60 * 1000L;

    public AuthService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private void persistToken(String token, String userId, String username, long expireTime) {
        try {
            java.sql.Timestamp expireAt = new java.sql.Timestamp(expireTime);
            jdbc.update(INSERT_TOKEN_SQL, token, userId, username, expireAt);
            log.debug("Token persisted to database for user {}", userId);
        } catch (Exception e) {
            log.warn("Failed to persist token to database: {}", e.getMessage());
        }
    }

    private void removePersistedToken(String token) {
        try {
            jdbc.update(DELETE_TOKEN_SQL, token);
            log.debug("Token removed from database");
        } catch (Exception e) {
            log.warn("Failed to remove token from database: {}", e.getMessage());
        }
    }

    public TokenUserInfo getUserIdFromToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        TokenInfo info = tokenStore.get(token);
        if (info != null && System.currentTimeMillis() <= info.getExpireTime()) {
            TokenUserInfo result = new TokenUserInfo();
            result.userId = info.getUserIdStr();
            result.username = info.getUsername();
            return result;
        }

        try {
            var rows = jdbc.queryForList(SELECT_TOKEN_SQL, token);
            if (!rows.isEmpty()) {
                TokenUserInfo result = new TokenUserInfo();
                result.userId = (String) rows.get(0).get("user_id");
                result.username = (String) rows.get(0).get("username");
                return result;
            }
        } catch (Exception e) {
            log.debug("Database token lookup failed: {}", e.getMessage());
        }

        return null;
    }

    private boolean passwordMatches(String raw, String stored) {
        if (stored == null) return false;
        String hashed = DigestUtils.sha256Hex(raw.getBytes(StandardCharsets.UTF_8));
        return hashed.equalsIgnoreCase(stored);
    }

    public LoginResponse login(LoginRequest request) {
        log.info("用户登录请求: username={}", request.getUsername());

        var rows = jdbc.queryForList(
            "SELECT id, username, password, real_name, email, status, approved FROM frontend_user WHERE username = ?",
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

        Integer approved = (Integer) user.get("approved");
        if (approved == null || approved != 1) {
            log.warn("用户未审核: {}", request.getUsername());
            throw new RuntimeException("账号待审核，请等待管理员批准");
        }

        String dbPassword = (String) user.get("password");
        if (dbPassword == null || !passwordMatches(request.getPassword(), dbPassword)) {
            log.warn("用户密码错误: username={}", request.getUsername());
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
        persistToken(accessToken, userId, request.getUsername(), tokenInfo.getExpireTime());

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
        try {
            jdbc.update("UPDATE frontend_user SET last_login_at = NOW(), last_login_ip = ? WHERE id = ?",
                request.getIp() != null ? request.getIp() : "unknown", userId);
            jdbc.update("INSERT INTO user_login_history (user_id, username, ip, login_type) VALUES (?, ?, ?, 'frontend')",
                userId, request.getUsername(), request.getIp() != null ? request.getIp() : "unknown");
        } catch (Exception e) {
            log.warn("记录登录历史失败: {}", e.getMessage());
        }
        try { String loginParams = "{\"username\":\"" + request.getUsername() + "\",\"password\":\"****\"}"; recordAudit(0L, request.getUsername(), "LOGIN", "AUTH", "frontend_user", userId, "/api/v1/auth/login", "POST", loginParams, "ok", null, 0, true, null); } catch (Exception e) { log.warn("审计日志写入失败: {}", e.getMessage()); }
        return response;
    }

    public LoginResponse adminLogin(LoginRequest request) {
        log.info("管理员登录请求: username={}", request.getUsername());

        if (isAccountLocked(request.getUsername())) {
            long remaining = getLockoutRemainingMs(request.getUsername());
            long minutes = (remaining + 59999) / 60000;
            log.warn("管理员账号已被锁定: username={}, 剩余 {} 分钟", request.getUsername(), minutes);
            throw new RuntimeException("账号已被锁定，请在 " + minutes + " 分钟后重试");
        }

        var rows = jdbc.queryForList(
            "SELECT id, username, password, real_name, status, last_login_at FROM admin_user WHERE username = ?",
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
            recordLoginFailure(request.getUsername());
            log.warn("管理员密码错误: username={}", request.getUsername());
            if (isAccountLocked(request.getUsername())) {
                throw new RuntimeException("连续" + MAX_LOGIN_FAILURES + "次登录失败，账号已锁定5分钟");
            }
            throw new RuntimeException("账号或密码错误");
        }

        Long userId = ((Number) user.get("id")).longValue();
        String realName = (String) user.get("real_name");
        Object lastLoginAt = user.get("last_login_at");

        clearLoginFailures(request.getUsername());

        try {
            jdbc.update("UPDATE admin_user SET last_login_at = NOW(), last_login_ip = ? WHERE id = ?",
                request.getIp() != null ? request.getIp() : "unknown", userId);
        } catch (Exception e) {
            log.debug("更新最后登录时间失败: {}", e.getMessage());
        }

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

        log.info("管理员登录成功: username={}, userId={}, lastLogin={}", request.getUsername(), userId, lastLoginAt);
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
        removePersistedToken(token);
    }

    public void changePassword(String token, String oldPassword, String newPassword) {
        TokenInfo tokenInfo = tokenStore.get(token);
        if (tokenInfo == null) {
            throw new RuntimeException("未登录或登录已过期");
        }

        String userIdStr = tokenInfo.getUserIdStr();
        if (userIdStr == null) {
            throw new RuntimeException("用户ID无效");
        }

        var rows = jdbc.queryForList(
            "SELECT password FROM frontend_user WHERE id = ?", userIdStr);
        if (rows.isEmpty()) {
            throw new RuntimeException("用户不存在");
        }

        String dbPassword = (String) rows.get(0).get("password");
        if (!passwordMatches(oldPassword, dbPassword)) {
            throw new RuntimeException("当前密码错误");
        }

        if (newPassword.length() < 6 || newPassword.length() > 32) {
            throw new RuntimeException("新密码长度需在6-32个字符之间");
        }

        String strengthError = validatePasswordStrength(newPassword);
        if (strengthError != null) {
            throw new RuntimeException(strengthError);
        }

        String hashedPassword = DigestUtils.sha256Hex(newPassword.getBytes(StandardCharsets.UTF_8));
        jdbc.update("UPDATE frontend_user SET password = ? WHERE id = ?", hashedPassword, userIdStr);
        log.info("密码修改成功: userId={}", userIdStr);
    }

    public void updateProfile(String token, String realName, String email, String phone, String bio) {
        TokenInfo tokenInfo = tokenStore.get(token);
        if (tokenInfo == null) {
            throw new RuntimeException("未登录或登录已过期");
        }

        String userIdStr = tokenInfo.getUserIdStr();
        if (userIdStr == null) {
            throw new RuntimeException("用户ID无效");
        }

        if (email != null && !email.isEmpty()) {
            var existingEmail = jdbc.queryForList(
                "SELECT id FROM frontend_user WHERE email = ? AND id != ?", email, userIdStr);
            if (!existingEmail.isEmpty()) {
                throw new RuntimeException("邮箱已被其他用户使用");
            }
        }

        jdbc.update(
            "UPDATE frontend_user SET real_name = ?, email = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
            realName, email, userIdStr);
        log.info("个人信息更新成功: userId={}", userIdStr);
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

    public void register(RegisterRequest request) {
        String username = request.getUsername().trim();
        String password = request.getPassword();
        String realName = request.getRealName() != null ? request.getRealName().trim() : null;
        String email = request.getEmail() != null ? request.getEmail().trim() : null;

        if (username.length() < 3 || username.length() > 32) {
            throw new RuntimeException("用户名长度需在3-32个字符之间");
        }
        String strengthError = validatePasswordStrength(password);
        if (strengthError != null) {
            throw new RuntimeException(strengthError);
        }

        var existing = jdbc.queryForList(
            "SELECT id FROM frontend_user WHERE username = ?", username);
        if (!existing.isEmpty()) {
            throw new RuntimeException("用户名已被注册");
        }

        if (email != null && !email.isEmpty()) {
            var existingEmail = jdbc.queryForList(
                "SELECT id FROM frontend_user WHERE email = ?", email);
            if (!existingEmail.isEmpty()) {
                throw new RuntimeException("邮箱已被注册");
            }
        }

        String userId = "u-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String hashedPassword = DigestUtils.sha256Hex(password.getBytes(StandardCharsets.UTF_8));

        jdbc.update(
            "INSERT INTO frontend_user (id, username, password, real_name, email, status, approved) VALUES (?, ?, ?, ?, ?, 1, 0)",
            userId, username, hashedPassword, realName, email);

        log.info("新用户注册成功: username={}, userId={}, 待审核", username, userId);
        try { String regParams = "{\"username\":\"" + username + "\",\"password\":\"****\",\"email\":\"" + (email != null ? email : "") + "\"}"; recordAudit(0L, username, "REGISTER", "AUTH", "frontend_user", userId, "/api/v1/auth/register", "POST", regParams, "ok", null, 0, true, null); } catch (Exception e) { log.warn("审计日志写入失败: {}", e.getMessage()); }
    }

    public ForgotPasswordResponse sendResetCode(String username) {
        var rows = jdbc.queryForList(
            "SELECT id FROM frontend_user WHERE username = ?", username);
        if (rows.isEmpty()) {
            throw new RuntimeException("该用户名不存在");
        }

        String code = String.format("%06d", random.nextInt(1000000));
        java.sql.Timestamp expireAt = new java.sql.Timestamp(System.currentTimeMillis() + RESET_CODE_EXPIRE_MS);

        jdbc.update("DELETE FROM password_reset_code WHERE username = ? AND used = 0", username);
        jdbc.update("INSERT INTO password_reset_code (username, code, expire_at) VALUES (?, ?, ?)", username, code, expireAt);

        log.info("密码重置码已生成: username={}", username);
        return new ForgotPasswordResponse(code, "验证码发送成功，10分钟内有效");
    }

    public void resetPassword(ResetPasswordRequest request) {
        String username = request.getUsername().trim();
        String code = request.getCode().trim();
        String newPassword = request.getNewPassword();

        String strengthError = validatePasswordStrength(newPassword);
        if (strengthError != null) {
            throw new RuntimeException(strengthError);
        }

        var codeRows = jdbc.queryForList(
            "SELECT id, expire_at FROM password_reset_code WHERE username = ? AND code = ? AND used = 0 ORDER BY id DESC LIMIT 1",
            username, code);
        if (codeRows.isEmpty()) {
            throw new RuntimeException("验证码错误或已过期");
        }
        long expireTime = ((java.sql.Timestamp) codeRows.get(0).get("expire_at")).getTime();
        if (System.currentTimeMillis() > expireTime) {
            jdbc.update("DELETE FROM password_reset_code WHERE username = ? AND code = ?", username, code);
            throw new RuntimeException("验证码已过期，请重新获取");
        }

        String hashedPassword = DigestUtils.sha256Hex(newPassword.getBytes(StandardCharsets.UTF_8));
        jdbc.update("UPDATE frontend_user SET password = ? WHERE username = ?", hashedPassword, username);
        jdbc.update("DELETE FROM password_reset_code WHERE username = ? AND code = ?", username, code);

        log.info("密码重置成功: username={}", username);
        try { String resetParams = "{\"username\":\"" + username + "\",\"code\":\"****\",\"newPassword\":\"****\"}"; recordAudit(0L, username, "PASSWORD_RESET", "AUTH", "frontend_user", null, "/api/v1/auth/reset-password", "POST", resetParams, "ok", null, 0, true, null); } catch (Exception e) { log.warn("审计日志写入失败: {}", e.getMessage()); }
    }

    private void recordAudit(Long userId, String username, String operation, String bizModule, String bizType, String bizId, String url, String method, String params, String result, String ip, int duration, boolean ok, String error) {
        try {
            jdbc.update("INSERT INTO admin_audit_log(user_id, username, operation, biz_module, biz_type, biz_id, request_url, request_method, request_params, response_result, ip, duration_ms, status, error_msg, trace_id, created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW())",
                userId, username, operation, bizModule, bizType, bizId, url, method, truncate(params, 4000), truncate(result, 4000), ip, duration, ok ? 1 : 0, error, java.util.UUID.randomUUID().toString());
        } catch (Exception e) {
            log.warn("审计日志记录失败: {}", e.getMessage());
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }

    private static class LoginFailureInfo {
        int attempts;
        long lockoutUntil;
        LoginFailureInfo(int attempts, long lockoutUntil) {
            this.attempts = attempts;
            this.lockoutUntil = lockoutUntil;
        }
    }

    public String validatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return "密码不能为空";
        }
        if (password.length() < 8) {
            return "密码长度至少8位";
        }
        if (password.length() > 32) {
            return "密码长度不能超过32位";
        }
        if (!password.matches(".*[A-Z].*")) {
            return "密码必须包含至少一个大写字母";
        }
        if (!password.matches(".*[a-z].*")) {
            return "密码必须包含至少一个小写字母";
        }
        if (!password.matches(".*[0-9].*")) {
            return "密码必须包含至少一个数字";
        }
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            return "密码必须包含至少一个特殊字符";
        }
        return null;
    }

    public boolean isAccountLocked(String username) {
        LoginFailureInfo info = loginFailures.get(username);
        if (info == null) return false;
        if (System.currentTimeMillis() > info.lockoutUntil) {
            loginFailures.remove(username);
            return false;
        }
        return info.attempts >= MAX_LOGIN_FAILURES;
    }

    public long getLockoutRemainingMs(String username) {
        LoginFailureInfo info = loginFailures.get(username);
        if (info == null) return 0;
        long remaining = info.lockoutUntil - System.currentTimeMillis();
        return remaining > 0 ? remaining : 0;
    }

    public void recordLoginFailure(String username) {
        long now = System.currentTimeMillis();
        LoginFailureInfo info = loginFailures.compute(username, (k, v) -> {
            if (v == null || now > v.lockoutUntil) {
                return new LoginFailureInfo(1, now + LOCKOUT_DURATION_MS);
            } else {
                v.attempts++;
                return v;
            }
        });
        log.warn("登录失败累计: username={}, attempts={}", username, info.attempts);
    }

    public void clearLoginFailures(String username) {
        loginFailures.remove(username);
    }

    public int forceLogoutUser(Long userId) {
        int count = 0;
        var tokensToRemove = tokenStore.entrySet().stream()
            .filter(e -> userId.equals(e.getValue().getUserId()))
            .map(Map.Entry::getKey)
            .toList();
        for (String t : tokensToRemove) {
            tokenStore.remove(t);
            removePersistedToken(t);
            count++;
        }
        log.info("强制下线用户 userId={}, 清除 {} 个token", userId, count);
        return count;
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

    public static class TokenUserInfo {
        public String userId;
        public String username;
    }
}