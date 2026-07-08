package com.legalai.admin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AppLogService {
    private static final Logger log = LoggerFactory.getLogger(AppLogService.class);

    @Value("${app.log.path:/var/log/legal-ai-assistant/app.log}")
    private String logPath;

    public Map<String, Object> getLogs(String level, int page, int pageSize) {
        Map<String, Object> result = new LinkedHashMap<>();

        try {
            File logFile = new File(logPath);
            if (!logFile.exists()) {
                result.put("logs", Collections.emptyList());
                result.put("total", 0);
                result.put("message", "日志文件不存在: " + logPath);
                return result;
            }

            List<String> allLines = Files.readAllLines(logFile.toPath(), StandardCharsets.UTF_8);

            List<String> filtered = allLines.stream()
                .filter(line -> filterByLevel(line, level))
                .collect(Collectors.toList());

            int total = filtered.size();
            int start = (page - 1) * pageSize;
            int end = Math.min(start + pageSize, total);

            List<String> pageLogs = start < total
                ? filtered.subList(start, end)
                : Collections.emptyList();

            result.put("logs", pageLogs);
            result.put("total", total);
            result.put("page", page);
            result.put("pageSize", pageSize);

        } catch (Exception e) {
            log.warn("[AppLog] read logs failed: {}", e.getMessage());
            result.put("logs", Collections.emptyList());
            result.put("total", 0);
            result.put("error", e.getMessage());
        }

        return result;
    }

    private boolean filterByLevel(String line, String level) {
        if ("ALL".equalsIgnoreCase(level)) return true;
        String upper = line.toUpperCase();
        switch (level.toUpperCase()) {
            case "ERROR": return upper.contains(" ERROR ") || upper.contains("ERROR:");
            case "WARN": return upper.contains(" WARN ") || upper.contains("WARN:");
            case "INFO": return upper.contains(" INFO ") || upper.contains("INFO:");
            case "DEBUG": return upper.contains(" DEBUG ") || upper.contains("DEBUG:");
            default: return true;
        }
    }
}
