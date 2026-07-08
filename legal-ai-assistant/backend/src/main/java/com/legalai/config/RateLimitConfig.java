package com.legalai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitConfig {
    private ApiConfig api = new ApiConfig();

    public ApiConfig getApi() {
        return api;
    }

    public void setApi(ApiConfig api) {
        this.api = api;
    }

    public static class ApiConfig {
        private QpsConfig search = new QpsConfig(30);
        private QpsConfig draft = new QpsConfig(10);

        public QpsConfig getSearch() {
            return search;
        }

        public void setSearch(QpsConfig search) {
            this.search = search;
        }

        public QpsConfig getDraft() {
            return draft;
        }

        public void setDraft(QpsConfig draft) {
            this.draft = draft;
        }
    }

    public static class QpsConfig {
        private int qps;

        public QpsConfig() {}

        public QpsConfig(int qps) {
            this.qps = qps;
        }

        public int getQps() {
            return qps;
        }

        public void setQps(int qps) {
            this.qps = qps;
        }
    }
}
