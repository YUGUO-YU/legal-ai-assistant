package com.legalai.service.scraper;

import com.legalai.model.ScrapResult;

public interface EnterpriseScraper {
    String getPlatformName();
    int getPriority();
    ScrapResult scrape(String companyName);
    boolean isAvailable();
    long getCooldownMs();
}
