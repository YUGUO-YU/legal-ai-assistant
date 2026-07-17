package com.legalai.service.scraper;

import com.legalai.model.ScrapResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class TianyanchaScraper extends BaseScraper {
    private static final Logger log = LoggerFactory.getLogger(TianyanchaScraper.class);

    @Override
    public String getPlatformName() {
        return "天眼查";
    }

    @Override
    public int getPriority() {
        return 1;
    }

    @Override
    public long getCooldownMs() {
        return 5 * 60 * 1000;
    }

    @Override
    public ScrapResult scrape(String companyName) {
        long start = System.currentTimeMillis();
        log.info("[天眼查] 开始爬取: {}", companyName);

        try {
            String url = "https://www.tianyancha.com/search?key=" + encodeQuery(companyName);
            String html = doGet(url);

            if (html == null) {
                return failResult("BLOCKED", "天眼查请求被拦截，切换其他数据源");
            }

            if (html.contains("验证") || html.contains("captcha") || html.contains("验证码")) {
                markCooldown();
                return failResult("CAPTCHA", "天眼查需要验证码验证");
            }

            ScrapResult result = buildResult(html);
            result.setCostMs(System.currentTimeMillis() - start);
            log.info("[天眼查] 完成，耗时 {}ms，提取字段数: {}", result.getCostMs(), result.getFields().size());
            return result;

        } catch (IOException e) {
            log.warn("[天眼查] 网络异常: {}", e.getMessage());
            return failResult("NETWORK_ERROR", e.getMessage());
        } catch (Exception e) {
            log.error("[天眼查] 未知异常: {}", e.getMessage());
            return failResult("ERROR", e.getMessage());
        }
    }
}
