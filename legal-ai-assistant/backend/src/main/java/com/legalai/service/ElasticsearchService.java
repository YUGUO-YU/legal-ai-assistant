package com.legalai.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Highlight;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.legalai.config.ElasticsearchConfig;
import com.legalai.dto.LegalSearchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class ElasticsearchService {

    private final Optional<ElasticsearchClient> client;
    private final ElasticsearchConfig esConfig;

    @Autowired
    public ElasticsearchService(Optional<ElasticsearchClient> client, ElasticsearchConfig esConfig) {
        this.client = client;
        this.esConfig = esConfig;
    }

    public List<LegalSearchResponse.SearchResultItem> searchByES(String query, int page, int pageSize, Map<String, Object> filters) {
        if (client.orElse(null) == null || !esConfig.isEnabled()) {
            log.info("Elasticsearch disabled, returning empty results");
            return new ArrayList<>();
        }

        try {
            int from = (page - 1) * pageSize;

            Query multiMatchQuery = MultiMatchQuery.of(m -> m
                    .query(query)
                    .fields(List.of("content^2", "title^3", "law_title^1.5", "article_no^1"))
                    .fuzziness("AUTO")
            )._toQuery();

            BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
            boolQueryBuilder.must(multiMatchQuery);

            if (filters != null) {
                if (filters.containsKey("category_l1")) {
                    List<String> categories = (List<String>) filters.get("category_l1");
                    boolQueryBuilder.filter(f -> f.terms(t -> t
                            .field("category_l1")
                            .terms(tv -> tv.value(categories.stream()
                                    .map(c -> co.elastic.clients.elasticsearch._types.FieldValue.of(c))
                                    .toList()))));
                }

                if (filters.containsKey("status")) {
                    List<Integer> statuses = (List<Integer>) filters.get("status");
                    boolQueryBuilder.filter(f -> f.terms(t -> t
                            .field("status")
                            .terms(tv -> tv.value(statuses.stream()
                                    .map(s -> co.elastic.clients.elasticsearch._types.FieldValue.of(s))
                                    .toList()))));
                }
            }

            Map<String, HighlightField> highlightFields = new HashMap<>();
            highlightFields.put("content", HighlightField.of(h -> h
                    .preTags(List.of("<em>"))
                    .postTags(List.of("</em>"))
                    .fragmentSize(150)
                    .numberOfFragments(3)));
            highlightFields.put("title", HighlightField.of(h -> h
                    .preTags(List.of("<em>"))
                    .postTags(List.of("</em>"))));

            Highlight highlight = Highlight.of(h -> h.fields(highlightFields));

            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(esConfig.getIndexName())
                    .query(q -> q.bool(boolQueryBuilder.build()))
                    .from(from)
                    .size(pageSize)
                    .highlight(highlight)
            );

            SearchResponse<LawArticleDocument> response = client.get().search(searchRequest, LawArticleDocument.class);

            List<LegalSearchResponse.SearchResultItem> items = new ArrayList<>();
            for (Hit<LawArticleDocument> hit : response.hits().hits()) {
                LawArticleDocument doc = hit.source();
                if (doc == null) continue;

                LegalSearchResponse.SearchResultItem item = new LegalSearchResponse.SearchResultItem();
                item.setArticleId(doc.articleId);
                item.setLawTitle(doc.lawTitle);
                item.setArticleNo(doc.articleNo);
                item.setTitle(doc.title);
                item.setContent(doc.content);
                item.setCategoryL1(doc.categoryL1);
                item.setCategoryL2(doc.categoryL2);
                item.setScore(hit.score() != null ? hit.score() : 0.0);

                if (hit.highlight() != null && hit.highlight().containsKey("content")) {
                    item.setHighlights(hit.highlight().get("content"));
                }

                items.add(item);
            }

            log.info("ES search returned {} results for query: {}", items.size(), query);
            return items;

        } catch (IOException e) {
            log.error("ES search failed: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public LegalSearchResponse.SearchResultItem getArticleById(String articleId) {
        if (client.orElse(null) == null || !esConfig.isEnabled()) {
            log.info("Elasticsearch disabled");
            return null;
        }

        try {
            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(esConfig.getIndexName())
                    .query(q -> q.term(t -> t.field("article_id").value(articleId)))
                    .size(1)
            );

            SearchResponse<LawArticleDocument> response = client.get().search(searchRequest, LawArticleDocument.class);

            if (!response.hits().hits().isEmpty()) {
                Hit<LawArticleDocument> hit = response.hits().hits().get(0);
                LawArticleDocument doc = hit.source();
                if (doc != null) {
                    LegalSearchResponse.SearchResultItem item = new LegalSearchResponse.SearchResultItem();
                    item.setArticleId(doc.articleId);
                    item.setLawTitle(doc.lawTitle);
                    item.setArticleNo(doc.articleNo);
                    item.setTitle(doc.title);
                    item.setContent(doc.content);
                    return item;
                }
            }
        } catch (IOException e) {
            log.error("ES getArticleById failed: {}", e.getMessage(), e);
        }
        return null;
    }

    public boolean isAvailable() {
        if (client.orElse(null) == null || !esConfig.isEnabled()) {
            return false;
        }

        try {
            client.get().info();
            return true;
        } catch (Exception e) {
            log.warn("Elasticsearch health check failed: {}", e.getMessage());
            return false;
        }
    }

    public record LawArticleDocument(
            String articleId,
            String lawId,
            String lawTitle,
            String articleNo,
            String title,
            String content,
            String categoryL1,
            String categoryL2,
            String sourceUrl,
            String sourceName
    ) {}
}