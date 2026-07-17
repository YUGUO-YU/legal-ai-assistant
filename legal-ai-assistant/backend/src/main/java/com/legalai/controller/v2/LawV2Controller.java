package com.legalai.controller.v2;

import com.legalai.dto.v2.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v2/laws")
@CrossOrigin
@Tag(name = "法规管理 v2", description = "法规管理API v2版本，支持分页、过滤、排序和字段选择")
public class LawV2Controller {

    private static final Set<String> ALLOWED_FIELDS = Set.of("id", "doc_uuid", "title", "short_title", "status", "category_l1", "category_l2", "issuing_authority", "document_no", "effective_date", "created_at", "updated_at");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping
    @Operation(summary = "获取法规列表", description = "支持分页、过滤、排序和字段选择")
    public ApiResponse<Map<String, Object>> listLaws(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(description = "过滤状态") @RequestParam(required = false) String status,
            @Parameter(description = "过滤分类") @RequestParam(required = false) String category,
            @Parameter(description = "排序字段") @RequestParam(required = false) String sort,
            @Parameter(description = "选择字段") @RequestParam(required = false) List<String> fields,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String search) {

        List<String> safeFields = new ArrayList<>();
        if (fields != null && !fields.isEmpty()) {
            for (String f : fields) {
                String trimmed = f.trim();
                if (ALLOWED_FIELDS.contains(trimmed)) {
                    safeFields.add(trimmed);
                }
            }
        }
        String baseFields = safeFields.isEmpty() ? "*" : String.join(", ", safeFields);

        StringBuilder sql = new StringBuilder("SELECT " + baseFields + " FROM law_document WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (status != null && !status.isEmpty()) {
            sql.append(" AND status = ?");
            params.add(status);
        }

        if (category != null && !category.isEmpty()) {
            sql.append(" AND (category_l1 = ? OR category_l2 = ?)");
            params.add(category);
            params.add(category);
        }

        if (search != null && !search.isEmpty()) {
            sql.append(" AND (title LIKE ? OR short_title LIKE ?)");
            params.add("%" + search + "%");
            params.add("%" + search + "%");
        }

        String countSql = "SELECT COUNT(*) FROM law_document WHERE 1=1";
        if (status != null && !status.isEmpty()) {
            countSql += " AND status = ?";
        }
        if (category != null && !category.isEmpty()) {
            countSql += " AND (category_l1 = ? OR category_l2 = ?)";
        }
        if (search != null && !search.isEmpty()) {
            countSql += " AND (title LIKE ? OR short_title LIKE ?)";
        }

        String sortField = "created_at";
        String sortOrder = "DESC";
        if (sort != null && !sort.isEmpty()) {
            if (sort.startsWith("-")) {
                sortField = sort.substring(1);
                sortOrder = "ASC";
            } else {
                sortField = sort;
            }
            Set<String> allowedSort = Set.of("created_at", "updated_at", "title", "doc_uuid", "status");
            if (!allowedSort.contains(sortField)) {
                sortField = "created_at";
            }
            sql.append(" ORDER BY ").append(sortField).append(" ").append(sortOrder);
        } else {
            sql.append(" ORDER BY created_at DESC");
        }

        int offset = (page - 1) * pageSize;
        sql.append(" LIMIT ? OFFSET ?");

        long total = 0;
        try {
            List<Object> countArgs = new ArrayList<>();
            if (status != null && !status.isEmpty()) countArgs.add(status);
            if (category != null && !category.isEmpty()) {
                countArgs.add(category);
                countArgs.add(category);
            }
            if (search != null && !search.isEmpty()) {
                countArgs.add("%" + search + "%");
                countArgs.add("%" + search + "%");
            }
            total = jdbcTemplate.queryForObject(countSql, Long.class, countArgs.toArray());
        } catch (Exception e) {
            total = 0;
        }

        params.add(pageSize);
        params.add(offset);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), params.toArray());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", rows);
        result.put("_page", page);
        result.put("_pageSize", pageSize);

        ApiResponse.Pagination pagination = ApiResponse.Pagination.of(page, pageSize, total);

        return ApiResponse.success(result, pagination);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取法规详情", description = "根据ID获取法规详细信息")
    public ApiResponse<Map<String, Object>> getLaw(
            @PathVariable Long id,
            @Parameter(description = "选择字段") @RequestParam(required = false) List<String> fields) {

        List<String> safeFields = new ArrayList<>();
        if (fields != null && !fields.isEmpty()) {
            for (String f : fields) {
                String trimmed = f.trim();
                if (ALLOWED_FIELDS.contains(trimmed)) {
                    safeFields.add(trimmed);
                }
            }
        }
        String baseFields = safeFields.isEmpty() ? "*" : String.join(", ", safeFields);

        String sql = "SELECT " + baseFields + " FROM law_document WHERE id = ?";

        try {
            Map<String, Object> law = jdbcTemplate.queryForMap(sql, id);
            return ApiResponse.success(law);
        } catch (Exception e) {
            return ApiResponse.error(404, "法规不存在");
        }
    }

    @GetMapping("/{id}/articles")
    @Operation(summary = "获取法规条款", description = "获取指定法规的所有条款")
    public ApiResponse<List<Map<String, Object>>> getLawArticles(
            @PathVariable Long id,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "50") int pageSize) {

        int offset = (page - 1) * pageSize;

        String countSql = "SELECT COUNT(*) FROM law_article WHERE law_id = ?";
        long total = jdbcTemplate.queryForObject(countSql, Long.class, id);

        String sql = "SELECT * FROM law_article WHERE law_id = ? ORDER BY sort_order, id LIMIT ? OFFSET ?";
        List<Map<String, Object>> articles = jdbcTemplate.queryForList(sql, id, pageSize, offset);

        return ApiResponse.success(articles, ApiResponse.Pagination.of(page, pageSize, total));
    }

    @GetMapping("/categories")
    @Operation(summary = "获取法规分类", description = "获取所有法规分类")
    public ApiResponse<List<Map<String, Object>>> getCategories(
            @Parameter(description = "分类类型") @RequestParam(required = false) String type) {

        if (type != null && !type.isEmpty()) {
            String sql = "SELECT * FROM law_category WHERE category_type = ?";
            return ApiResponse.success(jdbcTemplate.queryForList(sql, type));
        }
        return ApiResponse.success(jdbcTemplate.queryForList("SELECT * FROM law_category"));
    }

    @GetMapping("/stats")
    @Operation(summary = "获取法规统计", description = "获取法规统计数据")
    public ApiResponse<Map<String, Object>> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        Long totalLaws = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM law_document", Long.class);
        Long totalArticles = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM law_article", Long.class);
        Long activeLaws = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM law_document WHERE status = 'active'", Long.class);

        stats.put("totalLaws", totalLaws);
        stats.put("totalArticles", totalArticles);
        stats.put("activeLaws", activeLaws);

        return ApiResponse.success(stats);
    }
}
