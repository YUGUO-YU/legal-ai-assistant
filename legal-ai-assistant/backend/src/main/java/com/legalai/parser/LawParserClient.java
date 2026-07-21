package com.legalai.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

@Component
public class LawParserClient {
    private static final Logger log = LoggerFactory.getLogger(LawParserClient.class);
    private static final long RPC_TIMEOUT_SECONDS = 120;

    @Value("${law-parser.python-path:python3}")
    private String pythonPath;

    @Value("${law-parser.script-path:}")
    private String scriptPath;

    private Process process;
    private BufferedReader reader;
    private BufferedWriter writer;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentHashMap<Long, CompletableFuture<Map<String, Object>>> pending = new ConcurrentHashMap<>();
    private long nextId = 1;
    private ExecutorService senderExecutor = Executors.newSingleThreadExecutor();
    private volatile boolean started = false;

    @PostConstruct
    public void start() {
        try {
            String script = System.getProperty("law.parser.script",
                "/workspace/legal-ai-assistant/law_parser/src");

            ProcessBuilder pb = new ProcessBuilder();
            pb.command(pythonPath, "-m", "law_parser.main", "serve");
            pb.environment().put("PYTHONPATH", script);
            pb.redirectErrorStream(false);

            process = pb.start();
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

            senderExecutor.submit(this::readResponses);

            Map<String, Object> resp = callNoEx("health", Map.of());
            if (resp != null) {
                started = true;
                log.info("Law parser Python subprocess started and healthy");
            } else {
                log.warn("Law parser Python subprocess did not respond to health check");
            }
        } catch (IOException e) {
            log.warn("Failed to start law parser subprocess: {}. Falling back to Java parsing.", e.getMessage());
        }
    }

    @PreDestroy
    public void stop() {
        try {
            if (process != null && process.isAlive()) {
                try {
                    send("{\"jsonrpc\":\"2.0\",\"id\":-1,\"method\":\"shutdown\",\"params\":{}}");
                    process.waitFor(3, TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.warn("Error stopping law parser: {}", e.getMessage());
                }
            }
        } finally {
            senderExecutor.shutdownNow();
        }
    }

    private void readResponses() {
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                JsonNode node = objectMapper.readTree(line);
                long id = node.has("id") ? node.get("id").asLong() : 0;
                CompletableFuture<Map<String, Object>> future = pending.remove(id);
                if (future != null) {
                    if (node.has("error")) {
                        future.completeExceptionally(
                            new RuntimeException(node.get("error").get("message").asText()));
                    } else {
                        future.complete(objectMapper.convertValue(node.get("result"), Map.class));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error reading from Python process: {}", e.getMessage());
        }
    }

    public Map<String, Object> parse(String filePath) {
        return call("parse", Map.of("file_path", filePath));
    }

    public Map<String, Object> review(Map<String, Object> structureResult) {
        return call("review", Map.of("structure_result", structureResult));
    }

    public Map<String, Object> importLaw(Map<String, Object> lawData, boolean dryRun) {
        return call("import", Map.of("law_data", lawData, "dry_run", dryRun));
    }

    public boolean isAvailable() {
        return started && process != null && process.isAlive();
    }

    private Map<String, Object> callNoEx(String method, Map<String, Object> params) {
        try {
            return call(method, params);
        } catch (Exception e) {
            log.warn("Law parser call failed (non-fatal): {}: {}", method, e.getMessage());
            return null;
        }
    }

    private synchronized void send(String json) throws IOException {
        writer.write(json);
        writer.newLine();
        writer.flush();
    }

    private Map<String, Object> call(String method, Map<String, Object> params) {
        if (process == null || !process.isAlive()) {
            throw new IllegalStateException("Python parser process not running");
        }
        long id = nextId++;
        String request;
        try {
            request = String.format("{\"jsonrpc\":\"2.0\",\"id\":%d,\"method\":\"%s\",\"params\":%s}",
                id, method, objectMapper.writeValueAsString(params));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException("Law parser call failed: " + method, e);
        }
        try {
            send(request);
            CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
            pending.put(id, future);
            return future.get(RPC_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            pending.remove(id);
            throw new RuntimeException("Law parser call failed: " + method, e);
        }
    }
}
