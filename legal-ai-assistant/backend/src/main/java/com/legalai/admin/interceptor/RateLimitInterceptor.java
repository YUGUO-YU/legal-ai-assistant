package com.legalai.admin.interceptor;

import com.legalai.admin.annotation.RateLimit;
import com.legalai.utils.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private final ConcurrentHashMap<String, RateLimiter> limiters = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod hm)) {
            return true;
        }

        Method method = hm.getMethod();
        RateLimit annotation = method.getAnnotation(RateLimit.class);
        if (annotation == null) {
            Class<?> clazz = hm.getBeanType();
            annotation = clazz.getAnnotation(RateLimit.class);
        }
        if (annotation == null) {
            return true;
        }

        String key = annotation.key();
        int qps = annotation.qps();

        RateLimiter limiter = limiters.computeIfAbsent(key, k -> new RateLimiter(qps, 1000));
        if (!limiter.tryAcquire(key)) {
            log.warn("[RateLimit] Blocked: key={}, qps={}, path={}", key, qps, request.getRequestURI());
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁，请稍后重试\"}");
            return false;
        }
        return true;
    }
}
