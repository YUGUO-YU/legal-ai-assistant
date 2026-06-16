package com.legalai.service;

import com.legalai.config.ElasticsearchConfig;
import com.legalai.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CaseSearchService {
    private static final Logger log = LoggerFactory.getLogger(CaseSearchService.class);

    @Value("${mock.enabled:true}")
    private boolean mockEnabled;

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Autowired
    private ElasticsearchConfig esConfig;

    public CaseSearchResponse searchCases(CaseSearchRequest request) {
        log.info("案例查询请求: keyword={}, caseType={}, courtLevel={}",
            request.getKeyword(), request.getCaseType(), request.getCourtLevel());

        validateRequest(request);

        if (mockEnabled) {
            return mockSearchCases(request);
        }

        return esSearchCases(request);
    }

    private void validateRequest(CaseSearchRequest request) {
        if (request.getKeyword() != null && request.getKeyword().length() > 200) {
            throw new IllegalArgumentException("关键词长度不能超过200字");
        }
    }

    private CaseSearchResponse mockSearchCases(CaseSearchRequest request) {
        long startTime = System.currentTimeMillis();

        List<CaseSearchResponse.CaseSearchItem> allCases = generateMockCases(request);

        List<CaseSearchResponse.CaseSearchItem> filteredCases = applyFilters(allCases, request);

        List<CaseSearchResponse.CaseSearchItem> sortedCases = applySort(filteredCases, request);

        int total = sortedCases.size();
        int from = (request.getPage() - 1) * request.getPageSize();
        int to = Math.min(from + request.getPageSize(), total);

        List<CaseSearchResponse.CaseSearchItem> pagedCases = total > from
            ? sortedCases.subList(from, to)
            : Collections.emptyList();

        CaseSearchResponse response = new CaseSearchResponse();
        response.setTotal((long) total);
        response.setPage(request.getPage());
        response.setPageSize(request.getPageSize());
        response.setTookMs(System.currentTimeMillis() - startTime);
        response.setItems(pagedCases);

        return response;
    }

    private CaseSearchResponse esSearchCases(CaseSearchRequest request) {
        long startTime = System.currentTimeMillis();

        Map<String, Object> filters = buildFilters(request);

        List<LegalSearchResponse.SearchResultItem> esResults = elasticsearchService.searchByES(
            request.getKeyword(),
            request.getPage(),
            request.getPageSize(),
            filters
        );

        List<CaseSearchResponse.CaseSearchItem> items = esResults.stream()
            .map(this::convertToCaseSearchItem)
            .collect(Collectors.toList());

        CaseSearchResponse response = new CaseSearchResponse();
        response.setTotal((long) items.size());
        response.setPage(request.getPage());
        response.setPageSize(request.getPageSize());
        response.setTookMs(System.currentTimeMillis() - startTime);
        response.setItems(items);

        return response;
    }

    private CaseSearchResponse.CaseSearchItem convertToCaseSearchItem(LegalSearchResponse.SearchResultItem item) {
        CaseSearchResponse.CaseSearchItem caseItem = new CaseSearchResponse.CaseSearchItem();
        caseItem.setCaseUuid(item.getArticleId());
        caseItem.setCaseNo(item.getArticleNo());
        caseItem.setTitle(item.getTitle());
        caseItem.setSummary(item.getContent());
        caseItem.setSourceUrl(item.getSourceUrl());
        caseItem.setSourceName(item.getSourceName());
        return caseItem;
    }

    private List<CaseSearchResponse.CaseSearchItem> generateMockCases(CaseSearchRequest request) {
        List<CaseSearchResponse.CaseSearchItem> cases = new ArrayList<>();
        String[] courts = {"上海市第一中级人民法院", "北京市第二中级人民法院", "广东省广州市中级人民法院", "深圳市中级人民法院"};
        String[] caseTypes = {"民事", "刑事", "行政", "执行"};
        String[] caseCauses = {"合同纠纷", "劳动争议", "侵权纠纷", "借款合同纠纷", "建设工程合同纠纷", "房屋买卖合同纠纷"};
        String[] procedures = {"一审", "二审", "再审", "执行"};
        String[] results = {"全部支持", "部分支持", "驳回", "调解", "撤诉"};

        for (int i = 0; i < 50; i++) {
            CaseSearchResponse.CaseSearchItem item = new CaseSearchResponse.CaseSearchItem();
            item.setCaseUuid("CASE-202" + (3 + i / 10) + "-" + String.format("%05d", 1000 + i));
            item.setCaseNo("(202" + (2020 + i / 10) + ")沪01民终" + (1234 + i) + "号");
            item.setTitle(generateCaseTitle(request.getKeyword(), i));
            item.setCourt(courts[i % courts.length]);
            item.setCaseType(caseTypes[i % caseTypes.length]);
            item.setCaseCause(caseCauses[i % caseCauses.length]);
            item.setJudgeDate("202" + (2020 + i / 10) + "-0" + (1 + i % 9) + "-15");
            item.setTrialProcedure(procedures[i % procedures.length]);
            item.setJudgmentResult((i % 5) + 1);
            item.setLitigationAmount((long) (10000 + Math.random() * 10000000));
            item.setSummary("法院认定被告构成违约，判决支持原告诉讼请求。案件涉及标的额人民币" + String.format("%.2f", item.getLitigationAmount() / 10000.0) + "万元。");
            item.setSourceUrl("https://wenshu.court.gov.cn/");
            item.setSourceName("中国裁判文书网");
            cases.add(item);
        }

        return cases;
    }

    private List<CaseSearchResponse.CaseSearchItem> applyFilters(
            List<CaseSearchResponse.CaseSearchItem> cases,
            CaseSearchRequest request) {

        return cases.stream()
            .filter(c -> {
                if (request.getCaseType() != null && !c.getCaseType().equals(getCaseTypeName(request.getCaseType()))) {
                    return false;
                }
                if (request.getCourtLevel() != null && request.getCourtLevel() != c.getCourtLevel()) {
                    return false;
                }
                if (request.getJudgmentResult() != null && !request.getJudgmentResult().equals(c.getJudgmentResult())) {
                    return false;
                }
                return true;
            })
            .collect(Collectors.toList());
    }

    private List<CaseSearchResponse.CaseSearchItem> applySort(
            List<CaseSearchResponse.CaseSearchItem> cases,
            CaseSearchRequest request) {

        String sortField = request.getSort();
        String order = request.getOrder();

        if (sortField == null || sortField.isEmpty()) {
            sortField = "judge_date";
        }
        if (order == null || order.isEmpty()) {
            order = "desc";
        }

        final String finalSortField = sortField;
        final String finalOrder = order;

        return cases.stream()
            .sorted((a, b) -> {
                int cmp = 0;
                switch (finalSortField) {
                    case "judge_date":
                        cmp = a.getJudgeDate().compareTo(b.getJudgeDate());
                        break;
                    case "litigation_amount":
                        cmp = Long.compare(
                            a.getLitigationAmount() != null ? a.getLitigationAmount() : 0,
                            b.getLitigationAmount() != null ? b.getLitigationAmount() : 0
                        );
                        break;
                    default:
                        cmp = a.getCaseUuid().compareTo(b.getCaseUuid());
                }
                return "desc".equalsIgnoreCase(finalOrder) ? -cmp : cmp;
            })
            .collect(Collectors.toList());
    }

    private Map<String, Object> buildFilters(CaseSearchRequest request) {
        Map<String, Object> filters = new HashMap<>();
        if (request.getCaseType() != null) {
            filters.put("case_type", getCaseTypeName(request.getCaseType()));
        }
        if (request.getCourtLevel() != null) {
            filters.put("court_level", request.getCourtLevel());
        }
        if (request.getJudgmentResult() != null) {
            filters.put("judgment_result", request.getJudgmentResult());
        }
        return filters;
    }

    private String getCaseTypeName(Integer type) {
        if (type == null) return null;
        switch (type) {
            case 1: return "民事";
            case 2: return "刑事";
            case 3: return "行政";
            case 4: return "执行";
            default: return "民事";
        }
    }

    private String generateCaseTitle(String keyword, int index) {
        if (keyword != null && !keyword.isEmpty()) {
            return "关于" + keyword + "的" + getCaseCauseName(index) + "案";
        }
        String[] titles = {
            "李某与某公司合同纠纷案",
            "张某与某装饰公司装修合同纠纷案",
            "王某与某公司劳动争议案",
            "陈某与某公司借款合同纠纷案",
            "刘某与某建筑公司建设工程合同纠纷案"
        };
        return titles[index % titles.length];
    }

    private String getCaseCauseName(int index) {
        String[] causes = {"合同纠纷", "劳动争议", "侵权纠纷", "建设工程合同纠纷", "房屋买卖合同纠纷"};
        return causes[index % causes.length];
    }

    public CaseSearchResponse.CaseSearchItem getCaseDetail(String caseUuid) {
        log.info("获取案例详情: caseUuid={}", caseUuid);

        CaseSearchRequest request = new CaseSearchRequest();
        request.setKeyword("");
        request.setPageSize(50);

        CaseSearchResponse response = mockSearchCases(request);

        return response.getItems().stream()
            .filter(item -> item.getCaseUuid().equals(caseUuid))
            .findFirst()
            .orElse(null);
    }
}