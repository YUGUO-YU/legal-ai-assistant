package com.legalai.service.scraper;

import com.legalai.model.ScrapResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class BaiduEnterpriseScraper extends BaseScraper {
    private static final Logger log = LoggerFactory.getLogger(BaiduEnterpriseScraper.class);

    @Override
    public String getPlatformName() {
        return "百度";
    }

    @Override
    public int getPriority() {
        return 2;
    }

    @Override
    public long getCooldownMs() {
        return 3 * 60 * 1000;
    }

    @Override
    public ScrapResult scrape(String companyName) {
        long start = System.currentTimeMillis();
        log.info("[百度] 开始爬取: {}", companyName);

        try {
            String url = "https://www.baidu.com/s?wd=" + encodeQuery(companyName) + "+企业信息+工商+注册资金&rn=10&ie=utf-8";
            String html = doGet(url);

            if (html == null) {
                return failResult("BLOCKED", "百度搜索请求被拦截");
            }

            ScrapResult result = buildResult(html);
            result.setCostMs(System.currentTimeMillis() - start);
            log.info("[百度] 完成，耗时 {}ms，提取字段数: {}", result.getCostMs(), result.getFields().size());
            return result;

        } catch (IOException e) {
            log.warn("[百度] 网络异常: {}", e.getMessage());
            return failResult("NETWORK_ERROR", e.getMessage());
        } catch (Exception e) {
            log.error("[百度] 未知异常: {}", e.getMessage());
            return failResult("ERROR", e.getMessage());
        }
    }
}
