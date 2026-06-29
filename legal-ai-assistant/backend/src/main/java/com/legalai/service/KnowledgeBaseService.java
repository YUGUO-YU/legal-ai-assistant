package com.legalai.service;

import com.legalai.dto.KnowledgeBaseListResponse;
import com.legalai.dto.LegalSearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class KnowledgeBaseService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseService.class);

    private static final int CHUNK_SIZE = 512;
    private static final int CHUNK_OVERLAP = 64;

    @Value("${mock.enabled:true}")
    private boolean mockEnabled;

    private final JdbcTemplate jdbc;
    private final Map<Long, List<DocumentChunk>> chunkCache = new LinkedHashMap<Long, List<DocumentChunk>>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, List<DocumentChunk>> eldest) {
            return size() > 50;
        }
    };

    @Autowired
    private AIService aiService;

    public KnowledgeBaseService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public KnowledgeBaseListResponse listKnowledgeBases(String keyword, int page, int pageSize) {
        log.info("Listing knowledge bases: keyword={}, page={}, pageSize={}", keyword, page, pageSize);

        StringBuilder where = new StringBuilder("WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            where.append("AND (name LIKE ? OR description LIKE ?) ");
            String like = "%" + keyword + "%";
            args.add(like);
            args.add(like);
        }

        String countSql = "SELECT COUNT(*) FROM kb_knowledge_base " + where;
        Integer total = jdbc.queryForObject(countSql, Integer.class, args.toArray());

        int offset = (page - 1) * pageSize;
        String dataSql = "SELECT id, kb_uuid, name, description, owner_id, is_public, doc_count, created_at, updated_at FROM kb_knowledge_base " + where + " ORDER BY updated_at DESC LIMIT ? OFFSET ?";
        args.add(pageSize);
        args.add(offset);

        List<Map<String, Object>> rows = jdbc.queryForList(dataSql, args.toArray());
        List<KnowledgeBaseListResponse.KnowledgeBase> items = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            KnowledgeBaseListResponse.KnowledgeBase kb = mapRowToKb(row);
            items.add(kb);
        }

        KnowledgeBaseListResponse response = new KnowledgeBaseListResponse();
        response.setItems(items);
        response.setTotal(total != null ? total : 0);
        response.setPage(page);
        response.setPageSize(pageSize);
        return response;
    }

    public KnowledgeBaseListResponse.KnowledgeBase createKnowledgeBase(String name, String description, boolean isPublic) {
        log.info("Creating knowledge base: name={}, isPublic={}", name, isPublic);

        String kbUuid = "KB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String ownerId = "default";
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        jdbc.update(
            "INSERT INTO kb_knowledge_base (kb_uuid, name, description, owner_id, is_public, doc_count, created_at, updated_at) VALUES (?, ?, ?, ?, ?, 0, ?, ?)",
            kbUuid, name, description != null ? description : "", ownerId, isPublic ? 1 : 0, now, now
        );

        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        KnowledgeBaseListResponse.KnowledgeBase kb = new KnowledgeBaseListResponse.KnowledgeBase();
        kb.setId(id);
        kb.setName(name);
        kb.setDescription(description != null ? description : "");
        kb.setType(isPublic ? "public" : "private");
        kb.setIsPublic(isPublic);
        kb.setDocCount(0);
        kb.setChunkCount(0);
        kb.setSize("0MB");
        kb.setOwner("当前用户");
        kb.setUpdateTime(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE));
        kb.setParseStatus(0);
        return kb;
    }

    public boolean deleteKnowledgeBase(Long id) {
        log.info("Deleting knowledge base: id={}", id);
        chunkCache.remove(id);
        jdbc.update("DELETE FROM kb_chunk_store WHERE kb_id = ?", id);
        jdbc.update("DELETE FROM kb_document WHERE kb_id = ?", id);
        int affected = jdbc.update("DELETE FROM kb_knowledge_base WHERE id = ?", id);
        return affected > 0;
    }

    public String uploadDocument(Long kbId, String fileName, String content) {
        log.info("Uploading document to kb: kbId={}, fileName={}", kbId, fileName);

        KnowledgeBaseListResponse.KnowledgeBase kb = getKnowledgeBase(kbId);
        if (kb == null) {
            return "知识库不存在";
        }

        String docUuid = "DOC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        List<DocumentChunk> chunks = semanticChunking(content, fileName);
        int chunkCount = chunks.size();

        jdbc.update(
            "INSERT INTO kb_document (kb_id, doc_uuid, file_name, file_type, file_size, file_path, chunk_count, parse_status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, 2, ?)",
            kbId, docUuid, fileName, detectFileType(fileName), (long) content.getBytes().length, "/kb/" + kbId + "/" + docUuid, chunkCount, now
        );

        chunkCache.put(kbId, chunks);

        try {
            jdbc.update("DELETE FROM kb_chunk_store WHERE kb_id = ? AND file_name = ?", kbId, fileName);
        } catch (Exception ignored) {
        }

        jdbc.update(
            "UPDATE kb_knowledge_base SET doc_count = doc_count + 1, updated_at = ? WHERE id = ?",
            now, kbId
        );

        return String.format("文档上传成功，生成%d个语义块", chunkCount);
    }

    public List<DocumentChunk> semanticChunking(String content, String fileName) {
        List<DocumentChunk> chunks = new ArrayList<>();
        String[] paragraphs = content.split("\n\n");
        StringBuilder currentChunk = new StringBuilder();
        int currentSize = 0;

        for (String paragraph : paragraphs) {
            if (paragraph.trim().isEmpty()) continue;
            int paragraphSize = paragraph.length();
            if (currentSize + paragraphSize > CHUNK_SIZE && currentSize > 0) {
                DocumentChunk chunk = buildChunk(currentChunk.toString().trim(), fileName, chunks.size(), currentSize);
                chunks.add(chunk);
                String overlapText = currentChunk.toString();
                int overlapStart = Math.max(0, overlapText.length() - CHUNK_OVERLAP);
                currentChunk = new StringBuilder(overlapText.substring(overlapStart));
                currentSize = currentChunk.length();
            }
            currentChunk.append(paragraph).append("\n\n");
            currentSize += paragraphSize + 2;
        }

        if (currentChunk.length() > 0) {
            chunks.add(buildChunk(currentChunk.toString().trim(), fileName, chunks.size(), currentSize));
        }

        return chunks;
    }

    private DocumentChunk buildChunk(String content, String fileName, int index, int tokenCount) {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setContent(content);
        chunk.setFileName(fileName);
        chunk.setChunkIndex(index);
        chunk.setTokenCount(tokenCount);
        return chunk;
    }

    public List<LegalSearchResponse.SearchResultItem> searchInKnowledgeBase(Long kbId, String query, int topK) {
        log.info("知识库内搜索: kbId={}, query={}, topK={}", kbId, query, topK);

        List<DocumentChunk> chunks = chunkCache.get(kbId);
        if (chunks == null || chunks.isEmpty()) {
            return loadChunksFromDb(kbId, query, topK);
        }

        return textSearchChunks(chunks, query, topK);
    }

    private List<LegalSearchResponse.SearchResultItem> loadChunksFromDb(Long kbId, String query, int topK) {
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT file_name, LEFT(content, 1000) AS content, chunk_index FROM kb_chunk_store WHERE kb_id = ? ORDER BY chunk_index",
                kbId
            );
            if (rows.isEmpty()) {
                return Collections.emptyList();
            }
            List<DocumentChunk> chunks = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                DocumentChunk chunk = new DocumentChunk();
                chunk.setFileName((String) row.get("file_name"));
                chunk.setContent((String) row.get("content"));
                chunk.setChunkIndex(row.get("chunk_index") != null ? ((Number) row.get("chunk_index")).intValue() : 0);
                chunks.add(chunk);
            }
            return textSearchChunks(chunks, query, topK);
        } catch (Exception e) {
            log.warn("从DB加载知识库分块失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public KnowledgeBaseListResponse.KnowledgeBase getKnowledgeBase(Long id) {
        if (id == null) return null;
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, kb_uuid, name, description, owner_id, is_public, doc_count, created_at, updated_at FROM kb_knowledge_base WHERE id = ?", id
            );
            if (rows.isEmpty()) return null;
            return mapRowToKb(rows.get(0));
        } catch (Exception e) {
            log.warn("获取知识库失败: id={}, error={}", id, e.getMessage());
            return null;
        }
    }

    public List<DocumentChunk> getDocumentChunks(Long kbId) {
        if (kbId == null) return Collections.emptyList();
        List<DocumentChunk> cached = chunkCache.get(kbId);
        if (cached != null && !cached.isEmpty()) return cached;

        try {
            List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id as chunk_id, content, file_name, chunk_index, token_count FROM kb_chunk_store WHERE kb_id = ? ORDER BY chunk_index", kbId
            );
            List<DocumentChunk> chunks = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                DocumentChunk chunk = new DocumentChunk();
                chunk.setChunkId(((Number) row.get("chunk_id")).longValue());
                chunk.setContent((String) row.get("content"));
                chunk.setFileName((String) row.get("file_name"));
                chunk.setChunkIndex(row.get("chunk_index") != null ? ((Number) row.get("chunk_index")).intValue() : 0);
                chunk.setTokenCount(row.get("token_count") != null ? ((Number) row.get("token_count")).intValue() : 0);
                chunks.add(chunk);
            }
            return chunks;
        } catch (Exception e) {
            log.warn("从DB获取文档分块失败: kbId={}, error={}", kbId, e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<LegalSearchResponse.SearchResultItem> searchInKnowledgeBase(String kbId, String query, int topK) {
        Long numericKbId = parseKbId(kbId);
        if (numericKbId == null) {
            log.warn("无效的知识库ID: {}", kbId);
            return Collections.emptyList();
        }
        return searchInKnowledgeBase(numericKbId, query, topK);
    }

    private Long parseKbId(String kbId) {
        if (kbId == null || kbId.isBlank()) return null;
        if (kbId.matches("\\d+")) return Long.parseLong(kbId);
        if (kbId.matches("KB-\\d+")) return Long.parseLong(kbId.substring(3));
        try {
            Long id = jdbc.queryForObject("SELECT id FROM kb_knowledge_base WHERE name = ? OR kb_uuid = ? LIMIT 1", Long.class, kbId, kbId);
            return id;
        } catch (Exception e) {
            return null;
        }
    }

    private List<LegalSearchResponse.SearchResultItem> textSearchChunks(List<DocumentChunk> chunks, String query, int topK) {
        String lowerQuery = query.toLowerCase();
        String[] queryWords = lowerQuery.split("\\s+");

        List<ScoredChunk> scoredChunks = new ArrayList<>();
        for (DocumentChunk chunk : chunks) {
            String content = chunk.getContent().toLowerCase();
            int matchCount = 0;
            for (String word : queryWords) {
                if (word.length() > 1 && content.contains(word)) {
                    matchCount++;
                }
            }
            float score = matchCount > 0 ? (float) matchCount / queryWords.length : 0f;
            if (score > 0) {
                scoredChunks.add(new ScoredChunk(chunk, score));
            }
        }

        scoredChunks.sort((a, b) -> Float.compare(b.score, a.score));

        return scoredChunks.stream()
            .limit(topK)
            .map(sc -> {
                LegalSearchResponse.SearchResultItem item = new LegalSearchResponse.SearchResultItem();
                item.setArticleId(String.valueOf(sc.chunk.getChunkId()));
                item.setTitle(sc.chunk.getFileName());
                item.setContent(sc.chunk.getContent());
                item.setScore(Math.min(0.99, 0.7 + sc.score * 0.25));
                return item;
            })
            .collect(java.util.stream.Collectors.toList());
    }

    private KnowledgeBaseListResponse.KnowledgeBase mapRowToKb(Map<String, Object> row) {
        KnowledgeBaseListResponse.KnowledgeBase kb = new KnowledgeBaseListResponse.KnowledgeBase();
        kb.setId(((Number) row.get("id")).longValue());
        kb.setName((String) row.get("name"));
        kb.setDescription((String) row.get("description"));
        Boolean isPublic = row.get("is_public") instanceof Boolean
            ? (Boolean) row.get("is_public")
            : Integer.valueOf(1).equals(((Number) row.get("is_public")).intValue());
        kb.setIsPublic(isPublic);
        kb.setType(isPublic ? "public" : "private");
        kb.setOwner((String) row.getOrDefault("owner_id", "default"));
        kb.setDocCount(row.get("doc_count") != null ? ((Number) row.get("doc_count")).intValue() : 0);

        Object updatedAt = row.get("updated_at");
        if (updatedAt instanceof java.sql.Timestamp) {
            kb.setUpdateTime(((java.sql.Timestamp) updatedAt).toLocalDateTime().format(DateTimeFormatter.ISO_DATE));
        } else if (updatedAt instanceof LocalDateTime) {
            kb.setUpdateTime(((LocalDateTime) updatedAt).format(DateTimeFormatter.ISO_DATE));
        } else if (updatedAt != null) {
            kb.setUpdateTime(updatedAt.toString().substring(0, 10));
        } else {
            kb.setUpdateTime(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE));
        }

        kb.setChunkCount(0);
        kb.setSize("0MB");
        kb.setParseStatus(0);
        return kb;
    }

    private String detectFileType(String fileName) {
        if (fileName == null) return "txt";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf")) return "pdf";
        if (lower.endsWith(".docx")) return "docx";
        if (lower.endsWith(".doc")) return "doc";
        if (lower.endsWith(".txt")) return "txt";
        if (lower.endsWith(".md")) return "md";
        return "txt";
    }

    private static class ScoredChunk {
        DocumentChunk chunk;
        float score;

        ScoredChunk(DocumentChunk chunk, float score) {
            this.chunk = chunk;
            this.score = score;
        }
    }

    public static class DocumentChunk {
        private Long chunkId;
        private String content;
        private String fileName;
        private int chunkIndex;
        private int tokenCount;
        private String vectorId;
        private float[] vector;

        public Long getChunkId() { return chunkId; }
        public void setChunkId(Long chunkId) { this.chunkId = chunkId; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public int getChunkIndex() { return chunkIndex; }
        public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }
        public int getTokenCount() { return tokenCount; }
        public void setTokenCount(int tokenCount) { this.tokenCount = tokenCount; }
        public String getVectorId() { return vectorId; }
        public void setVectorId(String vectorId) { this.vectorId = vectorId; }
        public float[] getVector() { return vector; }
        public void setVector(float[] vector) { this.vector = vector; }
    }
}
