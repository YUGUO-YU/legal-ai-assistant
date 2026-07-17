package com.legalai.admin.controller;

import com.legalai.admin.service.AdminDataService;
import com.legalai.admin.service.ConfigRefreshService;
import com.legalai.admin.service.AppLogService;
import com.legalai.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/sys")
@CrossOrigin
@Tag(name = "管理后台-系统配置", description = "系统配置、字典、缓存、健康检查")
public class SystemController {
    @Autowired
    private AdminDataService adminDataService;

    @Autowired
    private AdminHelper adminHelper;

    @Autowired
    private ConfigRefreshService configRefreshService;

    @Autowired
    private AppLogService appLogService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.legalai.service.CacheService cacheService;

    @Operation(summary = "查询系统配置")
    @GetMapping("/configs")
    public ApiResponse<Map<String, Object>> sysConfigs(
            @RequestParam(required = false) String group,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize) {
        return ApiResponse.success(adminDataService.list("sys_config", group, page, pageSize, null));
    }

    @GetMapping("/dicts")
    public ApiResponse<Map<String, Object>> sysDicts(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int pageSize) {
        return ApiResponse.success(adminDataService.list("sys_dict", type, page, pageSize, null));
    }

    @PostMapping("/cache/refresh")
    public ApiResponse<Map<String, Object>> refreshCache() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            if (cacheService != null) {
                cacheService.invalidateSearchCache();
                result.put("ok", true);
                result.put("message", "缓存已刷新");
            } else {
                result.put("ok", true);
                result.put("message", "缓存服务不可用（Redis未配置）");
            }
            configRefreshService.notifyAllObservers();
            result.put("configRefreshed", true);
        } catch (Exception e) {
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return ApiResponse.success(result);
    }

    @PostMapping("/configs/refresh")
    public ApiResponse<Map<String, Object>> refreshSysConfigs() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            configRefreshService.notifyChange("sys_config");
            result.put("ok", true);
            result.put("message", "配置已热更新");
        } catch (Exception e) {
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return ApiResponse.success(result);
    }

    @Operation(summary = "管理后台健康检查")
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scope", "admin");
        result.put("timestamp", System.currentTimeMillis());
        result.put("domains", java.util.Arrays.asList(
                "infra", "biz(mod01-mod10)", "ai", "ops", "monitor", "sys"));
        try {
            Integer dbOk = adminDataService.jdbc().queryForObject("SELECT 1", Integer.class);
            result.put("status", dbOk != null && dbOk == 1 ? "UP" : "DOWN");
            result.put("database", "UP");
        } catch (Exception e) {
            result.put("status", "DOWN");
            result.put("database", "DOWN");
            result.put("db_error", e.getMessage());
        }
        return ApiResponse.success(result);
    }

    @Operation(summary = "查询应用日志")
    @GetMapping("/app-logs")
    public ApiResponse<Map<String, Object>> getAppLogs(
            @RequestParam(defaultValue = "INFO") String level,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int pageSize) {
        return ApiResponse.success(appLogService.getLogs(level, page, pageSize));
    }

    @Operation(summary = "下载应用日志")
    @GetMapping("/app-logs/download")
    public void downloadAppLogs(HttpServletResponse response) throws IOException {
        File logFile = new File("/var/log/legal-ai-assistant/app.log");
        if (logFile.exists()) {
            response.setContentType("text/plain");
            response.setHeader("Content-Disposition", "attachment; filename=app.log");
            Files.copy(logFile.toPath(), response.getOutputStream());
        }
    }
}
