package com.legalai.service;

import com.legalai.config.MilvusConfig;
import com.legalai.dto.KnowledgeBaseListResponse;
import com.legalai.dto.LegalSearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class KnowledgeBaseService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseService.class);

    private static final int CHUNK_SIZE = 512;
    private static final int CHUNK_OVERLAP = 64;

    @Value("${mock.enabled:true}")
    private boolean mockEnabled;

    private final AtomicLong idGenerator = new AtomicLong(5);
    private final AtomicLong chunkIdGenerator = new AtomicLong(1);
    private final ConcurrentHashMap<Long, KnowledgeBaseListResponse.KnowledgeBase> kbStore = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, List<DocumentChunk>> chunkStore = new ConcurrentHashMap<>();

    @Autowired
    private MilvusService milvusService;

    @Autowired
    private MilvusConfig milvusConfig;

    @Autowired
    private AIService aiService;

    public KnowledgeBaseService() {
        initMockData();
    }

    private void initMockData() {
        String today = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE);

        addKb(createKb(1L, "劳动法法规库", "劳动法律法规及相关案例汇总", "public", true, 156, 2340, "128MB", "系统管理员", today, 2));
        addKb(createKb(2L, "合同纠纷案例", "各类合同纠纷案例集", "private", false, 89, 1560, "96MB", "张三", today, 2));
        addKb(createKb(3L, "知识产权法规", "知识产权相关法律法规", "public", true, 234, 3200, "156MB", "系统管理员", today, 2));
        addKb(createKb(4L, "公司法务文档", "公司内部法务文档", "private", false, 45, 680, "45MB", "李四", today, 1));
    }

    private void addKb(KnowledgeBaseListResponse.KnowledgeBase kb) {
        kbStore.put(kb.getId(), kb);
    }

    private KnowledgeBaseListResponse.KnowledgeBase createKb(Long id, String name, String desc, String type, boolean isPublic,
                                                               int docCount, int chunkCount, String size, String owner, String updateTime, int parseStatus) {
        KnowledgeBaseListResponse.KnowledgeBase kb = new KnowledgeBaseListResponse.KnowledgeBase();
        kb.setId(id);
        kb.setName(name);
        kb.setDescription(desc);
        kb.setType(type);
        kb.setIsPublic(isPublic);
        kb.setDocCount(docCount);
        kb.setChunkCount(chunkCount);
        kb.setSize(size);
        kb.setOwner(owner);
        kb.setUpdateTime(updateTime);
        kb.setParseStatus(parseStatus);
        return kb;
    }

    public KnowledgeBaseListResponse listKnowledgeBases(String keyword, int page, int pageSize) {
        log.info("Listing knowledge bases: keyword={}, page={}, pageSize={}", keyword, page, pageSize);

        List<KnowledgeBaseListResponse.KnowledgeBase> allKbs = new ArrayList<>(kbStore.values());

        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.toLowerCase();
            allKbs = allKbs.stream()
                .filter(kb -> kb.getName().toLowerCase().contains(kw) ||
                             (kb.getDescription() != null && kb.getDescription().toLowerCase().contains(kw)))
                .toList();
        }

        int total = allKbs.size();
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, total);

        List<KnowledgeBaseListResponse.KnowledgeBase> pageItems = start < total
            ? allKbs.subList(start, end)
            : new ArrayList<>();

        KnowledgeBaseListResponse response = new KnowledgeBaseListResponse();
        response.setItems(pageItems);
        response.setTotal(total);
        response.setPage(page);
        response.setPageSize(pageSize);
        return response;
    }

    public KnowledgeBaseListResponse.KnowledgeBase createKnowledgeBase(String name, String description, boolean isPublic) {
        log.info("Creating knowledge base: name={}, isPublic={}", name, isPublic);

        long id = idGenerator.getAndIncrement();
        String today = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE);

        KnowledgeBaseListResponse.KnowledgeBase kb = createKb(
            id, name, description, isPublic ? "public" : "private",
            isPublic, 0, 0, "0MB",
            "当前用户", today, 0
        );
        kb.setIsPublic(isPublic);

        addKb(kb);
        chunkStore.put(id, new ArrayList<>());
        return kb;
    }

    public boolean deleteKnowledgeBase(Long id) {
        log.info("Deleting knowledge base: id={}", id);
        chunkStore.remove(id);
        return kbStore.remove(id) != null;
    }

    public String uploadDocument(Long kbId, String fileName, String content) {
        log.info("Uploading document to kb: kbId={}, fileName={}", kbId, fileName);

        KnowledgeBaseListResponse.KnowledgeBase kb = kbStore.get(kbId);
        if (kb == null) {
            return "知识库不存在";
        }

        List<DocumentChunk> chunks = semanticChunking(content, fileName);

        for (DocumentChunk chunk : chunks) {
            if (mockEnabled) {
                chunk.setChunkId(chunkIdGenerator.getAndIncrement());
            } else {
                vectorizeChunk(chunk);
            }
        }

        List<DocumentChunk> existingChunks = chunkStore.getOrDefault(kbId, new ArrayList<>());
        existingChunks.addAll(chunks);
        chunkStore.put(kbId, existingChunks);

        kb.setDocCount(kb.getDocCount() + 1);
        kb.setChunkCount(existingChunks.size());
        kb.setSize(String.format("%.0fMB", existingChunks.size() * 0.01));
        kb.setUpdateTime(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE));
        kb.setParseStatus(2);

        return String.format("文档上传成功，生成%d个语义块", chunks.size());
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
                DocumentChunk chunk = new DocumentChunk();
                chunk.setContent(currentChunk.toString().trim());
                chunk.setFileName(fileName);
                chunk.setChunkIndex(chunks.size());
                chunk.setTokenCount(currentSize);
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
            DocumentChunk chunk = new DocumentChunk();
            chunk.setContent(currentChunk.toString().trim());
            chunk.setFileName(fileName);
            chunk.setChunkIndex(chunks.size());
            chunk.setTokenCount(currentSize);
            chunks.add(chunk);
        }

        return chunks;
    }

    private void vectorizeChunk(DocumentChunk chunk) {
        try {
            float[] vector = aiService.embedText(chunk.getContent());
            chunk.setVector(vector);
            chunk.setVectorId("VEC-" + chunkIdGenerator.getAndIncrement());
            log.debug("向量化语义块: {}, 向量维度={}", chunk.getVectorId(), vector.length);
        } catch (Exception e) {
            log.error("向量化失败: {}", e.getMessage());
        }
    }

    public List<LegalSearchResponse.SearchResultItem> searchInKnowledgeBase(Long kbId, String query, int topK) {
        log.info("知识库内搜索: kbId={}, query={}, topK={}", kbId, query, topK);

        List<DocumentChunk> chunks = chunkStore.get(kbId);
        if (chunks == null || chunks.isEmpty()) {
            return Collections.emptyList();
        }

        if (!mockEnabled && chunks.stream().anyMatch(c -> c.getVector() != null)) {
            return semanticSearch(chunks, query, topK);
        }

        return textSearchChunks(chunks, query, topK);
    }

    public KnowledgeBaseListResponse.KnowledgeBase getKnowledgeBase(Long id) {
        return id == null ? null : kbStore.get(id);
    }

    public List<DocumentChunk> getDocumentChunks(Long kbId) {
        return kbId == null ? Collections.emptyList() :
            chunkStore.getOrDefault(kbId, Collections.emptyList());
    }

    public List<LegalSearchResponse.SearchResultItem> searchInKnowledgeBase(String kbId, String query, int topK) {
        log.info("知识库内搜索: kbId={}, query={}", kbId, query);

        Long numericKbId = parseKbId(kbId);
        if (numericKbId == null) {
            log.warn("无效的知识库ID: {}", kbId);
            return Collections.emptyList();
        }

        return searchInKnowledgeBase(numericKbId, query, topK);
    }

    private Long parseKbId(String kbId) {
        if (kbId == null || kbId.isBlank()) {
            return null;
        }
        if (kbId.matches("\\d+")) {
            return Long.parseLong(kbId);
        }
        if (kbId.matches("KB-\\d+")) {
            return Long.parseLong(kbId.substring(3));
        }
        for (Map.Entry<Long, KnowledgeBaseListResponse.KnowledgeBase> entry : kbStore.entrySet()) {
            if (entry.getValue().getName().contains(kbId) || entry.getValue().getName().equals(kbId)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private List<LegalSearchResponse.SearchResultItem> textSearchChunks(List<DocumentChunk> chunks, String query, int topK) {
        String lowerQuery = query.toLowerCase();

        List<ScoredChunk> scoredChunks = new ArrayList<>();
        for (DocumentChunk chunk : chunks) {
            String content = chunk.getContent().toLowerCase();
            int matchCount = 0;
            for (String word : lowerQuery.split("\\s+")) {
                if (word.length() > 1 && content.contains(word)) {
                    matchCount++;
                }
            }
            float score = matchCount > 0 ? (float) matchCount / lowerQuery.split("\\s+").length : 0f;
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
            .collect(Collectors.toList());
    }

    private List<LegalSearchResponse.SearchResultItem> semanticSearch(List<DocumentChunk> chunks, String query, int topK) {
        try {
            float[] queryVector = aiService.embedText(query);
            log.info("开始语义搜索: query长度={}, chunk数量={}", query.length(), chunks.size());

            List<ScoredChunk> scoredChunks = new ArrayList<>();
            for (DocumentChunk chunk : chunks) {
                if (chunk.getVector() != null) {
                    float similarity = cosineSimilarity(queryVector, chunk.getVector());
                    scoredChunks.add(new ScoredChunk(chunk, similarity));
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
                    item.setScore((double) sc.score);
                    return item;
                })
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("语义搜索失败: {}", e.getMessage());
            return chunks.stream()
                .limit(topK)
                .map(chunk -> {
                    LegalSearchResponse.SearchResultItem item = new LegalSearchResponse.SearchResultItem();
                    item.setArticleId(String.valueOf(chunk.getChunkId()));
                    item.setTitle(chunk.getFileName());
                    item.setContent(chunk.getContent());
                    item.setScore(0.85);
                    return item;
                })
                .collect(Collectors.toList());
        }
    }

    private float cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) return 0f;

        float dotProduct = 0f;
        float normA = 0f;
        float normB = 0f;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0f || normB == 0f) return 0f;
        return dotProduct / (float) (Math.sqrt(normA) * Math.sqrt(normB));
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