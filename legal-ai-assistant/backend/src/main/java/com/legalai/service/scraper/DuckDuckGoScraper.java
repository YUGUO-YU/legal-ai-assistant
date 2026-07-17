package com.legalai.service.scraper;

import com.legalai.model.ScrapResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class DuckDuckGoScraper extends BaseScraper {
    private static final Logger log = LoggerFactory.getLogger(DuckDuckGoScraper.class);

    @Override
    public String getPlatformName() {
        return "DuckDuckGo";
    }

    @Override
    public int getPriority() {
        return 5;
    }

    @Override
    public long getCooldownMs() {
        return 2 * 60 * 1000;
    }

    @Override
    public ScrapResult scrape(String companyName) {
        long start = System.currentTimeMillis();
        log.info("[DuckDuckGo] 开始爬取: {}", companyName);

        try {
            String url = "https://html.duckduckgo.com/html/?q=" + encodeQuery(companyName) + "+企业信息+工商&kl=zh-cn";
            String html = doGet(url);

            if (html == null) {
                return failResult("BLOCKED", "DuckDuckGo请求被拦截");
            }

            ScrapResult result = buildResult(html);
            result.setCostMs(System.currentTimeMillis() - start);
            log.info("[DuckDuckGo] 完成，耗时 {}ms，提取字段数: {}", result.getCostMs(), result.getFields().size());
            return result;

        } catch (IOException e) {
            log.warn("[DuckDuckGo] 网络异常: {}", e.getMessage());
            return failResult("NETWORK_ERROR", e.getMessage());
        } catch (Exception e) {
            log.error("[DuckDuckGo] 未知异常: {}", e.getMessage());
            return failResult("ERROR", e.getMessage());
        }
    }
}
