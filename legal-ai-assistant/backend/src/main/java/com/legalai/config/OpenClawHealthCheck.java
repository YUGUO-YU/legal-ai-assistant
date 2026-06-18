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
import java.util.concurrent.atomic.AtomicReference;

@Component
public class OpenClawHealthCheck implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(OpenClawHealthCheck.class);

    public enum State {
        NOT_STARTED,
        STARTING,
        RUNNING,
        FAILED
    }

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

    private final AtomicReference<State> state = new AtomicReference<>(State.NOT_STARTED);
    private final AtomicReference<String> lastError = new AtomicReference<>(null);
    private final AtomicReference<Process> currentProcess = new AtomicReference<>(null);
    private final AtomicReference<String> currentBinary = new AtomicReference<>(null);
    private final AtomicReference<String> currentLogFile = new AtomicReference<>(null);

    public State getState() {
        return state.get();
    }

    public String getLastError() {
        return lastError.get();
    }

    public String getCurrentBinary() {
        return currentBinary.get();
    }

    public String getCurrentLogFile() {
        return currentLogFile.get();
    }

    public void setState(State newState, String reason) {
        State previous = state.getAndSet(newState);
        if (reason == null) {
            lastError.set(null);
        } else {
            lastError.set(reason);
        }
        log.info("OpenClaw 状态变更: {} -> {} {}", previous, newState, reason == null ? "" : "(" + reason + ")");
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("检查 OpenClaw Gateway 连接状态: {}", openClawUrl);

        if (checkOpenClaw()) {
            setState(State.RUNNING, null);
            log.info("OpenClaw Gateway 已就绪，跳过自动启动");
            return;
        }

        if (!autoStartOpenClaw) {
            setState(State.NOT_STARTED, "未配置自动启动 (ai.openclaw.auto-start=false)");
            log.warn("OpenClaw Gateway 未就绪且已禁用自动启动，请手动启动或设置 ai.openclaw.auto-start=true");
            return;
        }

        setState(State.STARTING, "应用启动时自动启动");
        boolean ok = startOpenClaw();
        if (ok) {
            setState(State.RUNNING, null);
        } else {
            setState(State.FAILED, lastError.get() != null ? lastError.get() : "未知原因");
        }
    }

    public synchronized boolean restart() {
        log.info("收到 OpenClaw 重启请求");
        stopCurrentProcess();
        setState(State.STARTING, "用户触发重启");
        boolean ok = startOpenClaw();
        if (ok) {
            setState(State.RUNNING, null);
        } else {
            setState(State.FAILED, lastError.get() != null ? lastError.get() : "未知原因");
        }
        return ok;
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

    private boolean startOpenClaw() {
        String resolvedPath = resolveOpenClawBinary();
        if (resolvedPath == null) {
            String msg = "未找到 openclaw 可执行文件，请检查 ai.openclaw.binary-path 配置";
            log.error(msg);
            lastError.set(msg);
            return false;
        }

        log.info("使用 openclaw 二进制: {}", resolvedPath);
        currentBinary.set(resolvedPath);

        List<String> command = new ArrayList<>();
        command.add(resolvedPath);
        if (openClawGatewayArgs != null && !openClawGatewayArgs.trim().isEmpty()) {
            command.addAll(Arrays.asList(openClawGatewayArgs.trim().split("\\s+")));
        }

        File logFile = resolveLogFile();
        currentLogFile.set(logFile.getAbsolutePath());
        log.info("OpenClaw 日志文件: {}", logFile.getAbsolutePath());

        try {
            File parent = logFile.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                String warn = "无法创建日志目录: " + parent.getAbsolutePath();
                log.warn(warn);
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
            currentProcess.set(p);
            log.info("OpenClaw Gateway 启动命令已执行，PID: {}", p.pid());

            boolean ready = waitForReady(30);
            if (!ready) {
                String msg = "OpenClaw Gateway 启动超时 (" + 30 + "s)，请检查日志: " + logFile.getAbsolutePath();
                log.error(msg);
                lastError.set(msg);
                stopCurrentProcess();
                return false;
            }
            lastError.set(null);
            return true;
        } catch (IOException e) {
            String msg = "启动 OpenClaw Gateway 失败: " + e.getMessage();
            log.error(msg);
            lastError.set(msg);
            return false;
        }
    }

    private void stopCurrentProcess() {
        Process p = currentProcess.getAndSet(null);
        if (p != null && p.isAlive()) {
            log.info("停止 OpenClaw Gateway 进程 PID={}", p.pid());
            try {
                p.destroy();
                if (!p.waitFor(5, TimeUnit.SECONDS)) {
                    p.destroyForcibly();
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                p.destroyForcibly();
            }
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
                String msg = "ai.openclaw.binary-path 看起来是日志文件路径而不是可执行文件: " + configured;
                log.error(msg);
                log.error("请将 ai.openclaw.binary-path 指向 openclaw/openclaw.exe，例如 E:\\legal-ai-assistant\\legal-ai-assistant\\openclaw\\openclaw.exe");
                lastError.set(msg);
                return null;
            }
            Path configuredPath = Paths.get(configured);
            if (Files.isExecutable(configuredPath) || isExecutableBinary(configuredPath)) {
                return configuredPath.toAbsolutePath().toString();
            }
            String reason;
            if (Files.exists(configuredPath) && Files.isDirectory(configuredPath)) {
                reason = "ai.openclaw.binary-path 指向的是目录而不是可执行文件: " + configured;
            } else if (!Files.exists(configuredPath)) {
                reason = "ai.openclaw.binary-path 指定的文件不存在: " + configured;
            } else {
                reason = "ai.openclaw.binary-path 指定的文件不可执行: " + configured;
            }
            log.error(reason);
            lastError.set(reason);
            return null;
        }

        String os = System.getProperty("os.name").toLowerCase();
        String cwd = System.getProperty("user.dir");
        String[] candidateDirs = os.contains("win")
                ? new String[]{
                    cwd + "\\openclaw",
                    cwd + "\\..\\openclaw",
                    "C:\\openclaw\\bin",
                    "C:\\openclaw",
                    "C:\\Program Files\\openclaw\\bin",
                    System.getProperty("user.home") + "\\.openclaw\\bin",
                    System.getProperty("user.home") + "\\.openclaw"
                }
                : new String[]{
                    cwd + "/openclaw",
                    cwd + "/../openclaw",
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

        String msg = "在常见目录与 PATH 中都未找到 openclaw 可执行文件";
        lastError.set(msg);
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

    private boolean waitForReady(int maxSeconds) {
        for (int i = 0; i < maxSeconds; i++) {
            if (checkOpenClaw()) {
                log.info("OpenClaw Gateway 自动启动成功 ({}s)", i);
                return true;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }
}
