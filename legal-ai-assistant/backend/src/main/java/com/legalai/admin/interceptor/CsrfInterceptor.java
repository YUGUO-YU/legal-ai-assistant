package com.legalai.admin.interceptor;

import com.legalai.service.CsrfTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Set;

@Component
public class CsrfInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(CsrfInterceptor.class);

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/auth/login",
            "/auth/admin/login",
            "/auth/register",
            "/auth/forgot-password",
            "/csrf-token",
            "/company",
            "/law-search",
            "/legal-search",
            "/case-search",
            "/case-similar",
            "/document",
            "/legal-research",
            "/contract",
            "/knowledge-base",
            "/doc-qa",
            "/law-favorite",
            "/usage",
            "/progress",
            "/ppt"
    );

    @Autowired
    private CsrfTokenService csrfTokenService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String method = request.getMethod();
        if (SAFE_METHODS.contains(method.toUpperCase())) {
            return true;
        }

        String path = request.getRequestURI();
        for (String publicPath : PUBLIC_PATHS) {
            if (path.contains(publicPath)) {
                return true;
            }
        }

        String token = request.getHeader("X-CSRF-Token");
        if (token == null) {
            token = request.getParameter("_csrf");
        }

        String sessionId = request.getHeader("X-Session-Id");
        if (sessionId == null) {
            sessionId = "default";
        }

        if (!csrfTokenService.validateToken(sessionId, token)) {
            log.warn("CSRF token validation failed for session: {}, path: {}", sessionId, path);
            writeCsrfErrorResponse(response);
            return false;
        }

        return true;
    }

    private void writeCsrfErrorResponse(HttpServletResponse response) throws IOException {
        response.setStatus(403);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"code\":403,\"message\":\"CSRF token无效或已过期\"}");
    }
}
