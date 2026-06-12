package com.legalai.service;

import com.legalai.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CaseSearchService {
    private static final Logger log = LoggerFactory.getLogger(CaseSearchService.class);

    public CaseSearchResponse searchCases(CaseSearchRequest request) {
        log.info("案例查询请求: keyword={}, caseType={}, courtLevel={}",
            request.getKeyword(), request.getCaseType(), request.getCourtLevel());

        List<CaseSearchResponse.CaseSearchItem> items = new ArrayList<>();
        String[] courts = {"上海市第一中级人民法院", "北京市第二中级人民法院", "广东省广州市中级人民法院"};
        String[] caseTypes = {"民事", "刑事", "行政"};
        String[] caseCauses = {"合同纠纷", "劳动争议", "侵权纠纷", "借款合同纠纷"};
        String[] procedures = {"一审", "二审", "再审"};

        int count = Math.min(request.getPageSize(), 8);
        for (int i = 0; i < count; i++) {
            CaseSearchResponse.CaseSearchItem item = new CaseSearchResponse.CaseSearchItem();
            item.setCaseUuid("CASE-202" + (3 + i / 10) + "-" + String.format("%05d", 1000 + i));
            item.setCaseNo("(202" + (2020 + i / 10) + ")沪01民终" + (1234 + i) + "号");
            item.setTitle(generateCaseTitle(request.getKeyword(), i));
            item.setCourt(courts[i % courts.length]);
            item.setCaseType(caseTypes[i % caseTypes.length]);
            item.setCaseCause(caseCauses[i % caseCauses.length]);
            item.setJudgeDate("202" + (2020 + i / 10) + "-0" + (1 + i % 9) + "-15");
            item.setTrialProcedure(procedures[i % procedures.length]);
            item.setJudgmentResult((i % 3) + 1);
            item.setSummary("法院认定被告构成违约，判决支持原告诉讼请求。");
            item.setSourceUrl("https://wenshu.court.gov.cn/");
            item.setSourceName("中国裁判文书网");
            items.add(item);
        }

        CaseSearchResponse response = new CaseSearchResponse();
        response.setTotal(156L);
        response.setPage(request.getPage());
        response.setPageSize(request.getPageSize());
        response.setTookMs(35L);
        response.setItems(items);
        return response;
    }

    private String generateCaseTitle(String keyword, int index) {
        if (keyword != null && !keyword.isEmpty()) {
            return "关于" + keyword + "的" + getCaseCauseName(index) + "案";
        }
        String[] titles = {
            "李某与某公司合同纠纷案",
            "张某与某装饰公司装修合同纠纷案",
            "王某与某公司劳动争议案",
            "陈某与某公司借款合同纠纷案"
        };
        return titles[index % titles.length];
    }

    private String getCaseCauseName(int index) {
        String[] causes = {"合同纠纷", "劳动争议", "侵权纠纷", "建设工程合同纠纷"};
        return causes[index % causes.length];
    }
}