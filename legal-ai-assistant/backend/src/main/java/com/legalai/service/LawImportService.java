package com.legalai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalai.dto.LawImportJob;
import com.legalai.llm.LLMClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class LawImportService {

    private static final String INSERT_LAW_SQL = """
        INSERT INTO law_document
            (law_uuid, title, short_title, category_l1, category_l2,
             issuing_authority, issue_date, effective_date, status,
             source_url, source_name)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """;

    private static final String UPDATE_LAW_SQL = """
        UPDATE law_document SET
            title = ?, short_title = ?, category_l1 = ?, category_l2 = ?,
            issuing_authority = ?, issue_date = ?, effective_date = ?,
            status = ?, source_url = ?, source_name = ?
        WHERE id = ?
    """;

    private static final String SELECT_LAW_BY_TITLE = """
        SELECT id, law_uuid, title, short_title, category_l1, category_l2,
               issuing_authority, issue_date, effective_date, status, source_url, source_name
        FROM law_document WHERE title = ? LIMIT 1
    """;

    private static final String INSERT_ARTICLE_SQL = """
        INSERT INTO law_article
            (law_id, article_uuid, article_no, title, content, content_hash, sort_order)
        VALUES (?, ?, ?, ?, ?, ?, ?)
    """;

    private static final String SELECT_ARTICLE_HASH = """
        SELECT content_hash FROM law_article WHERE law_id = ? AND article_no = ?
    """;

    private static final String UPDATE_ARTICLE_SQL = """
        UPDATE law_article SET title = ?, content = ?, content_hash = ?, sort_order = ?
        WHERE id = ?
    """;

    private static final String SELECT_ARTICLE_ID = """
        SELECT id FROM law_article WHERE law_id = ? AND article_no = ?
    """;

    private static final String INSERT_HISTORY_SQL = """
        INSERT INTO law_import_history
            (task_uuid, law_name, source, status, total_articles, inserted_articles, updated_articles,
             mysql_ok, es_ok, milvus_ok, error_message, operator, started_at, finished_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """;

    private static final String UPDATE_HISTORY_SQL = """
        UPDATE law_import_history SET
            status = ?, total_articles = ?, inserted_articles = ?, updated_articles = ?,
            mysql_ok = ?, es_ok = ?, milvus_ok = ?, error_message = ?, finished_at = NOW()
        WHERE id = ?
    """;

    private static final String SELECT_HISTORY_PAGE = """
        SELECT id, task_uuid, law_name, source, status, total_articles, inserted_articles, updated_articles,
               mysql_ok, es_ok, milvus_ok, error_message, operator, started_at, finished_at
        FROM law_import_history
        ORDER BY started_at DESC
        LIMIT ? OFFSET ?
    """;

    private static final String COUNT_HISTORY = "SELECT COUNT(*) FROM law_import_history";

    private static final String SELECT_HISTORY_BY_ID = """
        SELECT id, task_uuid, law_name, source, status, total_articles, inserted_articles, updated_articles,
               mysql_ok, es_ok, milvus_ok, error_message, operator, started_at, finished_at
        FROM law_import_history WHERE id = ?
    """;

    private static final String SELECT_LAW_COUNT = "SELECT COUNT(*) FROM law_document";

    private static final String SELECT_ARTICLE_COUNT = "SELECT COUNT(*) FROM law_article";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AIService aiService;

    @Autowired
    private LLMClient llmClient;

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Autowired
    private MilvusService milvusService;

    @Value("${mock.enabled:false}")
    private boolean mockEnabled;

    /**
     * 三入口之一：联网搜索导入。AI 拉取数据，结构化后入库。
     */
    public LawImportJob importByWebSearch(String lawName, String operator) {
        return runImport(lawName, "web_search", operator, () -> fetchByWebSearch(lawName));
    }

    /**
     * 三入口之二：用户上传 JSON 字符串导入。
     */
    public LawImportJob importByUpload(String lawName, String jsonContent, String operator) {
        return runImport(lawName, "upload", operator, () -> parseUploadedJson(lawName, jsonContent));
    }

    /**
     * 三入口之三：预置种子数据导入（读取 classpath 下的 JSON 包）。
     */
    public LawImportJob importByPreset(String presetKey, String operator) {
        String filename = "seed-data/laws/" + presetKey + ".json";
        try (InputStream in = new ClassPathResource(filename).getInputStream()) {
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return importByUpload(presetKey, content, operator);
        } catch (IOException e) {
            throw new RuntimeException("预置数据文件不存在或读取失败: " + filename, e);
        }
    }

    private LawImportJob runImport(String lawName, String source, String operator,
                                   java.util.function.Supplier<ParsedLaw> loader) {
        String taskUuid = "IMPORT-" + System.currentTimeMillis();
        long historyId = createHistory(taskUuid, lawName, source, operator);

        ParsedLaw parsed;
        try {
            parsed = loader.get();
            if (parsed == null || parsed.articles == null || parsed.articles.isEmpty()) {
                finishHistory(historyId, "failed", 0, 0, 0, false, false, false,
                        "未拉取到任何条款", null);
                return loadJob(historyId);
            }
        } catch (Exception e) {
            log.error("[Import {}] 拉取数据失败: {}", taskUuid, e.getMessage(), e);
            finishHistory(historyId, "failed", 0, 0, 0, false, false, false,
                    "数据拉取失败: " + e.getMessage(), null);
            return loadJob(historyId);
        }

        int total = parsed.articles.size();
        boolean mysqlOk = false;
        boolean esOk = false;
        boolean milvusOk = false;
        int inserted = 0;
        int updated = 0;

        long lawId;
        try {
            Long existingId = findLawIdByTitle(parsed.lawTitle != null ? parsed.lawTitle : lawName);
            if (existingId != null) {
                lawId = existingId;
                updateLaw(lawId, parsed);
            } else {
                lawId = insertLaw(parsed);
                inserted++;
            }
            mysqlOk = true;
        } catch (Exception e) {
            log.error("[Import {}] MySQL 写入失败: {}", taskUuid, e.getMessage(), e);
            finishHistory(historyId, "failed", total, 0, 0, false, false, false,
                    "MySQL 写入失败: " + e.getMessage(), null);
            return loadJob(historyId);
        }

        try {
            UpsertResult upsert = upsertArticles(lawId, parsed.articles);
            inserted += upsert.inserted;
            updated += upsert.updated;
        } catch (Exception e) {
            log.error("[Import {}] 条款写入失败: {}", taskUuid, e.getMessage(), e);
        }

        try {
            List<ElasticsearchService.LawArticleDocument> esDocs = buildEsDocs(lawId, parsed);
            int esCount = elasticsearchService.bulkIndexArticles(esDocs);
            esOk = esCount > 0;
        } catch (Exception e) {
            log.error("[Import {}] ES 写入失败: {}", taskUuid, e.getMessage(), e);
        }

        try {
            milvusOk = indexToMilvus(lawId, parsed);
        } catch (Exception e) {
            log.error("[Import {}] Milvus 写入失败: {}", taskUuid, e.getMessage(), e);
        }

        finishHistory(historyId, "success", total, inserted, updated, mysqlOk, esOk, milvusOk, null, null);
        return loadJob(historyId);
    }

    public LawImportJob loadJob(long historyId) {
        try {
            List<LawImportJob> jobs = jdbcTemplate.query(SELECT_HISTORY_BY_ID,
                    (rs, rn) -> mapHistory(rs), historyId);
            return jobs.isEmpty() ? null : jobs.get(0);
        } catch (Exception e) {
            log.error("加载导入历史失败: {}", e.getMessage(), e);
            return null;
        }
    }

    public List<LawImportJob> listHistory(int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        return jdbcTemplate.query(SELECT_HISTORY_PAGE,
                (rs, rn) -> mapHistory(rs), pageSize, offset);
    }

    public long countHistory() {
        Long c = jdbcTemplate.queryForObject(COUNT_HISTORY, Long.class);
        return c != null ? c : 0L;
    }

    public Map<String, Long> stats() {
        Map<String, Long> map = new HashMap<>();
        map.put("lawCount", jdbcTemplate.queryForObject(SELECT_LAW_COUNT, Long.class));
        map.put("articleCount", jdbcTemplate.queryForObject(SELECT_ARTICLE_COUNT, Long.class));
        map.put("historyCount", countHistory());
        return map;
    }

    public List<String> listPresets() {
        List<String> presets = new ArrayList<>();
        try {
            org.springframework.core.io.Resource res = new ClassPathResource("seed-data/laws/");
            if (res.exists() && res.isReadable()) {
                java.io.File dir = res.getFile();
                java.io.File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
                if (files != null) {
                    for (java.io.File f : files) {
                        String name = f.getName();
                        presets.add(name.substring(0, name.length() - 5));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("枚举预置数据失败: {}", e.getMessage());
        }
        Collections.sort(presets);
        return presets;
    }

    private long createHistory(String taskUuid, String lawName, String source, String operator) {
        jdbcTemplate.update(INSERT_HISTORY_SQL,
                taskUuid, lawName, source, "running", 0, 0, 0, 0, 0, 0, null, operator, LocalDateTime.now(), null);
        return queryLastInsertId();
    }

    private void finishHistory(long id, String status, int total, int inserted, int updated,
                               boolean mysqlOk, boolean esOk, boolean milvusOk,
                               String error, String articlesJson) {
        jdbcTemplate.update(UPDATE_HISTORY_SQL, status, total, inserted, updated,
                mysqlOk ? 1 : 0, esOk ? 1 : 0, milvusOk ? 1 : 0, error, id);
    }

    private long queryLastInsertId() {
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return id != null ? id : 0L;
    }

    private Long findLawIdByTitle(String title) {
        try {
            List<Long> ids = jdbcTemplate.query(SELECT_LAW_BY_TITLE, (rs, rn) -> rs.getLong("id"), title);
            return ids.isEmpty() ? null : ids.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    private long insertLaw(ParsedLaw parsed) throws Exception {
        String lawUuid = "LAW-" + System.currentTimeMillis() + "-" + Math.abs(parsed.lawTitle.hashCode() % 10000);
        String shortTitle = parsed.lawTitle;
        if (parsed.lawTitle != null && parsed.lawTitle.length() > 50) {
            shortTitle = parsed.lawTitle.substring(0, 50);
        }
        LocalDate issueDate = parseDateOrNull(parsed.issueDate);
        LocalDate effectiveDate = parseDateOrNull(parsed.effectiveDate);
        Integer status = parseStatus(parsed.status);

        jdbcTemplate.update(INSERT_LAW_SQL,
                lawUuid,
                parsed.lawTitle,
                shortTitle,
                parsed.categoryL1 != null ? parsed.categoryL1 : "法律",
                parsed.categoryL2,
                parsed.issuingAuthority,
                issueDate,
                effectiveDate,
                status,
                parsed.sourceUrl,
                parsed.sourceName != null ? parsed.sourceName : "AI 联网搜索");

        return queryLastInsertId();
    }

    private void updateLaw(long lawId, ParsedLaw parsed) throws Exception {
        LocalDate issueDate = parseDateOrNull(parsed.issueDate);
        LocalDate effectiveDate = parseDateOrNull(parsed.effectiveDate);
        Integer status = parseStatus(parsed.status);
        jdbcTemplate.update(UPDATE_LAW_SQL,
                parsed.lawTitle,
                parsed.lawTitle != null && parsed.lawTitle.length() > 50 ? parsed.lawTitle.substring(0, 50) : parsed.lawTitle,
                parsed.categoryL1 != null ? parsed.categoryL1 : "法律",
                parsed.categoryL2,
                parsed.issuingAuthority,
                issueDate,
                effectiveDate,
                status,
                parsed.sourceUrl,
                parsed.sourceName != null ? parsed.sourceName : "AI 联网搜索",
                lawId);
    }

    private UpsertResult upsertArticles(long lawId, List<ParsedArticle> articles) {
        int inserted = 0;
        int updated = 0;
        int order = 0;
        for (ParsedArticle a : articles) {
            if (a.articleNo == null || a.articleNo.isBlank()) {
                continue;
            }
            order++;
            String hash = sha256(a.content == null ? "" : a.content);
            String existingHash = null;
            try {
                List<String> hashes = jdbcTemplate.query(SELECT_ARTICLE_HASH,
                        (rs, rn) -> rs.getString(1), lawId, a.articleNo);
                if (!hashes.isEmpty()) {
                    existingHash = hashes.get(0);
                }
            } catch (Exception ignored) {}

            if (existingHash == null) {
                String articleUuid = "ART-" + System.currentTimeMillis() + "-" + order;
                jdbcTemplate.update(INSERT_ARTICLE_SQL,
                        lawId, articleUuid, a.articleNo, a.title, a.content, hash, order);
                inserted++;
            } else if (!hash.equals(existingHash)) {
                List<Long> ids = jdbcTemplate.query(SELECT_ARTICLE_ID,
                        (rs, rn) -> rs.getLong(1), lawId, a.articleNo);
                if (!ids.isEmpty()) {
                    jdbcTemplate.update(UPDATE_ARTICLE_SQL,
                            a.title, a.content, hash, order, ids.get(0));
                    updated++;
                }
            }
        }
        return new UpsertResult(inserted, updated);
    }

    private List<ElasticsearchService.LawArticleDocument> buildEsDocs(long lawId, ParsedLaw parsed) {
        List<ElasticsearchService.LawArticleDocument> docs = new ArrayList<>();
        String lawIdStr = String.valueOf(lawId);
        for (ParsedArticle a : parsed.articles) {
            if (a.articleNo == null || a.articleNo.isBlank()) continue;
            String articleId = "LAW" + lawId + "-" + a.articleNo.hashCode() + "-" + Math.abs((a.articleNo + lawIdStr).hashCode() % 10000);
            docs.add(new ElasticsearchService.LawArticleDocument(
                    articleId, lawIdStr, parsed.lawTitle, a.articleNo, a.title, a.content,
                    parsed.categoryL1, parsed.categoryL2, parsed.sourceUrl,
                    parsed.sourceName != null ? parsed.sourceName : "AI 联网搜索"
            ));
        }
        return docs;
    }

    private boolean indexToMilvus(long lawId, ParsedLaw parsed) {
        if (mockEnabled) {
            log.info("[mock] 跳过 Milvus 写入");
            return false;
        }
        List<MilvusService.IndexableArticle> batch = new ArrayList<>();
        int batchSize = 10;
        for (ParsedArticle a : parsed.articles) {
            if (a.content == null || a.content.isBlank()) continue;
            try {
                float[] vector = aiService.embedText(a.content);
                String articleId = "LAW" + lawId + "-" + a.articleNo;
                batch.add(new MilvusService.IndexableArticle(articleId, vector, a.content));
                if (batch.size() >= batchSize) {
                    milvusService.indexArticles(batch);
                    batch.clear();
                }
            } catch (Exception e) {
                log.warn("Milvus 向量化失败: article={}, {}", a.articleNo, e.getMessage());
            }
        }
        if (!batch.isEmpty()) {
            milvusService.indexArticles(batch);
        }
        return true;
    }

    private ParsedLaw fetchByWebSearch(String lawName) throws IOException {
        log.info("调用 LLM 联网搜索导入法律: {}", lawName);

        String searchPrompt = String.format(
                "请联网搜索《中华人民共和国%s》的官方完整条文，包括发布机关、生效日期、当前状态、全部条款内容。" +
                "输出必须是严格的 JSON，结构如下：\n" +
                "{\n" +
                "  \"lawTitle\": \"完整法律名称\",\n" +
                "  \"issuingAuthority\": \"发布机关\",\n" +
                "  \"issueDate\": \"YYYY-MM-DD 或留空\",\n" +
                "  \"effectiveDate\": \"YYYY-MM-DD 或留空\",\n" +
                "  \"status\": \"现行/废止/修订中\",\n" +
                "  \"categoryL1\": \"法律/行政法规/司法解释/部门规章/地方性法规\",\n" +
                "  \"categoryL2\": \"二级分类（民商法/刑法/行政法/经济法/社会法/诉讼与非诉讼程序法 等）\",\n" +
                "  \"sourceUrl\": \"来源URL\",\n" +
                "  \"sourceName\": \"来源名称（如国家法律法规信息库）\",\n" +
                "  \"articles\": [\n" +
                "    {\"articleNo\": \"第一条\", \"title\": \"条款标题（可空）\", \"content\": \"条款正文\"}\n" +
                "  ]\n" +
                "}\n" +
                "要求：articles 必须按条文顺序排列，content 保留原文；仅返回 JSON，无任何解释文字。", lawName);

        String response = llmClient.searchAndStructure(searchPrompt,
                "请将上述搜索结果整理为合法的 JSON，注意 content 字段必须是字符串，articles 数组不为空。");

        return parseStructuredJson(lawName, response);
    }

    private ParsedLaw parseUploadedJson(String lawName, String jsonContent) {
        try {
            return parseStructuredJson(lawName, jsonContent);
        } catch (IOException e) {
            throw new RuntimeException("JSON 解析失败: " + e.getMessage(), e);
        }
    }

    private ParsedLaw parseStructuredJson(String defaultName, String content) throws IOException {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("输入内容为空");
        }
        String json = content.trim();
        int start = json.indexOf('{');
        int end = json.lastIndexOf('}');
        if (start >= 0 && end > start) {
            json = json.substring(start, end + 1);
        }
        json = json.replaceAll("```json\\s*", "").replaceAll("```\\s*", "");

        JsonNode root = objectMapper.readTree(json);
        ParsedLaw law = new ParsedLaw();
        law.lawTitle = textOr(root, "lawTitle", defaultName);
        law.issuingAuthority = textOr(root, "issuingAuthority", null);
        law.issueDate = textOr(root, "issueDate", null);
        law.effectiveDate = textOr(root, "effectiveDate", null);
        law.status = textOr(root, "status", "现行");
        law.categoryL1 = textOr(root, "categoryL1", "法律");
        law.categoryL2 = textOr(root, "categoryL2", null);
        law.sourceUrl = textOr(root, "sourceUrl", null);
        law.sourceName = textOr(root, "sourceName", "用户上传");

        law.articles = new ArrayList<>();
        JsonNode arts = root.get("articles");
        if (arts != null && arts.isArray()) {
            for (JsonNode a : arts) {
                ParsedArticle pa = new ParsedArticle();
                pa.articleNo = textOr(a, "articleNo", null);
                pa.title = textOr(a, "title", null);
                pa.content = textOr(a, "content", null);
                if (pa.content != null && !pa.content.isBlank()) {
                    law.articles.add(pa);
                }
            }
        }
        return law;
    }

    private static String textOr(JsonNode node, String field, String defaultValue) {
        if (node == null) return defaultValue;
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) return defaultValue;
        return v.asText();
    }

    private LawImportJob mapHistory(java.sql.ResultSet rs) throws java.sql.SQLException {
        LawImportJob j = new LawImportJob();
        j.setId(rs.getLong("id"));
        j.setTaskUuid(rs.getString("task_uuid"));
        j.setLawName(rs.getString("law_name"));
        j.setSource(rs.getString("source"));
        j.setStatus(rs.getString("status"));
        j.setTotalArticles((int) rs.getLong("total_articles"));
        j.setInsertedArticles((int) rs.getLong("inserted_articles"));
        j.setUpdatedArticles((int) rs.getLong("updated_articles"));
        j.setMysqlOk(rs.getInt("mysql_ok") == 1);
        j.setEsOk(rs.getInt("es_ok") == 1);
        j.setMilvusOk(rs.getInt("milvus_ok") == 1);
        j.setErrorMessage(rs.getString("error_message"));
        j.setOperator(rs.getString("operator"));
        java.sql.Timestamp started = rs.getTimestamp("started_at");
        j.setStartedAt(started != null ? started.toLocalDateTime() : null);
        java.sql.Timestamp finished = rs.getTimestamp("finished_at");
        j.setFinishedAt(finished != null ? finished.toLocalDateTime() : null);
        return j;
    }

    private LocalDate parseDateOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDate.parse(s.trim().substring(0, 10));
        } catch (Exception e) {
            return null;
        }
    }

    private Integer parseStatus(String s) {
        if (s == null) return 1;
        if (s.contains("废止")) return 2;
        if (s.contains("修订") || s.contains("修改")) return 3;
        return 1;
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }

    private static class ParsedLaw {
        String lawTitle;
        String issuingAuthority;
        String issueDate;
        String effectiveDate;
        String status;
        String categoryL1;
        String categoryL2;
        String sourceUrl;
        String sourceName;
        List<ParsedArticle> articles;
    }

    private static class ParsedArticle {
        String articleNo;
        String title;
        String content;
    }

    private record UpsertResult(int inserted, int updated) {}
}
