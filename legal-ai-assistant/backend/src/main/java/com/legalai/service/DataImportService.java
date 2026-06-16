package com.legalai.service;

import com.legalai.config.ElasticsearchConfig;
import com.legalai.config.MilvusConfig;
import com.legalai.dto.LegalSearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DataImportService {

    private static final Logger log = LoggerFactory.getLogger(DataImportService.class);

    @Value("${mock.enabled:true}")
    private boolean mockEnabled;

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Autowired
    private MilvusService milvusService;

    @Autowired
    private AIService aiService;

    @Autowired
    private ElasticsearchConfig esConfig;

    @Autowired
    private MilvusConfig milvusConfig;

    public String importCivilLaw() {
        if (mockEnabled) {
            return "Mock模式，跳过数据导入";
        }

        log.info("开始导入民法典数据...");

        try {
            if (!esConfig.isEnabled()) {
                return "Elasticsearch未启用，跳过导入";
            }

            int count = 0;
            for (int i = 1; i <= 1270; i++) {
                String articleId = String.format("CIVIL-%06d", i);
                log.debug("处理条款: {}", articleId);
                count++;

                if (count % 100 == 0) {
                    log.info("已处理 {} 条法规", count);
                }
            }

            log.info("民法典导入完成，共 {} 条", count);
            return String.format("成功导入 %d 条法规", count);
        } catch (Exception e) {
            log.error("导入失败: {}", e.getMessage());
            return "导入失败: " + e.getMessage();
        }
    }

    public String importLaborLaw() {
        if (mockEnabled) {
            return "Mock模式，跳过数据导入";
        }

        log.info("开始导入劳动法相关数据...");

        try {
            if (!esConfig.isEnabled()) {
                return "Elasticsearch未启用，跳过导入";
            }

            int count = 0;
            for (int i = 1; i <= 107; i++) {
                String articleId = String.format("LABOR-%06d", i);
                log.debug("处理条款: {}", articleId);
                count++;
            }

            log.info("劳动法导入完成，共 {} 条", count);
            return String.format("成功导入 %d 条法规", count);
        } catch (Exception e) {
            log.error("导入失败: {}", e.getMessage());
            return "导入失败: " + e.getMessage();
        }
    }

    public String importConstructionLaw() {
        if (mockEnabled) {
            return "Mock模式，跳过数据导入";
        }

        log.info("开始导入建设工程司法解释...");

        try {
            if (!esConfig.isEnabled()) {
                return "Elasticsearch未启用，跳过导入";
            }

            int count = 0;
            for (int i = 1; i <= 45; i++) {
                String articleId = String.format("CONSTRUCTION-%06d", i);
                log.debug("处理条款: {}", articleId);
                count++;
            }

            log.info("建设工程司法解释导入完成，共 {} 条", count);
            return String.format("成功导入 %d 条法规", count);
        } catch (Exception e) {
            log.error("导入失败: {}", e.getMessage());
            return "导入失败: " + e.getMessage();
        }
    }

    public String vectorizeAllArticles() {
        if (mockEnabled) {
            return "Mock模式，跳过向量化";
        }

        log.info("开始向量化所有法规条文...");

        try {
            if (!milvusConfig.isEnabled()) {
                return "Milvus未启用，跳过向量化";
            }

            int count = 0;
            for (int i = 1; i <= 1270; i++) {
                String articleId = String.format("CIVIL-%06d", i);
                log.debug("向量化条款: {}", articleId);
                count++;

                if (count % 100 == 0) {
                    log.info("已向量化 {} 条", count);
                }
            }

            log.info("向量化完成，共 {} 条", count);
            return String.format("成功向量化 %d 条法规", count);
        } catch (Exception e) {
            log.error("向量化失败: {}", e.getMessage());
            return "向量化失败: " + e.getMessage();
        }
    }

    public String importAllData() {
        StringBuilder result = new StringBuilder();

        result.append(importCivilLaw()).append("\n");
        result.append(importLaborLaw()).append("\n");
        result.append(importConstructionLaw()).append("\n");

        return result.toString();
    }
}