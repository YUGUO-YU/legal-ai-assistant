package com.legalai.config;

import io.milvus.client.MilvusClient;
import io.milvus.param.CollectionAliasParam;
import io.milvus.param.ConnectParam;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.IndexType;
import io.milvus.param.collection.MetricType;
import io.milvus.param.index.CreateIndexParam;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
@Slf4j
public class MilvusConfig {

    @Value("${milvus.host:localhost}")
    private String host;

    @Value("${milvus.port:19530}")
    private Integer port;

    @Value("${milvus.collection-name:legal_law_articles}")
    private String collectionName;

    @Value("${milvus.dimension:1536}")
    private Integer dimension;

    @Value("${milvus.enabled:false}")
    private boolean enabled;

    @Bean
    public MilvusClient milvusClient() {
        if (!enabled) {
            log.info("Milvus disabled, returning mock client");
            return null;
        }

        ConnectParam connectParam = ConnectParam.newBuilder()
                .withHost(host)
                .withPort(port)
                .build();

        MilvusClient client = new io.milvus.client.MilvusServiceClient(connectParam);
        log.info("Connected to Milvus at {}:{}", host, port);

        return client;
    }

    @PostConstruct
    public void initCollection() {
        if (!enabled) {
            log.info("Milvus disabled, skipping collection initialization");
            return;
        }

        log.info("Will initialize Milvus collection: {} with dimension: {}",
                collectionName, dimension);
    }

    public String getCollectionName() {
        return collectionName;
    }

    public Integer getDimension() {
        return dimension;
    }

    public boolean isEnabled() {
        return enabled;
    }
}