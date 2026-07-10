package com.legalai.admin.controller;

import com.legalai.admin.service.JudgmentImportService;
import com.legalai.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/admin/data-import")
@CrossOrigin
public class JudgmentImportController {
    private static final Logger log = LoggerFactory.getLogger(JudgmentImportController.class);

    @Autowired
    private JudgmentImportService judgmentImportService;

    private final Map<Long, Map<String, Object>> caseImportJobs = new ConcurrentHashMap<>();
    private final Map<Long, Long> caseImportCounter = new ConcurrentHashMap<>();

    @PostMapping("/judgments/preview")
    public ApiResponse<Map<String, Object>> previewJudgmentImport(@RequestParam("file") MultipartFile file) {
        try {
            Map<String, Object> preview = judgmentImportService.previewImport(file);

            int totalRows = (int) preview.getOrDefault("totalRows", 0);
            int errors = (int) preview.getOrDefault("errors", 0);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("previewRows", preview.get("previewRows"));
            result.put("totalRows", totalRows);
            result.put("errors", errors);
            result.put("data", preview.get("data"));

            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("预览判决书导入失败: {}", e.getMessage(), e);
            return ApiResponse.error(500, "预览失败: " + e.getMessage());
        }
    }

    @PostMapping("/judgments/confirm")
    public ApiResponse<Map<String, Object>> confirmJudgmentImport(@RequestBody Map<String, Object> payload) {
        try {
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> cases = (java.util.List<Map<String, Object>>) payload.get("cases");
            if (cases == null || cases.isEmpty()) {
                return ApiResponse.error(400, "没有可导入的案例数据");
            }

            long jobId = System.currentTimeMillis();
            int total = cases.size();

            Map<String, Object> jobInfo = new ConcurrentHashMap<>();
            jobInfo.put("status", "running");
            jobInfo.put("total", total);
            jobInfo.put("processed", 0);
            jobInfo.put("imported", 0);
            jobInfo.put("skipped", 0);
            jobInfo.put("startedAt", System.currentTimeMillis());
            caseImportJobs.put(jobId, jobInfo);
            caseImportCounter.put(jobId, 0L);

            new Thread(() -> {
                try {
                    Map<String, Object> result = judgmentImportService.confirmImport(cases, progress -> {
                        Map<String, Object> info = caseImportJobs.get(jobId);
                        if (info != null) {
                            info.put("processed", progress);
                            info.put("imported", progress);
                        }
                    });
                    Map<String, Object> info = caseImportJobs.get(jobId);
                    if (info != null) {
                        info.put("status", "success");
                        info.put("imported", result.getOrDefault("imported", 0));
                        info.put("skipped", result.getOrDefault("skipped", 0));
                        info.put("finishedAt", System.currentTimeMillis());
                    }
                } catch (Exception e) {
                    Map<String, Object> info = caseImportJobs.get(jobId);
                    if (info != null) {
                        info.put("status", "failed");
                        info.put("error", e.getMessage());
                        info.put("finishedAt", System.currentTimeMillis());
                    }
                } finally {
                    caseImportCounter.remove(jobId);
                }
            }).start();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("jobId", jobId);
            result.put("total", total);

            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("确认导入判决书失败: {}", e.getMessage(), e);
            return ApiResponse.error(500, "导入失败: " + e.getMessage());
        }
    }

    @GetMapping("/judgments/status/{jobId}")
    public ApiResponse<Map<String, Object>> getJudgmentImportStatus(@PathVariable Long jobId) {
        Map<String, Object> job = caseImportJobs.get(jobId);
        if (job == null) {
            return ApiResponse.error(404, "任务不存在");
        }

        Map<String, Object> result = new LinkedHashMap<>(job);
        int total = (int) job.getOrDefault("total", 0);
        int processed = (int) job.getOrDefault("processed", 0);
        int progress = total > 0 ? (int) (processed * 100.0 / total) : 0;
        result.put("progress", Math.min(progress, 100));

        return ApiResponse.success(result);
    }
}
