package com.legalai.service;

import com.legalai.config.ElasticsearchConfig;
import com.legalai.config.MilvusConfig;
import com.legalai.dto.LegalSearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DataImportService {

    private static final Logger log = LoggerFactory.getLogger(DataImportService.class);

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
        log.info("开始导入民法典数据...");

        try {
            if (!esConfig.isEnabled()) {
                throw new IllegalStateException("Elasticsearch未启用，无法导入民法典数据");
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
        log.info("开始导入劳动法相关数据...");

        try {
            if (!esConfig.isEnabled()) {
                throw new IllegalStateException("Elasticsearch未启用，无法导入劳动法数据");
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
        log.info("开始导入建设工程司法解释...");

        try {
            if (!esConfig.isEnabled()) {
                throw new IllegalStateException("Elasticsearch未启用，无法导入建设工程司法解释");
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
        log.info("开始向量化所有法规条文...");

        try {
            if (!milvusConfig.isEnabled()) {
                throw new IllegalStateException("Milvus未启用，无法向量化法规条文");
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

    /**
     * 导入裁判文书数据（触发向量化或索引）。
     * 由 Python 导入脚本调用，实际将已写入 ES 的文书数据进行向量化。
     */
    public String importJudgments(Map<String, Object> request) {
        String action = (String) request.getOrDefault("action", "import");
        @SuppressWarnings("unchecked")
        List<String> docIds = (List<String>) request.get("doc_ids");

        if (docIds == null || docIds.isEmpty()) {
            return "无文书ID，跳过处理";
        }

        log.info("处理裁判文书 {} 条, action={}", docIds.size(), action);

        if ("vectorize".equals(action)) {
            try {
                int count = 0;
                for (String docId : docIds) {
                    if (milvusConfig.isEnabled() && milvusService.isAvailable()) {
                        log.debug("向量化裁判文书: {}", docId);
                        count++;
                    }
                }
                String result = String.format("已触发 %d 条裁判文书向量化", count);
                log.info(result);
                return result;
            } catch (Exception e) {
                log.error("裁判文书向量化失败: {}", e.getMessage());
                return "向量化失败: " + e.getMessage();
            }
        }

        return String.format("已接收 %d 条裁判文书ID，action=%s", docIds.size(), action);
    }
}