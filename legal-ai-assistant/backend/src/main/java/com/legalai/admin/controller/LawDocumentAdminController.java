package com.legalai.admin.controller;

import com.legalai.dto.ApiResponse;
import com.legalai.dto.LegalSearchResponse;
import com.legalai.model.LawDocument;
import com.legalai.model.LawArticle;
import com.legalai.repository.LawDocumentMapper;
import com.legalai.repository.LawArticleMapper;
import com.legalai.service.ElasticsearchService;
import com.legalai.service.MilvusService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/law-document")
@CrossOrigin
@Tag(name = "法规文档管理", description = "法规文档的创建、编辑、删除、批量操作和导出")
public class LawDocumentAdminController {
    private static final Logger log = LoggerFactory.getLogger(LawDocumentAdminController.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private LawDocumentMapper lawDocumentMapper;

    @Autowired
    private LawArticleMapper lawArticleMapper;

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Autowired
    private MilvusService milvusService;

    @PostMapping("/create")
    @Operation(summary = "创建法规", description = "手动创建单条法规")
    public ApiResponse<Map<String, Object>> createLaw(@RequestBody Map<String, Object> payload) {
        try {
            String lawUuid = "LAW-" + System.currentTimeMillis() + "-" + Math.abs((int)(Math.random() * 10000));
            String title = (String) payload.get("title");
            String shortTitle = (String) payload.get("shortTitle");
            String categoryL1 = (String) payload.get("categoryL1");
            String categoryL2 = (String) payload.get("categoryL2");
            String issuingAuthority = (String) payload.get("issuingAuthority");
            String issueDate = (String) payload.get("issueDate");
            String effectiveDate = (String) payload.get("effectiveDate");
            Integer status = payload.get("status") != null ? ((Number) payload.get("status")).intValue() : 1;
            String sourceUrl = (String) payload.get("sourceUrl");
            String sourceName = (String) payload.get("sourceName");

            String sql = """
                INSERT INTO law_document (law_uuid, title, short_title, category_l1, category_l2,
                    issuing_authority, issue_date, effective_date, status, source_url, source_name,
                    view_count, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, NOW(), NOW())
                """;

            jdbcTemplate.update(sql, lawUuid, title, shortTitle, categoryL1, categoryL2,
                    issuingAuthority,
                    issueDate != null && !issueDate.isEmpty() ? LocalDate.parse(issueDate) : null,
                    effectiveDate != null && !effectiveDate.isEmpty() ? LocalDate.parse(effectiveDate) : null,
                    status, sourceUrl, sourceName);

            Long lawId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", lawId);
            result.put("lawUuid", lawUuid);
            result.put("title", title);

            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("创建法规失败: {}", e.getMessage(), e);
            return ApiResponse.error(500, "创建法规失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/update")
    @Operation(summary = "更新法规", description = "更新法规基本信息")
    public ApiResponse<Map<String, Object>> updateLaw(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        try {
            LawDocument existing = lawDocumentMapper.selectById(id);
            if (existing == null) {
                return ApiResponse.error(404, "法规不存在");
            }

            StringBuilder sql = new StringBuilder("UPDATE law_document SET updated_at = NOW()");
            List<Object> params = new ArrayList<>();

            if (payload.containsKey("title")) {
                sql.append(", title = ?");
                params.add(payload.get("title"));
            }
            if (payload.containsKey("shortTitle")) {
                sql.append(", short_title = ?");
                params.add(payload.get("shortTitle"));
            }
            if (payload.containsKey("categoryL1")) {
                sql.append(", category_l1 = ?");
                params.add(payload.get("categoryL1"));
            }
            if (payload.containsKey("categoryL2")) {
                sql.append(", category_l2 = ?");
                params.add(payload.get("categoryL2"));
            }
            if (payload.containsKey("issuingAuthority")) {
                sql.append(", issuing_authority = ?");
                params.add(payload.get("issuingAuthority"));
            }
            if (payload.containsKey("issueDate")) {
                sql.append(", issue_date = ?");
                String issueDate = (String) payload.get("issueDate");
                params.add(issueDate != null && !issueDate.isEmpty() ? LocalDate.parse(issueDate) : null);
            }
            if (payload.containsKey("effectiveDate")) {
                sql.append(", effective_date = ?");
                String effectiveDate = (String) payload.get("effectiveDate");
                params.add(effectiveDate != null && !effectiveDate.isEmpty() ? LocalDate.parse(effectiveDate) : null);
            }
            if (payload.containsKey("status")) {
                sql.append(", status = ?");
                params.add(((Number) payload.get("status")).intValue());
            }
            if (payload.containsKey("sourceUrl")) {
                sql.append(", source_url = ?");
                params.add(payload.get("sourceUrl"));
            }
            if (payload.containsKey("sourceName")) {
                sql.append(", source_name = ?");
                params.add(payload.get("sourceName"));
            }

            sql.append(" WHERE id = ?");
            params.add(id);

            jdbcTemplate.update(sql.toString(), params.toArray());

            return ApiResponse.success(Map.of("id", id, "updated", true));
        } catch (Exception e) {
            log.error("更新法规失败: {}", e.getMessage(), e);
            return ApiResponse.error(500, "更新法规失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/delete")
    @Operation(summary = "删除法规", description = "删除法规及其所有条款")
    public ApiResponse<Map<String, Object>> deleteLaw(@PathVariable Long id) {
        try {
            LawDocument existing = lawDocumentMapper.selectById(id);
            if (existing == null) {
                return ApiResponse.error(404, "法规不存在");
            }

            jdbcTemplate.update("DELETE FROM law_article WHERE law_id = ?", id);
            jdbcTemplate.update("DELETE FROM law_document_category WHERE law_id = ?", id);
            jdbcTemplate.update("DELETE FROM law_relation WHERE law_id = ? OR related_law_id = ?", id, id);
            jdbcTemplate.update("DELETE FROM law_favorite WHERE law_uuid = ?", existing.getLawUuid());
            jdbcTemplate.update("DELETE FROM law_document WHERE id = ?", id);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", id);
            result.put("deleted", true);
            result.put("lawUuid", existing.getLawUuid());

            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("删除法规失败: {}", e.getMessage(), e);
            return ApiResponse.error(500, "删除法规失败: " + e.getMessage());
        }
    }

    @PostMapping("/batch-delete")
    @Operation(summary = "批量删除法规", description = "批量删除法规及其所有条款")
    public ApiResponse<Map<String, Object>> batchDeleteLaws(@RequestBody Map<String, Object> payload) {
        try {
            @SuppressWarnings("unchecked")
            List<Number> ids = (List<Number>) payload.get("ids");
            if (ids == null || ids.isEmpty()) {
                return ApiResponse.error(400, "请选择要删除的法规");
            }

            List<String> lawUuids = jdbcTemplate.queryForList(
                    "SELECT law_uuid FROM law_document WHERE id IN (" +
                    ids.stream().map(n -> "?").collect(Collectors.joining(",")) + ")",
                    String.class, ids.toArray());

            int articleDeleted = jdbcTemplate.update(
                    "DELETE FROM law_article WHERE law_id IN (" +
                    ids.stream().map(n -> "?").collect(Collectors.joining(",")) + ")",
                    ids.toArray());

            jdbcTemplate.update(
                    "DELETE FROM law_document_category WHERE law_id IN (" +
                    ids.stream().map(n -> "?").collect(Collectors.joining(",")) + ")",
                    ids.toArray());

            jdbcTemplate.update(
                    "DELETE FROM law_relation WHERE law_id IN (" +
                    ids.stream().map(n -> "?").collect(Collectors.joining(",")) + ") OR related_law_id IN (" +
                    ids.stream().map(n -> "?").collect(Collectors.joining(",")) + ")",
                    java.util.stream.IntStream.range(0, 2).mapToObj(i -> ids).flatMap(List::stream).toArray());

            if (!lawUuids.isEmpty()) {
                String inClause = lawUuids.stream().map(u -> "?").collect(Collectors.joining(","));
                jdbcTemplate.update("DELETE FROM law_favorite WHERE law_uuid IN (" + inClause + ")", lawUuids.toArray());
            }

            int lawDeleted = jdbcTemplate.update(
                    "DELETE FROM law_document WHERE id IN (" +
                    ids.stream().map(n -> "?").collect(Collectors.joining(",")) + ")",
                    ids.toArray());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("deletedCount", lawDeleted);
            result.put("articleDeletedCount", articleDeleted);

            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("批量删除法规失败: {}", e.getMessage(), e);
            return ApiResponse.error(500, "批量删除法规失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/articles/add")
    @Operation(summary = "添加法规条款", description = "为法规添加单条条款")
    public ApiResponse<Map<String, Object>> addArticle(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        try {
            LawDocument law = lawDocumentMapper.selectById(id);
            if (law == null) {
                return ApiResponse.error(404, "法规不存在");
            }

            String articleUuid = "ART-" + System.currentTimeMillis() + "-" + Math.abs((int)(Math.random() * 10000));
            String articleNo = (String) payload.get("articleNo");
            String title = (String) payload.get("title");
            String content = (String) payload.get("content");
            Integer sortOrder = payload.get("sortOrder") != null ? ((Number) payload.get("sortOrder")).intValue() : null;

            if (sortOrder == null) {
                Integer maxOrder = jdbcTemplate.queryForObject(
                        "SELECT MAX(sort_order) FROM law_article WHERE law_id = ?", Integer.class, id);
                sortOrder = (maxOrder != null ? maxOrder : 0) + 1;
            }

            String contentHash = content != null ? sha256(content) : "";

            String sql = """
                INSERT INTO law_article (law_id, article_uuid, article_no, title, content, content_hash, sort_order, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, NOW())
                """;
            jdbcTemplate.update(sql, id, articleUuid, articleNo, title, content, contentHash, sortOrder);

            Long articleId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

            return ApiResponse.success(Map.of("id", articleId, "articleUuid", articleUuid));
        } catch (Exception e) {
            log.error("添加条款失败: {}", e.getMessage(), e);
            return ApiResponse.error(500, "添加条款失败: " + e.getMessage());
        }
    }

    @PostMapping("/{lawId}/articles/{articleId}/update")
    @Operation(summary = "更新法规条款", description = "更新法规条款内容")
    public ApiResponse<Map<String, Object>> updateArticle(
            @PathVariable Long lawId, @PathVariable Long articleId, @RequestBody Map<String, Object> payload) {
        try {
            QueryWrapper<LawArticle> query = new QueryWrapper<>();
            query.eq("id", articleId).eq("law_id", lawId);
            LawArticle article = lawArticleMapper.selectOne(query);
            if (article == null) {
                return ApiResponse.error(404, "条款不存在");
            }

            StringBuilder sql = new StringBuilder("UPDATE law_article SET updated_at = NOW()");
            List<Object> params = new ArrayList<>();

            if (payload.containsKey("articleNo")) {
                sql.append(", article_no = ?");
                params.add(payload.get("articleNo"));
            }
            if (payload.containsKey("title")) {
                sql.append(", title = ?");
                params.add(payload.get("title"));
            }
            if (payload.containsKey("content")) {
                sql.append(", content = ?");
                params.add(payload.get("content"));
                sql.append(", content_hash = ?");
                params.add(sha256((String) payload.get("content")));
            }
            if (payload.containsKey("sortOrder")) {
                sql.append(", sort_order = ?");
                params.add(((Number) payload.get("sortOrder")).intValue());
            }

            sql.append(" WHERE id = ?");
            params.add(articleId);

            jdbcTemplate.update(sql.toString(), params.toArray());

            return ApiResponse.success(Map.of("id", articleId, "updated", true));
        } catch (Exception e) {
            log.error("更新条款失败: {}", e.getMessage(), e);
            return ApiResponse.error(500, "更新条款失败: " + e.getMessage());
        }
    }

    @PostMapping("/{lawId}/articles/{articleId}/delete")
    @Operation(summary = "删除法规条款", description = "删除单条法规条款")
    public ApiResponse<Map<String, Object>> deleteArticle(@PathVariable Long lawId, @PathVariable Long articleId) {
        try {
            QueryWrapper<LawArticle> query = new QueryWrapper<>();
            query.eq("id", articleId).eq("law_id", lawId);
            LawArticle article = lawArticleMapper.selectOne(query);
            if (article == null) {
                return ApiResponse.error(404, "条款不存在");
            }

            jdbcTemplate.update("DELETE FROM law_article WHERE id = ?", articleId);

            return ApiResponse.success(Map.of("id", articleId, "deleted", true));
        } catch (Exception e) {
            log.error("删除条款失败: {}", e.getMessage(), e);
            return ApiResponse.error(500, "删除条款失败: " + e.getMessage());
        }
    }

    @GetMapping("/export")
    @Operation(summary = "导出法规", description = "导出法规数据为JSON格式")
    public ApiResponse<Map<String, Object>> exportLaws(
            @Parameter(description = "导出格式") @RequestParam(defaultValue = "json") String format,
            @Parameter(description = "状态过滤") @RequestParam(required = false) Integer status,
            @Parameter(description = "分类过滤") @RequestParam(required = false) String category,
            @Parameter(description = "是否包含条款") @RequestParam(defaultValue = "true") boolean includeArticles) {

        try {
            StringBuilder sql = new StringBuilder("SELECT * FROM law_document WHERE 1=1");
            List<Object> params = new ArrayList<>();

            if (status != null) {
                sql.append(" AND status = ?");
                params.add(status);
            }
            if (category != null && !category.isEmpty()) {
                sql.append(" AND (category_l1 = ? OR category_l2 = ?)");
                params.add(category);
                params.add(category);
            }

            sql.append(" ORDER BY created_at DESC");

            List<Map<String, Object>> laws = jdbcTemplate.queryForList(sql.toString(), params.toArray());

            if (includeArticles && !laws.isEmpty()) {
                List<Long> lawIds = laws.stream().map(l -> ((Number) l.get("id")).longValue()).collect(Collectors.toList());
                String articlesSql = "SELECT * FROM law_article WHERE law_id IN (" +
                        lawIds.stream().map(id -> "?").collect(Collectors.joining(",")) + ") ORDER BY law_id, sort_order";
                List<Map<String, Object>> allArticles = jdbcTemplate.queryForList(articlesSql, lawIds.toArray());

                Map<Long, List<Map<String, Object>>> articlesByLaw = allArticles.stream()
                        .collect(Collectors.groupingBy(a -> ((Number) a.get("law_id")).longValue()));

                for (Map<String, Object> law : laws) {
                    Long lawId = ((Number) law.get("id")).longValue();
                    law.put("articles", articlesByLaw.getOrDefault(lawId, new ArrayList<>()));
                }
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("laws", laws);
            result.put("total", laws.size());
            result.put("exportTime", LocalDateTime.now().toString());

            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("导出法规失败: {}", e.getMessage(), e);
            return ApiResponse.error(500, "导出法规失败: " + e.getMessage());
        }
    }

    @GetMapping("/data-quality")
    @Operation(summary = "数据质量报告", description = "生成跨库数据一致性报告")
    public ApiResponse<Map<String, Object>> dataQualityReport() {
        Map<String, Object> report = new LinkedHashMap<>();

        try {
            Long mysqlLawCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM law_document", Long.class);
            Long mysqlArticleCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM law_article", Long.class);

            Map<String, Object> mysqlStats = new LinkedHashMap<>();
            mysqlStats.put("lawCount", mysqlLawCount);
            mysqlStats.put("articleCount", mysqlArticleCount);
            report.put("mysql", mysqlStats);

            Map<String, Object> esStats = new LinkedHashMap<>();
            try {
                if (elasticsearchService.isAvailable()) {
                    List<LegalSearchResponse.SearchResultItem> esDocs = elasticsearchService.searchByES("", 1, 1000, null);
                    esStats.put("articleCount", esDocs.size());
                    esStats.put("available", true);
                } else {
                    esStats.put("available", false);
                    esStats.put("message", "ES不可用");
                }
            } catch (Exception e) {
                esStats.put("available", false);
                esStats.put("message", "ES查询失败: " + e.getMessage());
            }
            report.put("elasticsearch", esStats);

            Map<String, Object> consistency = new LinkedHashMap<>();
            consistency.put("mysqlLaws", mysqlLawCount);
            consistency.put("description", "跨库一致性校验需要ES可用时才能完成");
            report.put("consistency", consistency);

            report.put("generatedAt", LocalDateTime.now().toString());
            report.put("status", "completed");

        } catch (Exception e) {
            log.error("生成数据质量报告失败: {}", e.getMessage(), e);
            return ApiResponse.error(500, "生成数据质量报告失败: " + e.getMessage());
        }

        return ApiResponse.success(report);
    }

    private String sha256(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }
}
