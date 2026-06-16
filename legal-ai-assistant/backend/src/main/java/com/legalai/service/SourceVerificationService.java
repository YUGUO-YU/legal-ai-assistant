package com.legalai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class SourceVerificationService {

    private static final Logger log = LoggerFactory.getLogger(SourceVerificationService.class);

    private static final Set<String> ALLOWED_DOMAINS = new HashSet<>(Arrays.asList(
        "flk.npc.gov.cn",
        "npc.gov.cn",
        "court.gov.cn",
        "wenshu.court.gov.cn",
        "pkulaw.cn",
        "chinalaw.gov.cn",
        "lawinfo.com.cn"
    ));

    private static final Set<String> SENSITIVE_KEYWORDS = new HashSet<>(Arrays.asList(
        "色情", "赌博", "毒品", "枪支", "暴力", "恐怖",
        "分裂", "颠覆", "邪教", "贪污", "贿赂"
    ));

    private static final Pattern URL_PATTERN = Pattern.compile(
        "^(https?://)?([\\da-z.-]+)\\.([a-z.]{2,6})([/\\w .-]*)*/?$"
    );

    public boolean isAllowedSource(String sourceUrl) {
        if (sourceUrl == null || sourceUrl.isEmpty()) {
            log.warn("来源URL为空");
            return false;
        }

        try {
            String domain = extractDomain(sourceUrl);
            if (domain == null) {
                log.warn("无法从URL提取域名: {}", sourceUrl);
                return false;
            }

            boolean allowed = ALLOWED_DOMAINS.stream()
                .anyMatch(allowedDomain -> domain.endsWith(allowedDomain) || domain.equals(allowedDomain));

            if (!allowed) {
                log.warn("来源URL不在白名单中: {}", sourceUrl);
            }

            return allowed;
        } catch (Exception e) {
            log.error("验证来源URL失败: {}", e.getMessage());
            return false;
        }
    }

    public String extractDomain(String url) {
        if (url == null) return null;

        url = url.toLowerCase();
        if (url.startsWith("http://")) {
            url = url.substring(7);
        } else if (url.startsWith("https://")) {
            url = url.substring(8);
        }

        int slashIndex = url.indexOf('/');
        if (slashIndex > 0) {
            url = url.substring(0, slashIndex);
        }

        return url;
    }

    public boolean containsSensitiveKeywords(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        String lowerText = text.toLowerCase();
        for (String keyword : SENSITIVE_KEYWORDS) {
            if (lowerText.contains(keyword)) {
                log.warn("文本包含敏感词: {}", keyword);
                return true;
            }
        }

        return false;
    }

    public boolean isQuerySensitive(String query) {
        return containsSensitiveKeywords(query);
    }

    public String getSourceConfidence(String sourceUrl) {
        if (!isAllowedSource(sourceUrl)) {
            return "UNKNOWN";
        }

        if (sourceUrl.contains("flk.npc.gov.cn") || sourceUrl.contains("npc.gov.cn")) {
            return "HIGH";
        } else if (sourceUrl.contains("court.gov.cn") || sourceUrl.contains("wenshu.court.gov.cn")) {
            return "HIGH";
        } else if (sourceUrl.contains("pkulaw.cn")) {
            return "MEDIUM";
        }

        return "MEDIUM";
    }

    public List<String> getAllowedDomains() {
        return ALLOWED_DOMAINS.stream().sorted().toList();
    }
}