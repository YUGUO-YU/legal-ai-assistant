package com.legalai.admin.controller;

import com.legalai.dto.ApiResponse;
import com.legalai.service.LawImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController("adminLawImportController")
@RequestMapping("/api/v1/admin/law-import")
@CrossOrigin
public class LawImportController {
    private static final Logger log = LoggerFactory.getLogger(LawImportController.class);

    @Autowired
    private LawImportService lawImportService;

    @GetMapping("/history/{jobId}")
    public ApiResponse<Map<String, Object>> getImportJobStatus(@PathVariable Long jobId) {
        try {
            var job = lawImportService.loadJob(jobId);
            if (job == null) {
                return ApiResponse.error(404, "任务不存在");
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", job.getId());
            result.put("taskUuid", job.getTaskUuid());
            result.put("lawName", job.getLawName());
            result.put("source", job.getSource());
            result.put("status", job.getStatus());
            result.put("totalArticles", job.getTotalArticles());
            result.put("insertedArticles", job.getInsertedArticles());
            result.put("updatedArticles", job.getUpdatedArticles());
            result.put("mysqlOk", job.getMysqlOk());
            result.put("esOk", job.getEsOk());
            result.put("milvusOk", job.getMilvusOk());
            result.put("errorMessage", job.getErrorMessage());
            result.put("operator", job.getOperator());
            result.put("startedAt", job.getStartedAt());
            result.put("finishedAt", job.getFinishedAt());

            int progress = 0;
            if (job.getTotalArticles() != null && job.getTotalArticles() > 0) {
                int processed = (job.getInsertedArticles() != null ? job.getInsertedArticles() : 0)
                              + (job.getUpdatedArticles() != null ? job.getUpdatedArticles() : 0);
                progress = (int) (processed * 100.0 / job.getTotalArticles());
                progress = Math.min(progress, 100);
            }
            if ("success".equals(job.getStatus()) || "failed".equals(job.getStatus())) {
                progress = 100;
            }
            result.put("progress", progress);

            int successCount = job.getInsertedArticles() != null ? job.getInsertedArticles() : 0;
            int failCount = job.getTotalArticles() != null ? job.getTotalArticles() - successCount : 0;
            if ("failed".equals(job.getStatus())) {
                failCount = job.getTotalArticles() != null ? job.getTotalArticles() : 0;
                successCount = 0;
            }
            result.put("successCount", successCount);
            result.put("failCount", failCount);

            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("查询导入任务状态失败: {}", e.getMessage(), e);
            return ApiResponse.error(500, "查询失败: " + e.getMessage());
        }
    }
}
