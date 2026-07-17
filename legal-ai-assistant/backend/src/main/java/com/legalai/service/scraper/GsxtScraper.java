package com.legalai.service.scraper;

import com.legalai.model.ScrapResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class GsxtScraper extends BaseScraper {
    private static final Logger log = LoggerFactory.getLogger(GsxtScraper.class);

    @Override
    public String getPlatformName() {
        return "国家企业信用信息公示系统";
    }

    @Override
    public int getPriority() {
        return 4;
    }

    @Override
    public long getCooldownMs() {
        return 10 * 60 * 1000;
    }

    @Override
    public ScrapResult scrape(String companyName) {
        long start = System.currentTimeMillis();
        log.info("[gsxt] 开始爬取: {}", companyName);

        try {
            String url = "https://www.gsxt.gov.cn/corp-query-search-info.html?searchword=" + encodeQuery(companyName);
            String html = doGet(url);

            if (html == null) {
                return failResult("BLOCKED", "公示系统请求被拦截");
            }

            ScrapResult result = buildResult(html);
            result.setCostMs(System.currentTimeMillis() - start);
            log.info("[gsxt] 完成，耗时 {}ms，提取字段数: {}", result.getCostMs(), result.getFields().size());
            return result;

        } catch (IOException e) {
            log.warn("[gsxt] 网络异常: {}", e.getMessage());
            return failResult("NETWORK_ERROR", e.getMessage());
        } catch (Exception e) {
            log.error("[gsxt] 未知异常: {}", e.getMessage());
            return failResult("ERROR", e.getMessage());
        }
    }
}
