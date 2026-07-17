package com.legalai.service;

import com.legalai.model.ScrapResult;
import com.legalai.service.scraper.*;
import com.legalai.utils.HtmlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;

@Service
public class EnterpriseScraperService {
    private static final Logger log = LoggerFactory.getLogger(EnterpriseScraperService.class);

    private final List<EnterpriseScraper> scrapers = new ArrayList<>();

    @PostConstruct
    public void init() {
        scrapers.add(new TianyanchaScraper());
        scrapers.add(new BaiduEnterpriseScraper());
        scrapers.add(new BingSearchScraper());
        scrapers.add(new GsxtScraper());
        scrapers.add(new DuckDuckGoScraper());
        scrapers.sort(Comparator.comparingInt(EnterpriseScraper::getPriority));
        log.info("企业查询爬虫服务初始化完成，共注册 {} 个爬取器", scrapers.size());
    }

    public String scrapeCompanyInfo(String companyName) {
        log.info("开始多源爬取: {}", companyName);
        List<ScrapResult> successfulResults = new ArrayList<>();
        StringBuilder combinedText = new StringBuilder();

        for (EnterpriseScraper scraper : scrapers) {
            if (!scraper.isAvailable()) {
                log.info("[{}] 平台在冷却中，跳过", scraper.getPlatformName());
                continue;
            }

            try {
                log.info("[{}] 正在尝试 (优先级 {})", scraper.getPlatformName(), scraper.getPriority());
                ScrapResult result = scraper.scrape(companyName);

                if (result != null && result.isSuccess()) {
                    Map<String, ScrapResult.ExtractedField> fields = result.getFields();
                    if (!fields.isEmpty()) {
                        successfulResults.add(result);
                        combinedText.append("【来源: ").append(result.getPlatform()).append("】\n");
                        for (Map.Entry<String, ScrapResult.ExtractedField> entry : fields.entrySet()) {
                            combinedText.append(entry.getKey()).append(": ")
                                       .append(entry.getValue().getValue()).append("\n");
                        }
                        combinedText.append("\n");
                        log.info("[{}] 成功提取 {} 个字段", scraper.getPlatformName(), fields.size());

                        if (fields.containsKey("uscc") && fields.containsKey("legalRepresentative") && fields.containsKey("registeredCapital")) {
                            log.info("[{}] 获得核心字段，提前结束爬取", scraper.getPlatformName());
                            break;
                        }
                    } else if (result.getRawContent() != null && result.getRawContent().length() > 50) {
                        successfulResults.add(result);
                        String snippets = HtmlParser.extractSearchSnippets(result.getRawContent());
                        combinedText.append("【来源: ").append(result.getPlatform()).append("】\n")
                                   .append(snippets).append("\n");
                    }
                } else {
                    log.warn("[{}] 爬取失败: {}", scraper.getPlatformName(),
                            result != null ? result.getErrorCode() : "null");
                }
            } catch (Exception e) {
                log.error("[{}] 异常: {}", scraper.getPlatformName(), e.getMessage());
            }
        }

        if (successfulResults.isEmpty()) {
            log.warn("所有数据源均失败: {}", companyName);
            return null;
        }

        log.info("多源爬取完成，成功来源: {} / {}, 合并文本长度: {}",
                successfulResults.size(), scrapers.size(), combinedText.length());
        return combinedText.toString();
    }

    public List<String> getAvailablePlatforms() {
        List<String> available = new ArrayList<>();
        for (EnterpriseScraper scraper : scrapers) {
            if (scraper.isAvailable()) {
                available.add(scraper.getPlatformName() + "(优先级" + scraper.getPriority() + ")");
            } else {
                available.add(scraper.getPlatformName() + "(冷却中)");
            }
        }
        return available;
    }
}
