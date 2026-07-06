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

@RestController
@RequestMapping("/api/v1/admin/data-import")
@CrossOrigin
public class JudgmentImportController {
    private static final Logger log = LoggerFactory.getLogger(JudgmentImportController.class);

    @Autowired
    private JudgmentImportService judgmentImportService;

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

            Map<String, Object> result = judgmentImportService.confirmImport(cases);

            int imported = (int) result.getOrDefault("imported", 0);
            int skipped = (int) result.getOrDefault("skipped", 0);

            log.info("确认导入判决书: 成功={}, 跳过={}", imported, skipped);

            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("确认导入判决书失败: {}", e.getMessage(), e);
            return ApiResponse.error(500, "导入失败: " + e.getMessage());
        }
    }
}
