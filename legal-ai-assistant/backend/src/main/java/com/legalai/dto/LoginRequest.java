package com.legalai.dto;

public class LoginRequest {
    private String username;
    private String password;
    private String captcha;
    private String captchaKey;
    private String ip;
    private String loginType;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getCaptcha() { return captcha; }
    public void setCaptcha(String captcha) { this.captcha = captcha; }
    public String getCaptchaKey() { return captchaKey; }
    public void setCaptchaKey(String captchaKey) { this.captchaKey = captchaKey; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public String getLoginType() { return loginType; }
    public void setLoginType(String loginType) { this.loginType = loginType; }
}