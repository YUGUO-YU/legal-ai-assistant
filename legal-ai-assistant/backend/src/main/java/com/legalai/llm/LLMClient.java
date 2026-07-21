package com.legalai.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 直接调用 MiniMax-M3 (OpenAI 兼容 Chat Completions 协议) 的客户端。
 *
 * 配置项 (application.yml):
 *   ai.minimax.base-url       默认 https://api.minimax.chat/v1
 *   ai.minimax.api-key        必填
 *   ai.minimax.model          默认 MiniMax-M3
 *   ai.minimax.timeout        秒，默认 120
 *   ai.minimax.embedding-model 默认 embo-01
 */
@Component
public class LLMClient {

    private static final Logger log = LoggerFactory.getLogger(LLMClient.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Value("${ai.minimax.base-url:https://api.minimax.chat/v1}")
    private String baseUrl;

    @Value("${ai.minimax.api-key:}")
    private String apiKey;

    @Value("${ai.minimax.model:MiniMax-M3}")
    private String model;

    @Value("${ai.minimax.timeout:120}")
    private int timeout;

    @Value("${ai.minimax.embedding-model:embo-01}")
    private String embeddingModel;

    @Value("${ai.minimax.health-endpoint:/models}")
    private String healthEndpoint;

    @Value("${ai.minimax.health-timeout:5}")
    private int healthTimeout;

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private static final int MAX_RETRIES = 2;

    public LLMClient() {
        this.client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(timeout, TimeUnit.SECONDS)
            .writeTimeout(timeout, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();
        this.objectMapper = new ObjectMapper();
    }

    private OkHttpClient healthClient() {
        return client.newBuilder()
            .connectTimeout(healthTimeout, TimeUnit.SECONDS)
            .readTimeout(healthTimeout, TimeUnit.SECONDS)
            .writeTimeout(healthTimeout, TimeUnit.SECONDS)
            .build();
    }

    public String getModel() {
        return resolveModel();
    }

    public String getBaseUrl() {
        return resolveBaseUrl();
    }

    public String getApiKey() {
        return resolveApiKey();
    }

    private Map<String, Object> getActiveModelConfig() {
        try {
            var adminDataService = applicationContext.getBean("adminDataService",
                com.legalai.admin.service.AdminDataService.class);
            return adminDataService.getActiveModelConfigFromDb();
        } catch (Exception e) {
            log.debug("从数据库获取活跃模型配置失败: {}", e.getMessage());
            return null;
        }
    }

    private String resolveModel() {
        var cfg = getActiveModelConfig();
        if (cfg != null && cfg.get("model_code") != null) {
            return cfg.get("model_code").toString();
        }
        return model;
    }

    private String resolveBaseUrl() {
        var cfg = getActiveModelConfig();
        if (cfg != null && cfg.get("endpoint") != null) {
            String ep = cfg.get("endpoint").toString();
            if (!ep.endsWith("/")) ep += "/";
            return ep;
        }
        return baseUrl;
    }

    private String resolveApiKey() {
        var cfg = getActiveModelConfig();
        if (cfg != null && cfg.get("api_key_enc") != null) {
            String key = cfg.get("api_key_enc").toString();
            if (!key.isEmpty()) {
                return key;
            }
        }
        return apiKey;
    }

    public Map<String, Object> getResolvedConfig() {
        Map<String, Object> cfg = new LinkedHashMap<>();
        cfg.put("model", resolveModel());
        cfg.put("baseUrl", resolveBaseUrl());
        cfg.put("apiKey", resolveApiKey());
        cfg.put("embeddingModel", embeddingModel);
        cfg.put("timeout", timeout);
        var dbCfg = getActiveModelConfig();
        if (dbCfg != null) {
            cfg.put("isFromDb", true);
            cfg.put("modelName", dbCfg.get("model_name"));
            cfg.put("provider", dbCfg.get("provider"));
            cfg.put("temperature", dbCfg.get("temperature"));
            cfg.put("maxTokens", dbCfg.get("max_tokens"));
            cfg.put("topP", dbCfg.get("top_p"));
        } else {
            cfg.put("isFromDb", false);
        }
        return cfg;
    }

    /**
     * 单轮对话
     */
    public String chat(String prompt) throws IOException {
        String resolvedModel = resolveModel();
        log.info("调用 MiniMax: model={}, prompt长度={}", resolvedModel, prompt == null ? 0 : prompt.length());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", resolvedModel);
        requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt == null ? "" : prompt)));
        requestBody.put("stream", false);

