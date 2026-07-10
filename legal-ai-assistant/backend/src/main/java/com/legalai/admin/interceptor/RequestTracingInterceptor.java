package com.legalai.admin.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
public class RequestTracingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RequestTracingInterceptor.class);
    private static final String TRACE_ID_HEADER = "X-Trace-ID";
    private static final String TRACE_ID_MDC_KEY = "traceId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isEmpty()) {
            traceId = generateTraceId();
        }

        MDC.put(TRACE_ID_MDC_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);

        long startTime = System.currentTimeMillis();
        request.setAttribute("startTime", startTime);

        log.debug("请求开始: {} {} [traceId={}]", request.getMethod(), request.getRequestURI(), traceId);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Long startTime = (Long) request.getAttribute("startTime");
        long duration = startTime != null ? System.currentTimeMillis() - startTime : 0;

        String traceId = MDC.get(TRACE_ID_MDC_KEY);
        int status = response.getStatus();

        if (ex != null) {
            log.error("请求异常: {} {} [traceId={}] status={} duration={}ms",
                    request.getMethod(), request.getRequestURI(), traceId, status, duration, ex);
        } else {
            log.info("请求完成: {} {} [traceId={}] status={} duration={}ms",
                    request.getMethod(), request.getRequestURI(), traceId, status, duration);
        }

        MDC.remove(TRACE_ID_MDC_KEY);
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
