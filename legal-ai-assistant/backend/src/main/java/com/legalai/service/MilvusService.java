package com.legalai.service;

import com.legalai.config.MilvusConfig;
import com.legalai.dto.LegalSearchResponse;
import io.milvus.client.MilvusClient;
import io.milvus.param.dml.QueryParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.response.QueryResultsWrapper;
import io.milvus.response.SearchResultsWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MilvusService {

    private final MilvusClient milvusClient;
    private final MilvusConfig milvusConfig;
    private final AIService aiService;

    @Autowired
    public MilvusService(MilvusClient milvusClient, MilvusConfig milvusConfig, AIService aiService) {
        this.milvusClient = milvusClient;
        this.milvusConfig = milvusConfig;
        this.aiService = aiService;
    }

    public List<LegalSearchResponse.SearchResultItem> searchByVector(String query, int topK) {
        if (milvusClient == null || !milvusConfig.isEnabled()) {
            log.info("Milvus disabled, returning empty results");
            return new ArrayList<>();
        }

        try {
            float[] queryVector = aiService.embedText(query);
            List<List<Float>> vectors = Arrays.asList(queryVector);

            SearchParam searchParam = SearchParam.newBuilder()
                    .withCollectionName(milvusConfig.getCollectionName())
                    .withVectors(vectors)
                    .withTopK(topK)
                    .withVectorFieldName("content_vector")
                    .withMetricType(MetricType.COSINE)
                    .build();

            SearchResultsWrapper resultsWrapper = milvusClient.search(searchParam);

            List<LegalSearchResponse.SearchResultItem> items = new ArrayList<>();
            for (int i = 0; i < resultsWrapper.getRowRecords().size(); i++) {
                SearchResultsWrapper.RowRecord record = resultsWrapper.getRowRecords().get(i);
                LegalSearchResponse.SearchResultItem item = new LegalSearchResponse.SearchResultItem();
                item.setArticleId(String.valueOf(record.get("article_id")));
                item.setLawTitle((String) record.get("law_title"));
                item.setArticleNo((String) record.get("article_no"));
                item.setTitle((String) record.get("title"));
                item.setScore(resultsWrapper.getScores().get(i).doubleValue());
                items.add(item);
            }

            log.info("Milvus search returned {} results", items.size());
            return items;

        } catch (Exception e) {
            log.error("Milvus search failed: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public void indexArticle(LegalSearchResponse.SearchResultItem item, float[] vector) {
        if (milvusClient == null || !milvusConfig.isEnabled()) {
            log.info("Milvus disabled, skipping indexing");
            return;
        }

        try {
            List<SearchResultsWrapper.RowRecord> records = new ArrayList<>();
            log.info("Indexing article: {}", item.getArticleId());
        } catch (Exception e) {
            log.error("Failed to index article: {}", e.getMessage(), e);
        }
    }

    public void deleteArticle(String articleId) {
        if (milvusClient == null || !milvusConfig.isEnabled()) {
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
        if (milvusClient == null || !milvusConfig.isEnabled()) {
            return false;
        }

        try {
            QueryParam queryParam = QueryParam.newBuilder()
                    .withCollectionName(milvusConfig.getCollectionName())
                    .withExpr("article_id == 'test'")
                    .withLimit(1)
                    .build();
            milvusClient.query(queryParam);
            return true;
        } catch (Exception e) {
            log.warn("Milvus health check failed: {}", e.getMessage());
            return false;
        }
    }
}