        return executeChatRequest(requestBody);
    }

    /**
     * 多轮对话
     */
    public String chatWithMessages(List<Map<String, String>> messages) throws IOException {
        String resolvedModel = resolveModel();
        log.info("调用 MiniMax(多轮): 消息数量={}", messages == null ? 0 : messages.size());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", resolvedModel);
        requestBody.put("messages", messages);
        requestBody.put("stream", false);

        return executeChatRequest(requestBody);
    }

    /**
     * 带工具调用的对话
     */
    public String chatWithTools(String prompt, List<Map<String, Object>> tools) throws IOException {
        String resolvedModel = resolveModel();
        log.info("调用 MiniMax(带工具): model={}, prompt长度={}, tools数量={}", resolvedModel,
            prompt == null ? 0 : prompt.length(),
            tools != null ? tools.size() : 0);

        List<Map<String, String>> messages = List.of(Map.of("role", "user", "content", prompt == null ? "" : prompt));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", resolvedModel);
        requestBody.put("messages", messages);
        requestBody.put("stream", false);
        if (tools != null && !tools.isEmpty()) {
            requestBody.put("tools", tools);
        }

        return executeChatRequest(requestBody);
    }

    /**
     * 带联网搜索的对话（MiniMax web_search 工具）。
     * 返回 AI 整理后的文本，搜索结果会合并在响应中。
     */
    public String searchWeb(String prompt) throws IOException {
        log.info("调用 MiniMax(联网搜索): model={}, prompt长度={}", model, prompt == null ? 0 : prompt.length());

        List<Map<String, Object>> tools = List.of(Map.of(
            "type", "function",
            "function", Map.of(
                "name", "web_search",
                "description", "联网搜索功能，可以搜索互联网上的最新信息",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "search_query", Map.of("type", "string", "description", "搜索查询内容"),
                        "search_mode", Map.of("type", "string", "description", "搜索模式"),
                        "enable_brief_search_result", Map.of("type", "boolean", "description", "是否启用简短结果")
                    ),
                    "required", List.of("search_query")
                )
            )
        ));

        String argumentsJson = new ObjectMapper().writeValueAsString(Map.of(
            "search_query", prompt != null ? prompt : "",
            "search_mode", "highlight",
            "enable_brief_search_result", true
        ));

        List<Map<String, Object>> toolCalls = List.of(Map.of(
            "id", "call_1",
            "type", "function",
            "function", Map.of(
                "name", "web_search",
                "arguments", argumentsJson
            )
        ));

        List<Map<String, String>> messages = List.of(
            Map.of("role", "user", "content", prompt != null ? prompt : "")
        );

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("stream", false);
        requestBody.put("tools", tools);
        requestBody.put("tool_calls", toolCalls);

        return executeSearchWebRequest(requestBody);
    }

    private String executeSearchWebRequest(Map<String, Object> requestBody) throws IOException {
        String resolvedModel = resolveModel();
        String resolvedBaseUrl = resolveBaseUrl();
        String resolvedApiKey = resolveApiKey();

        String json = objectMapper.writeValueAsString(requestBody);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

        Request request = new Request.Builder()
            .url(resolvedBaseUrl + "chat/completions")
            .addHeader("Authorization", "Bearer " + resolvedApiKey)
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "no body";
                log.error("MiniMax 联网搜索API调用失败: code={}, message={}, body={}", response.code(), response.message(), errorBody);
                throw new IOException("API调用失败: " + response.code() + " " + errorBody);
            }

            String responseBody = response.body().string();
            log.info("MiniMax 联网搜索响应长度: {}", responseBody.length());

            return parseSearchWebResponse(responseBody);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            log.error("MiniMax 联网搜索异常: {}", e.getMessage(), e);
            throw new IOException("联网搜索异常: " + e.getMessage(), e);
        }
    }

    private String parseSearchWebResponse(String responseBody) {
        try {
            JsonNode json = objectMapper.readTree(responseBody);
            JsonNode choices = json.get("choices");
            if (choices == null || !choices.isArray() || choices.size() == 0) {
                log.warn("MiniMax 联网搜索响应格式异常: choices={}", choices);
                return "{\"error\": \"响应格式异常\"}";
            }

            JsonNode message = choices.get(0).get("message");

            if (message.has("content") && !message.get("content").isNull()) {
                String content = message.get("content").asText();
                if (content != null && !content.trim().isEmpty()) {
                    log.info("MiniMax 联网搜索解析成功(content): content长度={}", content.length());
                    return content;
                }
            }

            JsonNode toolCalls = message.get("tool_calls");
            if (toolCalls != null && toolCalls.isArray() && toolCalls.size() > 0) {
                JsonNode firstCall = toolCalls.get(0);
                JsonNode function = firstCall.get("function");
                if (function != null) {
                    String fnName = function.has("name") ? function.get("name").asText() : "";
                    String args = function.has("arguments") ? function.get("arguments").asText() : "{}";
                    log.info("MiniMax 联网搜索解析成功(tool_call): fn={}, args前100字符={}", fnName, args.substring(0, Math.min(100, args.length())));

                    try {
                        JsonNode argsNode = objectMapper.readTree(args);
                        if (argsNode.has("search_result") || argsNode.has("results") || argsNode.has("data")) {
                            String searchResults = argsNode.has("search_result") ? argsNode.get("search_result").asText()
                                : argsNode.has("results") ? argsNode.get("results").asText()
                                : argsNode.get("data").asText();
                            return searchResults;
                        }
                        if (argsNode.has("content")) {
                            return argsNode.get("content").asText();
                        }
                    } catch (Exception e) {
                        log.debug("解析tool_call arguments失败，返回原始文本: {}", e.getMessage());
                    }

                    return args;
                }
            }

            log.warn("MiniMax 联网搜索响应content和tool_calls均为空: responseBody前300字符={}",
                    responseBody.substring(0, Math.min(300, responseBody.length())));
            return "{\"error\": \"未获取到搜索结果\"}";
        } catch (Exception e) {
            log.error("解析 MiniMax 联网搜索响应失败: {}", e.getMessage(), e);
            return "{\"error\": \"解析搜索响应失败: " + e.getMessage() + "\"}";
        }
    }

    /**
     * 联网搜索 + 结构化输出：先用 web_search 获取信息，再调用一次让 AI 整理为结构化 JSON。
     * 失败时自动降级到 DuckDuckGo 直连搜索。
     */
    public String searchAndStructure(String searchPrompt, String structurePrompt) throws IOException {
        log.info("调用 MiniMax(搜索+结构化): 两步流程");

        String searchResult = null;
        try {
            searchResult = searchWeb(searchPrompt);
            log.info("MiniMax联网搜索完成，结果长度={}", searchResult != null ? searchResult.length() : 0);
        } catch (IOException e) {
            log.warn("MiniMax联网搜索失败，降级到DuckDuckGo直连: {}", e.getMessage());
            searchResult = directWebSearch(searchPrompt);
            log.info("DuckDuckGo直连搜索完成，结果长度={}", searchResult != null ? searchResult.length() : 0);
        }

        List<Map<String, String>> messages = List.of(
            Map.of("role", "system", "content", searchPrompt),
            Map.of("role", "assistant", "content", searchResult != null ? searchResult : ""),
            Map.of("role", "user", "content", structurePrompt)
        );

        return chatWithMessages(messages);
    }

    /**
     * DuckDuckGo 直连搜索（DuckDuckGo HTML 页面抓取）。
     * 当 MiniMax 联网搜索失败时降级使用。
     */
    public String directWebSearch(String query) throws IOException {
        String encodedQuery = java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);

        // 策略1: DuckDuckGo 即时答案 API
        String result = ddgInstantAnswer(encodedQuery);
        if (result != null && !result.isEmpty()) {
            return result;
        }

        // 策略2: DuckDuckGo HTML 抓取（更全面）
        result = ddgHtmlSearch(encodedQuery);
        if (result != null && !result.isEmpty()) {
            return result;
        }

        return "未获取到搜索结果";
    }

    private String ddgInstantAnswer(String encodedQuery) throws IOException {
        String url = "https://api.duckduckgo.com/?q=" + encodedQuery + "&format=json&no_html=1&skip_disambig=1&hl=zh-cn";
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (compatible; LegalAI/1.0)")
                .get()
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.warn("DuckDuckGo即时搜索失败: code={}", response.code());
                return null;
            }
            String body = response.body() != null ? response.body().string() : "";
            if (body.isEmpty()) return null;
            JsonNode node = objectMapper.readTree(body);
            StringBuilder sb = new StringBuilder();
            if (node.has("AbstractText") && !node.get("AbstractText").isNull()) {
                sb.append(node.get("AbstractText").asText());
            }
            if (node.has("RelatedTopics")) {
                for (JsonNode topic : node.get("RelatedTopics")) {
                    if (topic.has("Text")) {
                        sb.append("\n").append(topic.get("Text").asText());
                    }
                    if (sb.length() > 3000) break;
                }
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("DuckDuckGo即时搜索异常: {}", e.getMessage());
            return null;
        }
    }

    private String ddgHtmlSearch(String encodedQuery) throws IOException {
        String url = "https://html.duckduckgo.com/html/?q=" + encodedQuery + "&kl=zh-cn";
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (compatible; LegalAI/1.0)")
                .get()
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.warn("DuckDuckGo HTML搜索失败: code={}", response.code());
                return null;
            }
            String html = response.body() != null ? response.body().string() : "";
            if (html.isEmpty()) return null;
            return parseDdgHtml(html);
        } catch (Exception e) {
            log.warn("DuckDuckGo HTML搜索异常: {}", e.getMessage());
            return null;
        }
    }

    private String parseDdgHtml(String html) {
        StringBuilder sb = new StringBuilder();
        java.util.regex.Pattern linkPattern = java.util.regex.Pattern.compile(
                "<a class=\"result__a\"[^>]*href=\"([^\"]*)\"[^>]*>(.*?)</a>",
                java.util.regex.Pattern.DOTALL);
        java.util.regex.Pattern snippetPattern = java.util.regex.Pattern.compile(
                "<a class=\"result__snippet\"[^>]*>(.*?)</a>",
                java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher linkMatcher = linkPattern.matcher(html);
        java.util.regex.Matcher snippetMatcher = snippetPattern.matcher(html);
        java.util.List<String> links = new java.util.ArrayList<>();
        java.util.List<String> titles = new java.util.ArrayList<>();
        while (linkMatcher.find()) {
            String href = linkMatcher.group(1);
            String title = linkMatcher.group(2).replaceAll("<[^>]+>", "").trim();
            if (!title.isEmpty()) {
                links.add(href);
                titles.add(title);
            }
            if (links.size() >= 10) break;
        }
        int snippetCount = 0;
        while (snippetMatcher.find()) {
            String snippet = snippetMatcher.group(1).replaceAll("<[^>]+>", "").trim();
            if (snippetCount < links.size()) {
                sb.append("【").append(titles.get(snippetCount)).append("】")
                  .append(snippet).append("\n来源: ").append(links.get(snippetCount)).append("\n\n");
            }
            snippetCount++;
            if (sb.length() > 4000) break;
        }
        log.info("DuckDuckGo HTML解析提取 {} 条结果", snippetCount);
        return sb.toString();
    }

    /**
     * 流式对话
     */
    public String chatStream(String prompt, Consumer<String> onChunk, Supplier<Boolean> isCancelled) throws IOException {
        String resolvedModel = resolveModel();
        String resolvedBaseUrl = resolveBaseUrl();
        String resolvedApiKey = resolveApiKey();
        log.info("调用 MiniMax(流式): model={}, prompt长度={}", resolvedModel, prompt == null ? 0 : prompt.length());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", resolvedModel);
        requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt == null ? "" : prompt)));
        requestBody.put("stream", true);

        String json = objectMapper.writeValueAsString(requestBody);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

        Request request = new Request.Builder()
            .url(resolvedBaseUrl + "chat/completions")
            .addHeader("Authorization", "Bearer " + resolvedApiKey)
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "no body";
                log.error("MiniMax 流式调用失败: code={}, message={}, body={}", response.code(), response.message(), errorBody);
                throw new IOException("流式API调用失败: " + response.code() + " " + errorBody);
            }

            if (response.body() == null) {
                throw new IOException("响应体为空");
            }

            StringBuilder fullResponse = new StringBuilder();
            String responseBody = response.body().string();

            String[] lines = responseBody.split("\n");
            for (String line : lines) {
                if (isCancelled != null && Boolean.TRUE.equals(isCancelled.get())) {
                    log.info("流式响应被取消");
                    break;
                }
                if (line.startsWith("data: ")) {
                    String data = line.substring(6);
                    if ("[DONE]".equals(data)) {
                        break;
                    }
                    try {
                        JsonNode jsonNode = objectMapper.readTree(data);
                        JsonNode choices = jsonNode.get("choices");
                        if (choices != null && choices.isArray() && choices.size() > 0) {
                            JsonNode delta = choices.get(0).get("delta");
                            if (delta != null && delta.has("content")) {
                                String content = delta.get("content").asText();
                                fullResponse.append(content);
                                if (onChunk != null) {
                                    onChunk.accept(content);
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.debug("解析流式响应行失败: {}", line);
                    }
                }
            }

            log.info("流式响应完成: 总长度={}", fullResponse.length());
            return fullResponse.toString();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            log.error("MiniMax 流式API异常: {}", e.getMessage(), e);
            throw new IOException("流式API异常: " + e.getMessage(), e);
        }
    }

    public float[] embedText(String text) throws IOException {
        log.info("调用 MiniMax Embedding: text长度={}", text == null ? 0 : text.length());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", embeddingModel);
        requestBody.put("input", text == null ? "" : text);

        String json = objectMapper.writeValueAsString(requestBody);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

        Request request = new Request.Builder()
            .url(baseUrl + "/embeddings")
            .addHeader("Authorization", "Bearer " + apiKey)
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "no body";
                log.error("MiniMax Embedding 失败: code={}, message={}, body={}", response.code(), response.message(), errorBody);
                throw new IOException("Embedding API调用失败: " + response.code());
            }

            String responseBody = response.body().string();
            return parseEmbeddingResponse(responseBody);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            log.error("MiniMax Embedding 异常: {}", e.getMessage(), e);
            throw new IOException("Embedding API异常: " + e.getMessage(), e);
        }
    }

    /**
     * 探测模型是否可访问（用于健康检查）。
     * 严格判定：只有 HTTP 2xx 才算在线；401/403/网络异常/超时都算离线。
     */
    public boolean ping() {
        String resolvedApiKey = resolveApiKey();
        String resolvedBaseUrl = resolveBaseUrl();
        if (resolvedApiKey == null || resolvedApiKey.isEmpty()) {
            log.warn("LLM 健康检查失败: api-key 未配置");
            return false;
        }
        try {
            Request request = new Request.Builder()
                .url(resolvedBaseUrl + healthEndpoint)
                .addHeader("Authorization", "Bearer " + resolvedApiKey)
                .get()
                .build();
            try (Response response = healthClient().newCall(request).execute()) {
                boolean ok = response.isSuccessful();
                if (!ok) {
                    log.warn("LLM 健康检查失败: code={}, message={}",
                        response.code(), response.message());
                }
                return ok;
            }
        } catch (Exception e) {
            log.warn("LLM 健康检查失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 探测第一个可用模型名（用于 ai-status 展示）
     */
    public String firstModel() {
        String resolvedApiKey = resolveApiKey();
        String resolvedBaseUrl = resolveBaseUrl();
        String resolvedModel = resolveModel();
        if (resolvedApiKey == null || resolvedApiKey.isEmpty()) {
            return resolvedModel;
        }
        try {
            Request request = new Request.Builder()
                .url(resolvedBaseUrl + healthEndpoint)
                .addHeader("Authorization", "Bearer " + resolvedApiKey)
                .get()
                .build();
            try (Response response = healthClient().newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    return resolvedModel;
                }
                JsonNode json = objectMapper.readTree(response.body().string());
                JsonNode data = json.get("data");
                if (data != null && data.isArray() && data.size() > 0) {
                    JsonNode first = data.get(0);
                    if (first.has("id")) {
                        return first.get("id").asText();
                    }
                }
            }
        } catch (Exception e) {
            log.debug("获取模型列表失败: {}", e.getMessage());
        }
        return resolvedModel;
    }

    /**
     * 健康检查并返回延迟信息
     */
    public Map<String, Object> healthCheck() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("model", resolveModel());
        result.put("baseUrl", resolveBaseUrl());
        long start = System.currentTimeMillis();
        boolean healthy = ping();
        long latency = System.currentTimeMillis() - start;
        result.put("healthy", healthy);
        result.put("latencyMs", latency);
        result.put("apiKeyConfigured", resolveApiKey() != null && !resolveApiKey().isEmpty());
        return result;
    }

    public boolean isApiKeyConfigured() {
        return resolveApiKey() != null && !resolveApiKey().isEmpty();
    }

    private String executeChatRequest(Map<String, Object> requestBody) throws IOException {
        String resolvedModel = resolveModel();
        String resolvedBaseUrl = resolveBaseUrl();
        String resolvedApiKey = resolveApiKey();

        String json = objectMapper.writeValueAsString(requestBody);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

        IOException lastEx = null;
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            if (attempt > 0) {
                log.info("MiniMax API 重试第 {} 次", attempt);
                try { Thread.sleep(500L * attempt); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }

            Request request = new Request.Builder()
                .url(resolvedBaseUrl + "chat/completions")
                .addHeader("Authorization", "Bearer " + resolvedApiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

            try (Response response = client.newCall(request).execute()) {
                int code = response.code();
                if (code >= 500 && code < 600 && attempt < MAX_RETRIES) {
                    String errorBody = response.body() != null ? response.body().string() : "no body";
                    log.warn("MiniMax API 服务端错误: code={}, body前200={}, 重试", code, errorBody.substring(0, Math.min(200, errorBody.length())));
                    continue;
                }
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "no body";
                    log.error("MiniMax API调用失败: code={}, message={}, body={}", code, response.message(), errorBody);
                    throw new IOException("API调用失败: " + code + " " + errorBody);
                }

                String responseBody = response.body().string();
                log.info("MiniMax API响应长度: {}", responseBody.length());
                return parseResponse(responseBody);
            } catch (IOException e) {
                lastEx = e;
                if (attempt < MAX_RETRIES) {
                    log.warn("MiniMax API IO异常: {}, 重试", e.getMessage());
                }
            } catch (Exception e) {
                log.error("MiniMax API异常: {}", e.getMessage(), e);
                throw new IOException("API调用异常: " + e.getMessage(), e);
            }
        }
        throw lastEx != null ? lastEx : new IOException("API调用未知异常");
    }

    private float[] parseEmbeddingResponse(String responseBody) {
        try {
            JsonNode json = objectMapper.readTree(responseBody);
            JsonNode embedding = json.get("data").get(0).get("embedding");
            float[] result = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                result[i] = (float) embedding.get(i).asDouble();
            }
            log.info("Embedding解析成功: 向量维度={}", result.length);
            return result;
        } catch (Exception e) {
            log.error("解析Embedding响应失败: {}", e.getMessage(), e);
            return new float[1536];
        }
    }

    private String parseResponse(String responseBody) {
        try {
            JsonNode json = objectMapper.readTree(responseBody);
            JsonNode choices = json.get("choices");
            if (choices == null || !choices.isArray() || choices.size() == 0) {
                log.warn("MiniMax 响应格式异常: choices={}", choices);
                return "抱歉，AI服务暂时无法响应，请稍后重试。";
            }

            JsonNode message = choices.get(0).get("message");
            String content = null;

            if (message.has("content") && !message.get("content").isNull()) {
                content = message.get("content").asText();
            }

            if (content != null && !content.trim().isEmpty()) {
                log.info("MiniMax 解析成功: content长度={}", content.length());
                return content;
            }

            JsonNode finishReason = choices.get(0).has("finish_reason") ? choices.get(0).get("finish_reason") : null;
            if (finishReason != null && "tool_calls".equals(finishReason.asText())) {
                JsonNode toolCalls = message.get("tool_calls");
                if (toolCalls != null && toolCalls.isArray() && toolCalls.size() > 0) {
                    JsonNode firstCall = toolCalls.get(0);
                    JsonNode function = firstCall.get("function");
                    if (function != null) {
                        String fnName = function.has("name") ? function.get("name").asText() : "";
                        String args = function.has("arguments") ? function.get("arguments").asText() : "{}";
                        log.info("MiniMax tool_calls 响应: fn={}, args长度={}", fnName, args.length());
                        return "联网搜索工具(" + fnName + ")已调用，参数: " + args + "。搜索结果已由AI整理。";
                    }
                }
            }

            log.warn("MiniMax 响应content为空: finish_reason={}, responseBody前200字符={}",
                    finishReason, responseBody.substring(0, Math.min(200, responseBody.length())));
            return "抱歉，AI服务暂时无法响应，请稍后重试。";
        } catch (Exception e) {
            log.error("解析 MiniMax 响应失败: {}", e.getMessage(), e);
            return "抱歉，AI服务暂时无法响应，请稍后重试。";
        }
    }
}
