package com.legalai.config;

import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class OpenClawHealthCheck implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(OpenClawHealthCheck.class);

    @Value("${ai.openclaw.url:http://localhost:19001}")
    private String openClawUrl;

    @Value("${ai.openclaw.token:my-secret-token}")
    private String openClawToken;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    @Override
    public void run(ApplicationArguments args) {
        log.info("检查 OpenClaw Gateway 连接状态...");

        int maxRetries = 5;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                Request request = new Request.Builder()
                        .url(openClawUrl + "/v1/models")
                        .addHeader("Authorization", "Bearer " + openClawToken)
                        .get()
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        log.info("OpenClaw Gateway 连接正常");
                        return;
                    } else {
                        log.warn("OpenClaw Gateway 响应异常: {}", response.code());
                    }
                }
            } catch (Exception e) {
                log.error("OpenClaw Gateway 连接失败: {}", e.getMessage());
            }

            retryCount++;
            if (retryCount < maxRetries) {
                log.info("等待 5 秒后重试... ({}/{})", retryCount, maxRetries);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        log.warn("OpenClaw Gateway 未就绪，但应用将继续启动");
    }
}
