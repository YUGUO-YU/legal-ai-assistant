package com.legalai.admin.controller;

import com.legalai.dto.ApiResponse;
import com.legalai.dto.LawImportPreview;
import com.legalai.service.LawImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/law-import")
@CrossOrigin
public class LawImportController {
    private static final Logger log = LoggerFactory.getLogger(LawImportController.class);

    @Autowired
    private LawImportService lawImportService;

    @PostMapping("/preview")
    public ApiResponse<Map<String, Object>> previewImport(@RequestParam("file") MultipartFile file) {
        try {
            LawImportPreview preview = lawImportService.previewImport(file);

            List<Map<String, Object>> previewRows = preview.getArticles().stream().limit(10).map(a -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("title", a.getTitle() != null ? a.getTitle() : a.getArticleNo());
                row.put("category", preview.getSuggestedCategories() != null && !preview.getSuggestedCategories().isEmpty()
                    ? preview.getSuggestedCategories().get(0).getCategoryName() : null);
                row.put("content", a.getContent() != null && a.getContent().length() > 100
                    ? a.getContent().substring(0, 100) + "..." : a.getContent());
                row.put("valid", a.getContent() != null && !a.getContent().isBlank());
                row.put("error", a.getContent() == null || a.getContent().isBlank() ? "缺少正文内容" : null);
                return row;
            }).toList();

            int errors = (int) preview.getArticles().stream()
                .filter(a -> a.getContent() == null || a.getContent().isBlank()).count();
            int warnings = preview.getSuggestedCategories() == null || preview.getSuggestedCategories().isEmpty() ? 1 : 0;

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("totalRows", preview.getArticles().size());
            data.put("previewRows", previewRows);
            data.put("errors", errors);
            data.put("warnings", warnings);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("lawTitle", preview.getLawTitle());
            result.put("shortTitle", preview.getShortTitle());
            result.put("documentNo", preview.getDocumentNo());
            result.put("issuingAuthority", preview.getIssuingAuthority());
            result.put("issueDate", preview.getIssueDate());
            result.put("effectiveDate", preview.getEffectiveDate());
            result.put("chapters", preview.getChapterTree());
            result.put("_preview", data);
            result.put("articles", preview.getArticles());

            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("预览导入失败: {}", e.getMessage(), e);
            return ApiResponse.error(500, "预览失败: " + e.getMessage());
        }
    }

    @PostMapping("/confirm")
    public ApiResponse<Map<String, Object>> confirmImport(@RequestBody Map<String, Object> payload) {
        try {
            String lawTitle = (String) payload.get("lawTitle");
            String shortTitle = (String) payload.get("shortTitle");
            String documentNo = (String) payload.get("documentNo");
            String issuingAuthority = (String) payload.get("issuingAuthority");
            String issueDate = (String) payload.get("issueDate");
            String effectiveDate = (String) payload.get("effectiveDate");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> chapters = (List<Map<String, Object>>) payload.get("chapters");

            LawImportPreview preview = new LawImportPreview();
            preview.setLawTitle(lawTitle);
            preview.setShortTitle(shortTitle);
            preview.setDocumentNo(documentNo);
            preview.setIssuingAuthority(issuingAuthority);
            preview.setIssueDate(issueDate);
            preview.setEffectiveDate(effectiveDate);

            if (chapters != null) {
                List<LawImportPreview.ChapterNode> chapterNodes = chapters.stream().map(c -> {
                    LawImportPreview.ChapterNode node = new LawImportPreview.ChapterNode();
                    node.setTitle((String) c.get("title"));
                    node.setLevel(c.get("level") != null ? ((Number) c.get("level")).intValue() : 1);
                    return node;
                }).toList();
                preview.setChapterTree(chapterNodes);
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> articlesData = (List<Map<String, Object>>) payload.get("articles");
            if (articlesData != null) {
                List<LawImportPreview.ArticleParse> articles = articlesData.stream().map(a -> {
                    LawImportPreview.ArticleParse ap = new LawImportPreview.ArticleParse();
                    ap.setArticleNo((String) a.get("articleNo"));
                    ap.setTitle((String) a.get("title"));
                    ap.setContent((String) a.get("content"));
                    ap.setChapterPath((String) a.get("chapterPath"));
                    return ap;
                }).toList();
                preview.setArticles(articles);
            }

            var job = lawImportService.confirmImport(preview, "admin");

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("jobId", job.getId());
            result.put("imported", preview.getArticles() != null ? preview.getArticles().size() : 0);
            result.put("skipped", 0);

            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("确认导入失败: {}", e.getMessage(), e);
            return ApiResponse.error(500, "导入失败: " + e.getMessage());
        }
    }

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
