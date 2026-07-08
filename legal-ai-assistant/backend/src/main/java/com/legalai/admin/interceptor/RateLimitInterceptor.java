package com.legalai.admin.interceptor;

import com.legalai.admin.annotation.RateLimit;
import com.google.common.util.concurrent.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod method) {
            RateLimit annotation = method.getMethodAnnotation(RateLimit.class);
            if (annotation == null) {
                return true;
            }

            String limiterKey = annotation.key();
            RateLimiter limiter = limiters.computeIfAbsent(limiterKey,
                k -> RateLimiter.create(annotation.qps()));

            if (!limiter.tryAcquire()) {
                writeRateLimitResponse(response);
                return false;
            }
        }
        return true;
    }

    private void writeRateLimitResponse(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\"}");
    }
}
