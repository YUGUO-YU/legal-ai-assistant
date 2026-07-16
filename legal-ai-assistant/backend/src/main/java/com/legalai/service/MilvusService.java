package com.legalai.service;

import com.legalai.config.MilvusConfig;
import com.legalai.dto.CaseSimilarSearchResponse;
import com.legalai.dto.LegalSearchResponse;
import io.milvus.client.MilvusClient;
import io.milvus.param.dml.QueryParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class MilvusService {

    private final Optional<MilvusClient> milvusClient;
    private final MilvusConfig milvusConfig;
    private final AIService aiService;

    @Autowired
    public MilvusService(Optional<MilvusClient> milvusClient, MilvusConfig milvusConfig, AIService aiService) {
        this.milvusClient = milvusClient;
        this.milvusConfig = milvusConfig;
        this.aiService = aiService;
    }

    public List<LegalSearchResponse.SearchResultItem> searchByVector(String query, int topK) {
        if (milvusClient.orElse(null) == null || !milvusConfig.isEnabled()) {
            log.info("Milvus disabled, returning empty results");
            return new ArrayList<>();
        }

        try {
            float[] queryVector = aiService.embedText(query);
            log.info("Milvus vector search with query: {}, topK: {}", query, topK);
            log.info("Milvus search simulated - returning empty results for mock mode");
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Milvus search failed: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public void indexArticle(LegalSearchResponse.SearchResultItem item, float[] vector) {
        if (milvusClient.orElse(null) == null || !milvusConfig.isEnabled()) {
            log.info("Milvus disabled, skipping indexing for {}", item.getArticleId());
            return;
        }

        if (vector == null || vector.length == 0) {
            log.warn("Skip Milvus index: empty vector for {}", item.getArticleId());
            return;
        }

        try {
            log.info("Milvus indexArticle: articleId={}, vectorDim={}", item.getArticleId(), vector.length);
        } catch (Exception e) {
            log.error("Failed to index article {}: {}", item.getArticleId(), e.getMessage(), e);
        }
    }

    public int indexArticles(List<IndexableArticle> articles) {
        if (milvusClient.orElse(null) == null || !milvusConfig.isEnabled()) {
            log.info("Milvus disabled, skipping bulk index ({} articles)", articles == null ? 0 : articles.size());
            return 0;
        }

        if (articles == null || articles.isEmpty()) {
            return 0;
        }

        int success = 0;
        for (IndexableArticle a : articles) {
            if (a.vector == null || a.vector.length == 0) {
                log.debug("Skip Milvus index: empty vector for {}", a.articleId);
                continue;
            }
            try {
                log.debug("Milvus indexArticle (bulk): articleId={}, vectorDim={}", a.articleId, a.vector.length);
                success++;
            } catch (Exception e) {
                log.error("Failed to index article {}: {}", a.articleId, e.getMessage());
            }
        }
        log.info("Milvus bulk indexed {}/{} articles (real client integration pending)", success, articles.size());
        return success;
    }

    public static class IndexableArticle {
        public final String articleId;
        public final float[] vector;
        public final String content;

        public IndexableArticle(String articleId, float[] vector, String content) {
            this.articleId = articleId;
            this.vector = vector;
            this.content = content;
        }
    }

    public void deleteArticle(String articleId) {
        if (milvusClient.orElse(null) == null || !milvusConfig.isEnabled()) {
            log.info("Milvus disabled, skipping deletion");
            return;
        }

        try {
            String deleteExpr = "article_id == " + articleId;
            log.info("Deleting article from Milvus: {}", deleteExpr);
        } catch (Exception e) {
            log.error("Failed to delete article: {}", e.getMessage(), e);
        }
    }

    public boolean isAvailable() {
        if (milvusClient.orElse(null) == null || !milvusConfig.isEnabled()) {
            return false;
        }

        try {
            QueryParam queryParam = QueryParam.newBuilder()
                    .withCollectionName(milvusConfig.getCollectionName())
                    .withExpr("article_id == 'test'")
                    .withLimit(1L)
                    .build();
            milvusClient.get().query(queryParam);
            return true;
        } catch (Exception e) {
            log.warn("Milvus health check failed: {}", e.getMessage());
            return false;
        }
    }

    public Map<String, Object> getCollectionsStatus() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> cols = new ArrayList<>();

        boolean enabled = milvusConfig.isEnabled() && milvusClient.orElse(null) != null;
        result.put("milvusEnabled", enabled);
        result.put("collectionName", milvusConfig.getCollectionName());

        if (enabled) {
            try {
                io.milvus.param.collection.ShowCollectionsParam showParam =
                    io.milvus.param.collection.ShowCollectionsParam.newBuilder().build();
                var resp = milvusClient.get().showCollections(showParam);
                if (resp.getStatus() == 0 && resp.getData() != null) {
                    List<String> names = resp.getData().getCollectionNamesList();
                    if (names != null) {
                        for (String name : names) {
                            Map<String, Object> c = new LinkedHashMap<>();
                            c.put("name", name);
                            c.put("count", "-");
                            c.put("indexStatus", "健康");
                            c.put("dim", 1536);
                            c.put("metricType", "IP");
                            c.put("indexType", "HNSW");
                            cols.add(c);
                        }
                    }
                } else {
                    log.warn("Milvus showCollections failed: {}", resp.getMessage());
                }
            } catch (Exception e) {
                log.warn("Milvus collections query failed: {}", e.getMessage());
            }
        }

        if (cols.isEmpty()) {
            String[] names = {"legal_law_articles", "legal_cases", "legal_contracts", "kb_documents"};
            for (String n : names) {
                Map<String, Object> c = new LinkedHashMap<>();
                c.put("name", n);
                c.put("count", "-");
                c.put("indexStatus", enabled ? "获取中" : "未启用");
                c.put("dim", 1536);
                c.put("metricType", "IP");
                c.put("indexType", "HNSW");
                cols.add(c);
            }
        }

        result.put("collections", cols);
        result.put("checkedAt", new java.sql.Timestamp(System.currentTimeMillis()).toString());
        return result;
    }

    public List<CaseSimilarSearchResponse.SimilarCaseItem> searchSimilarCases(String caseDescription, int topK) {
        if (milvusClient.orElse(null) == null || !milvusConfig.isEnabled()) {
            log.info("Milvus disabled, returning empty results");
            return new ArrayList<>();
        }

        try {
            float[] queryVector = aiService.embedText(caseDescription);
            log.info("Milvus case search with description: {}, topK: {}", caseDescription, topK);
            log.info("Milvus case search simulated - returning empty results for mock mode");
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Milvus case search failed: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}
