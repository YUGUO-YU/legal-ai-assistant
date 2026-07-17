package com.legalai.service.scraper;

import com.legalai.model.ScrapResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class BingSearchScraper extends BaseScraper {
    private static final Logger log = LoggerFactory.getLogger(BingSearchScraper.class);

    @Override
    public String getPlatformName() {
        return "必应";
    }

    @Override
    public int getPriority() {
        return 3;
    }

    @Override
    public long getCooldownMs() {
        return 2 * 60 * 1000;
    }

    @Override
    public ScrapResult scrape(String companyName) {
        long start = System.currentTimeMillis();
        log.info("[必应] 开始爬取: {}", companyName);

        try {
            String url = "https://cn.bing.com/search?q=" + encodeQuery(companyName) + "+企业信息+工商+注册资金&count=10";
            String html = doGet(url);

            if (html == null) {
                return failResult("BLOCKED", "必应搜索请求被拦截");
            }

            ScrapResult result = buildResult(html);
            result.setCostMs(System.currentTimeMillis() - start);
            log.info("[必应] 完成，耗时 {}ms，提取字段数: {}", result.getCostMs(), result.getFields().size());
            return result;

        } catch (IOException e) {
            log.warn("[必应] 网络异常: {}", e.getMessage());
            return failResult("NETWORK_ERROR", e.getMessage());
        } catch (Exception e) {
            log.error("[必应] 未知异常: {}", e.getMessage());
            return failResult("ERROR", e.getMessage());
        }
    }
}
