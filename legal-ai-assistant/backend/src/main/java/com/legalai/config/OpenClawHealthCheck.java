package com.legalai.config;

import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    @Value("${ai.openclaw.binary-path:}")
    private String openClawBinaryPath;

    @Value("${ai.openclaw.config-path:}")
    private String openClawConfigPath;

    @Value("${ai.openclaw.log-path:}")
    private String openClawLogPath;

    @Value("${ai.openclaw.gateway-args:gateway --allow-unconfigured}")
    private String openClawGatewayArgs;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    @Override
    public void run(ApplicationArguments args) {
        log.info("检查 OpenClaw Gateway 连接状态: {}", openClawUrl);

        if (checkOpenClaw()) {
            log.info("OpenClaw Gateway 已就绪，跳过自动启动");
            return;
        }

        if (!autoStartOpenClaw) {
            log.warn("OpenClaw Gateway 未就绪且已禁用自动启动，请手动启动或设置 ai.openclaw.auto-start=true");
            return;
        }

        log.info("OpenClaw Gateway 未就绪，尝试自动启动...");
        startOpenClaw();
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
            log.debug("OpenClaw 健康检查失败: {}", e.getMessage());
            return false;
        }
    }

    private void startOpenClaw() {
        String resolvedPath = resolveOpenClawBinary();
        if (resolvedPath == null) {
            log.error("未找到 openclaw 可执行文件。请通过 ai.openclaw.binary-path 指定绝对路径，常见位置:");
            log.error("  Linux/macOS: /usr/local/bin/openclaw, /opt/openclaw/bin/openclaw, ~/.openclaw/bin/openclaw");
            log.error("  Windows:     C:\\\\openclaw\\\\openclaw.exe, %USERPROFILE%\\\\.openclaw\\\\bin\\\\openclaw.exe");
            return;
        }

        log.info("使用 openclaw 二进制: {}", resolvedPath);

        List<String> command = new ArrayList<>();
        command.add(resolvedPath);
        if (openClawGatewayArgs != null && !openClawGatewayArgs.trim().isEmpty()) {
            command.addAll(Arrays.asList(openClawGatewayArgs.trim().split("\\s+")));
        }

        File logFile = resolveLogFile();
        log.info("OpenClaw 日志文件: {}", logFile.getAbsolutePath());

        try {
            File parent = logFile.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                log.warn("无法创建日志目录: {}", parent.getAbsolutePath());
            }

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectOutput(logFile);
            pb.redirectErrorStream(true);
            if (openClawConfigPath != null && !openClawConfigPath.isEmpty()) {
                File cfg = new File(openClawConfigPath);
                if (cfg.exists()) {
                    pb.environment().put("OPENCLAW_CONFIG", openClawConfigPath);
                    log.info("使用配置文件: {}", openClawConfigPath);
                } else {
                    log.warn("配置文件不存在，忽略: {}", openClawConfigPath);
                }
            }
            Process p = pb.start();
            log.info("OpenClaw Gateway 启动命令已执行，PID: {}", p.pid());

            waitForReady(30);
        } catch (IOException e) {
            log.error("启动 OpenClaw Gateway 失败，命令={}, 原因={}", command, e.getMessage());
        }
    }

    private File resolveLogFile() {
        String configured = (openClawLogPath == null) ? "" : openClawLogPath.trim();
        if (!configured.isEmpty()) {
            return new File(configured);
        }
        String os = System.getProperty("os.name").toLowerCase();
        String tmpDir = System.getProperty("java.io.tmpdir");
        String name = os.contains("win") ? "openclaw_stderr.log" : "openclaw_stderr.log";
        return new File(tmpDir, name);
    }

    private String resolveOpenClawBinary() {
        if (openClawBinaryPath != null && !openClawBinaryPath.trim().isEmpty()) {
            String configured = openClawBinaryPath.trim();
            if (looksLikeLogFile(configured)) {
                log.error("ai.openclaw.binary-path 看起来是日志文件路径而不是可执行文件: {}", configured);
                log.error("请将 ai.openclaw.binary-path 指向 openclaw/openclaw.exe，例如 /usr/local/bin/openclaw");
                return null;
            }
            Path configuredPath = Paths.get(configured);
            if (Files.isExecutable(configuredPath) || isExecutableBinary(configuredPath)) {
                return configuredPath.toAbsolutePath().toString();
            }
            if (Files.exists(configuredPath) && Files.isDirectory(configuredPath)) {
                log.error("ai.openclaw.binary-path 指向的是目录而不是可执行文件: {}", configured);
            } else if (!Files.exists(configuredPath)) {
                log.error("ai.openclaw.binary-path 指定的文件不存在: {}", configured);
            } else {
                log.error("ai.openclaw.binary-path 指定的文件不可执行: {}", configured);
            }
            return null;
        }

        String os = System.getProperty("os.name").toLowerCase();
        String[] candidateDirs = os.contains("win")
                ? new String[]{
                    "C:\\openclaw\\bin",
                    "C:\\openclaw",
                    "C:\\Program Files\\openclaw\\bin",
                    System.getProperty("user.home") + "\\.openclaw\\bin",
                    System.getProperty("user.home") + "\\.openclaw"
                }
                : new String[]{
                    "/usr/local/bin",
                    "/usr/bin",
                    "/opt/openclaw/bin",
                    System.getProperty("user.home") + "/.openclaw/bin"
                };

        String[] candidateNames = os.contains("win")
                ? new String[]{"openclaw.exe", "openclaw.cmd", "openclaw.bat"}
                : new String[]{"openclaw"};

        for (String dir : candidateDirs) {
            for (String name : candidateNames) {
                Path p = Paths.get(dir, name);
                if (Files.isExecutable(p) || isExecutableBinary(p)) {
                    return p.toAbsolutePath().toString();
                }
            }
        }

        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            String separator = os.contains("win") ? ";" : ":";
            for (String dir : pathEnv.split(separator)) {
                if (dir == null || dir.isEmpty()) continue;
                for (String name : candidateNames) {
                    Path p = Paths.get(dir, name);
                    if (Files.isExecutable(p) || isExecutableBinary(p)) {
                        return p.toAbsolutePath().toString();
                    }
                }
            }
        }

        return null;
    }

    private boolean isExecutableBinary(Path path) {
        if (path == null || !Files.isRegularFile(path)) {
            return false;
        }
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".exe") || name.endsWith(".cmd") || name.endsWith(".bat") || name.endsWith(".sh");
    }

    private boolean looksLikeLogFile(String path) {
        if (path == null) return false;
        String lower = path.toLowerCase();
        return lower.endsWith(".log") || lower.endsWith(".txt") || lower.endsWith(".err");
    }

    private void waitForReady(int maxSeconds) {
        for (int i = 0; i < maxSeconds; i++) {
            if (checkOpenClaw()) {
                log.info("OpenClaw Gateway 自动启动成功 ({}s)", i);
                return;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        log.error("OpenClaw Gateway 自动启动超时，请检查日志: {}", resolveLogFile().getAbsolutePath());
    }
}
