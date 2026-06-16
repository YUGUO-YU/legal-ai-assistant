package com.legalai.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class ElasticsearchConfig {

    @Value("${elasticsearch.host:localhost}")
    private String host;

    @Value("${elasticsearch.port:9200}")
    private Integer port;

    @Value("${elasticsearch.index-name:legal_law_articles}")
    private String indexName;

    @Value("${elasticsearch.enabled:false}")
    private boolean enabled;

    @Bean
    public RestClient restClient() {
        if (!enabled) {
            log.info("Elasticsearch disabled, returning null RestClient");
            return null;
        }

        RestClient restClient = RestClient.builder(
                new HttpHost(host, port, "http")
        ).build();

        log.info("Elasticsearch RestClient created: {}:{}", host, port);
        return restClient;
    }

    @Bean
    public ElasticsearchTransport elasticsearchTransport(RestClient restClient) {
        if (restClient == null) {
            return null;
        }

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        return new RestClientTransport(restClient, new JacksonJsonpMapper(objectMapper));
    }

    @Bean
    public ElasticsearchClient elasticsearchClient(ElasticsearchTransport transport) {
        if (transport == null) {
            log.info("Elasticsearch disabled");
            return null;
        }

        ElasticsearchClient client = new ElasticsearchClient(transport);
        log.info("ElasticsearchClient created");
        return client;
    }

    public String getIndexName() {
        return indexName;
    }

    public boolean isEnabled() {
        return enabled;
    }
}