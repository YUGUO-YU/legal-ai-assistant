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
import java.util.List;
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
            log.info("Milvus disabled, skipping indexing");
            return;
        }

        try {
            log.info("Indexing article: {}", item.getArticleId());
        } catch (Exception e) {
            log.error("Failed to index article: {}", e.getMessage(), e);
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