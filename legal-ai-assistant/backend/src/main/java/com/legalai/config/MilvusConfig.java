package com.legalai.config;

import io.milvus.client.MilvusClient;
import io.milvus.param.ConnectParam;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    @ConditionalOnProperty(name = "milvus.enabled", havingValue = "true")
    public MilvusClient milvusClient() {
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