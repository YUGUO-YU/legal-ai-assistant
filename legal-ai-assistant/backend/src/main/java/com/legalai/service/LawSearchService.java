package com.legalai.service;

import com.legalai.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class LawSearchService {
    private static final Logger log = LoggerFactory.getLogger(LawSearchService.class);

    public LawSearchResponse searchLaws(LawSearchRequest request) {
        log.info("法规查询请求: keyword={}, categoryL1={}, status={}",
            request.getKeyword(), request.getCategoryL1(), request.getStatus());

        List<LawSearchResponse.LawSearchItem> items = new ArrayList<>();

        List<Map<String, Object>> mockLaws = new ArrayList<>();
        mockLaws.add(createLawMap("LAW-2023-001", "中华人民共和国民法典", "民法典", "法律", "民法",
            "全国人民代表大会", "2020-05-28", "2021-01-01", 1, 1260, 156789));
        mockLaws.add(createLawMap("LAW-2023-002", "中华人民共和国劳动合同法", "劳动合同法", "法律", "劳动法",
            "全国人民代表大会常务委员会", "2012-12-28", "2013-07-01", 1, 89, 89456));
        mockLaws.add(createLawMap("LAW-2023-003", "中华人民共和国公司法", "公司法", "法律", "商法",
            "全国人民代表大会常务委员会", "2023-12-29", "2024-07-01", 4, 216, 45678));
        mockLaws.add(createLawMap("LAW-2023-004", "中华人民共和国刑法", "刑法", "法律", "刑法",
            "全国人民代表大会", "2023-12-29", "2024-03-01", 1, 452, 234567));
        mockLaws.add(createLawMap("LAW-2023-005", "最高人民法院关于审理建设工程施工合同纠纷案件适用法律问题的解释（一）",
            "建设工程施工合同司法解释", "司法解释", "建设工程",
            "最高人民法院", "2020-12-29", "2021-01-01", 1, 26, 67890));

        String keyword = request.getKeyword() != null ? request.getKeyword().toLowerCase() : "";

        for (Map<String, Object> law : mockLaws) {
            String title = (String) law.get("title");
            if (keyword.isEmpty() || title.toLowerCase().contains(keyword)) {
                LawSearchResponse.LawSearchItem item = new LawSearchResponse.LawSearchItem();
                item.setLawUuid((String) law.get("lawUuid"));
                item.setTitle(title);
                item.setShortTitle((String) law.get("shortTitle"));
                item.setCategoryL1((String) law.get("categoryL1"));
                item.setCategoryL2((String) law.get("categoryL2"));
                item.setIssuingAuthority((String) law.get("issuingAuthority"));
                item.setIssueDate((String) law.get("issueDate"));
                item.setEffectiveDate((String) law.get("effectiveDate"));
                item.setStatus((Integer) law.get("status"));
                item.setStatusName(getStatusName((Integer) law.get("status")));
                item.setArticleCount((Integer) law.get("articleCount"));
                item.setViewCount((Integer) law.get("viewCount"));
                item.setSourceUrl("https://flk.npc.gov.cn/");
                item.setSourceName("国家法律法规信息库");
                items.add(item);
            }
        }

        LawSearchResponse response = new LawSearchResponse();
        response.setTotal((long) items.size());
        response.setPage(request.getPage());
        response.setPageSize(request.getPageSize());
        response.setTookMs(28L);
        response.setItems(items);
        return response;
    }

    private Map<String, Object> createLawMap(String lawUuid, String title, String shortTitle,
            String categoryL1, String categoryL2, String issuingAuthority,
            String issueDate, String effectiveDate, int status, int articleCount, int viewCount) {
        Map<String, Object> map = new HashMap<>();
        map.put("lawUuid", lawUuid);
        map.put("title", title);
        map.put("shortTitle", shortTitle);
        map.put("categoryL1", categoryL1);
        map.put("categoryL2", categoryL2);
        map.put("issuingAuthority", issuingAuthority);
        map.put("issueDate", issueDate);
        map.put("effectiveDate", effectiveDate);
        map.put("status", status);
        map.put("articleCount", articleCount);
        map.put("viewCount", viewCount);
        return map;
    }

    private String getStatusName(int status) {
        return switch (status) {
            case 1 -> "现行有效";
            case 2 -> "已废止";
            case 3 -> "修订中";
            case 4 -> "尚未生效";
            case 5 -> "部分失效";
            default -> "未知";
        };
    }
}