package com.legalai.service.scraper;

import com.legalai.model.ScrapResult;
import com.legalai.utils.HtmlParser;
import com.legalai.utils.RateLimiter;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public abstract class BaseScraper implements EnterpriseScraper {
    private static final Logger log = LoggerFactory.getLogger(BaseScraper.class);

    protected static final String[] USER_AGENTS = {
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Safari/605.1.15"
    };

    protected static final MediaType TEXT_PLAIN = MediaType.parse("text/plain; charset=utf-8");

    protected final OkHttpClient httpClient;
    protected final Random random = new Random();
    private volatile long lastRequestTime = 0;
    private volatile long cooldownUntil = 0;

    protected static final RateLimiter rateLimiter = new RateLimiter(20, 60_000);

    protected BaseScraper() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public boolean isAvailable() {
        return cooldownUntil < System.currentTimeMillis();
    }

    @Override
    public long getCooldownMs() {
        return Math.max(0, cooldownUntil - System.currentTimeMillis());
    }

    protected void markCooldown() {
        cooldownUntil = System.currentTimeMillis() + getCooldownMs();
    }

    protected void throttle() throws InterruptedException {
        long elapsed = System.currentTimeMillis() - lastRequestTime;
        long delay = 300 + random.nextInt(500);
        if (elapsed < delay) {
            Thread.sleep(delay - elapsed);
        }
        lastRequestTime = System.currentTimeMillis();
    }

    protected String encodeQuery(String query) {
        return URLEncoder.encode(query, StandardCharsets.UTF_8);
    }

    protected String userAgent() {
        return USER_AGENTS[random.nextInt(USER_AGENTS.length)];
    }

    protected String doGet(String url) throws IOException {
        if (!rateLimiter.tryAcquire(getPlatformName())) {
            log.warn("[{}] Rate limit hit, skipping", getPlatformName());
            return null;
        }
        try {
            throttle();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", userAgent())
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .header("Accept-Encoding", "gzip, deflate, br")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.code() == 403 || response.code() == 429) {
                log.warn("[{}] Blocked with status {}", getPlatformName(), response.code());
                markCooldown();
                return null;
            }
            if (!response.isSuccessful()) {
                log.warn("[{}] HTTP {} for URL: {}", getPlatformName(), response.code(), url);
                return null;
            }
            ResponseBody body = response.body();
            return body != null ? body.string() : "";
        }
    }

    protected ScrapResult buildResult(String rawContent) {
        ScrapResult result = new ScrapResult(getPlatformName(), true);
        result.setRawContent(rawContent);

        if (rawContent == null || rawContent.isEmpty()) {
            result.setSuccess(false);
            return result;
        }

        String text = HtmlParser.stripTags(rawContent);
        extractFields(text, result);

        if (result.getFields().isEmpty()) {
            String snippets = HtmlParser.extractSearchSnippets(rawContent);
            if (!snippets.isEmpty()) {
                ScrapResult.ExtractedField field = new ScrapResult.ExtractedField(
                        "searchSnippets", snippets, 0.5, getPlatformName());
                result.addField("searchSnippets", field);
            }
        }

        return result;
    }

    protected void extractFields(String text, ScrapResult result) {
        String uscc = HtmlParser.extractUscc(text);
        if (uscc != null) {
            result.addField("uscc", new ScrapResult.ExtractedField("uscc", uscc, 0.9, getPlatformName()));
        }

        String legalRep = HtmlParser.extractLegalRepresentative(text);
        if (legalRep != null) {
            result.addField("legalRepresentative", new ScrapResult.ExtractedField("legalRepresentative", legalRep, 0.8, getPlatformName()));
        }

        String capital = HtmlParser.extractRegisteredCapital(text);
        if (capital != null) {
            result.addField("registeredCapital", new ScrapResult.ExtractedField("registeredCapital", capital, 0.8, getPlatformName()));
        }

        String date = HtmlParser.extractEstablishDate(text);
        if (date != null) {
            result.addField("establishDate", new ScrapResult.ExtractedField("establishDate", date, 0.8, getPlatformName()));
        }

        String status = HtmlParser.extractBusinessStatus(text);
        if (status != null) {
            result.addField("businessStatus", new ScrapResult.ExtractedField("businessStatus", status, 0.7, getPlatformName()));
        }

        String regAuth = HtmlParser.extractRegistrationAuthority(text);
        if (regAuth != null) {
            result.addField("registrationAuthority", new ScrapResult.ExtractedField("registrationAuthority", regAuth, 0.7, getPlatformName()));
        }
    }

    protected ScrapResult failResult(String errorCode, String message) {
        ScrapResult result = new ScrapResult(getPlatformName(), false);
        result.setErrorCode(errorCode);
        result.setRawContent(message);
        return result;
    }
}
