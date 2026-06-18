package com.legalai.config;

import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
public class OpenClawHealthCheck implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(OpenClawHealthCheck.class);

    @Value("${ai.openclaw.url:http://localhost:19001}")
    private String openClawUrl;

    @Value("${ai.openclaw.token:my-secret-token}")
    private String openClawToken;

    @Value("${ai.openclaw.auto-start:true}")
    private boolean autoStartOpenClaw;

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

        if (autoStartOpenClaw) {
            log.info("尝试自动启动 OpenClaw Gateway...");
            startOpenClaw();
        } else {
            log.warn("OpenClaw Gateway 未就绪，请手动启动或设置 ai.openclaw.auto-start=true");
        }
    }

    private void startOpenClaw() {
        try {
            ProcessBuilder pb = new ProcessBuilder("openclaw", "gateway");
            pb.redirectOutput(new java.io.File("/tmp/openclaw_stderr.log"));
            pb.redirectError(new java.io.File("/tmp/openclaw_stderr.log"));
            Process p = pb.start();
            log.info("OpenClaw Gateway 启动命令已执行，PID: {}", p.pid());

            // 等待 OpenClaw 启动
            Thread.sleep(8000);

            // 检查是否启动成功
            if (checkOpenClaw()) {
                log.info("OpenClaw Gateway 自动启动成功！");
            } else {
                log.error("OpenClaw Gateway 自动启动失败，请检查日志");
            }
        } catch (IOException e) {
            log.error("启动 OpenClaw Gateway 失败: {}", e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean checkOpenClaw() {
        try {
            Request request = new Request.Builder()
                    .url(openClawUrl + "/v1/models")
                    .addHeader("Authorization", "Bearer " + openClawToken)
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            return false;
        }
    }
}
