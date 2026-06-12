package com.legalai.service;

import com.legalai.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class LegalSearchService {
    private static final Logger log = LoggerFactory.getLogger(LegalSearchService.class);

    @Value("${mock.enabled:true}")
    private boolean mockEnabled;

    private static final String SYSTEM_PROMPT =
        "你是一个专业的法律助手，专注于中国法律法规的检索与解读。你拥有法学背景，能够准确理解法律条文含义并给出专业解释。\n\n" +
        "核心任务：根据用户输入的法律问题，从检索到的法规条文中提取相关信息，给出准确、专业的回答。\n\n" +
        "约束条件（HARD RULES）：\n" +
        "1. 溯源必须：每个法律结论必须标注来源，格式为：[法规名称] 第X条 | 来源URL\n" +
        "2. 禁止胡编：只陈述检索结果中明确存在的内容，不得编造、推测法条内容\n" +
        "3. 不确定声明：如检索结果不足以回答，明确说明\"未检索到相关内容\"\n" +
        "4. 语言严谨：使用规范法律用语，避免口语化表达\n" +
        "5. 时效性：注意标注法条的时效性，提示可能已修订\n\n" +
        "输出格式：\n" +
        "## 回答\n" +
        "[正文内容]\n" +
        "## 参考依据\n" +
        "1. [法规名称] 第X条 | 来源URL\n" +
        "## 追问建议\n" +
        "- 问题1\n" +
        "- 问题2";

    private static final List<Map<String, String>> MOCK_LAWS = List.of(
        Map.of(
            "articleId", "ART-2023-001",
            "lawTitle", "中华人民共和国民法典",
            "articleNo", "第一百四十八条",
            "title", "欺诈的认定",
            "content", "一方以欺诈手段，使对方在违背真实意思的情况下订立的合同，受欺诈方有权请求人民法院或者仲裁机构予以撤销。",
            "categoryL1", "法律",
            "categoryL2", "民法"
        ),
        Map.of(
            "articleId", "ART-2023-002",
            "lawTitle", "中华人民共和国民法典",
            "articleNo", "第一百四十九条",
            "title", "第三人欺诈",
            "content", "第三人实施欺诈行为，使一方陷入错误认识的，适用欺诈规定。",
            "categoryL1", "法律",
            "categoryL2", "民法"
        ),
        Map.of(
            "articleId", "ART-2023-003",
            "lawTitle", "中华人民共和国民法典",
            "articleNo", "第五百六十三条",
            "title", "合同解除情形",
            "content", "有下列情形之一的，当事人可以解除合同：（一）因不可抗力致使不能实现合同目的；（二）履行期限届满前，当事人一方明确表示或者以自己的行为表明不履行主要债务；（三）当事人一方迟延履行主要债务，经催告后在合理期限内仍未履行；（四）当事人一方迟延履行债务或者有其他违约行为致使不能实现合同目的。",
            "categoryL1", "法律",
            "categoryL2", "民法"
        ),
        Map.of(
            "articleId", "ART-2023-004",
            "lawTitle", "中华人民共和国民法典",
            "articleNo", "第五百七十七条",
            "title", "违约责任",
            "content", "当事人一方不履行合同义务或者履行合同义务不符合约定的，应当承担违约责任。",
            "categoryL1", "法律",
            "categoryL2", "民法"
        ),
        Map.of(
            "articleId", "ART-2023-005",
            "lawTitle", "中华人民共和国民法典",
            "articleNo", "第五百八十四条",
            "title", "损失赔偿范围",
            "content", "当事人一方不履行合同义务或者履行合同义务不符合约定的，给对方造成损失的，损失赔偿额应当相当于因违约所造成的损失，包括合同履行后可以获得的利益。",
            "categoryL1", "法律",
            "categoryL2", "民法"
        ),
        Map.of(
            "articleId", "ART-2023-006",
            "lawTitle", "中华人民共和国劳动合同法",
            "articleNo", "第三十九条",
            "title", "用人单位单方解除劳动合同",
            "content", "劳动者有下列情形之一的，用人单位可以解除劳动合同：（一）在试用期间被证明不符合录用条件的；（二）严重违反用人单位的规章制度的；（三）严重失职，营私舞弊，给用人单位造成重大损害的。",
            "categoryL1", "法律",
            "categoryL2", "劳动法"
        ),
        Map.of(
            "articleId", "ART-2023-007",
            "lawTitle", "中华人民共和国劳动合同法",
            "articleNo", "第四十六条",
            "title", "经济补偿",
            "content", "有下列情形之一的，用人单位应当向劳动者支付经济补偿：（一）劳动者依照本法第三十八条规定解除劳动合同的；（二）用人单位依照本法第三十六条规定向劳动者提出解除劳动合同并与劳动者协商一致解除劳动合同的。",
            "categoryL1", "法律",
            "categoryL2", "劳动法"
        ),
        Map.of(
            "articleId", "ART-2023-008",
            "lawTitle", "最高人民法院关于审理建设工程施工合同纠纷案件适用法律问题的解释（一）",
            "articleNo", "第十条",
            "title", "工程价款结算",
            "content", "当事人对建设工程的计价标准或者计价方法有约定的，按照约定结算工程价款。",
            "categoryL1", "司法解释",
            "categoryL2", "建设工程"
        )
    );

    public LegalSearchResponse search(LegalSearchRequest request) {
        log.info("法律检索请求: query={}, page={}, pageSize={}",
            request.getQuery(), request.getPage(), request.getPageSize());

        validateRequest(request);

        return mockSearch(request);
    }

    private void validateRequest(LegalSearchRequest request) {
        if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            throw new IllegalArgumentException("检索关键词不能为空");
        }
        if (request.getQuery().length() > 200) {
            throw new IllegalArgumentException("检索关键词长度不能超过200字");
        }
    }

    private LegalSearchResponse mockSearch(LegalSearchRequest request) {
        List<LegalSearchResponse.SearchResultItem> items = new ArrayList<>();
        String query = request.getQuery() != null ? request.getQuery().toLowerCase() : "";

        int count = 0;
        for (Map<String, String> law : MOCK_LAWS) {
            if (count >= request.getPageSize()) break;

            String content = law.get("content").toLowerCase();
            String title = law.get("title").toLowerCase();
            String lawTitle = law.get("lawTitle").toLowerCase();

            if (query.isEmpty() ||
                content.contains(query) ||
                title.contains(query) ||
                lawTitle.contains(query) ||
                matchQueryWithSynonyms(query, content, title, lawTitle)) {

                LegalSearchResponse.SearchResultItem item = new LegalSearchResponse.SearchResultItem();
                item.setArticleId(law.get("articleId"));
                item.setLawId("LAW-2023-001");
                item.setLawTitle(law.get("lawTitle"));
                item.setArticleNo(law.get("articleNo"));
                item.setTitle(law.get("title"));
                item.setContent(law.get("content"));
                item.setHighlights(List.of("<em>" + highlightKeyword(request.getQuery()) + "</em>"));
                item.setSourceUrl("https://flk.npc.gov.cn/detail2.html?ZmY4MDgxODE3OTZhNjMyYTAxNzk3YWIzYzIyYzA2M2I=");
                item.setSourceName("国家法律法规信息库");
                item.setScore(18.56 - count * 2.0);
                item.setRelatedCasesCount(new Random().nextInt(10));
                items.add(item);
                count++;
            }
        }

        List<LegalSearchResponse.RelatedCase> relatedCases = new ArrayList<>();
        if (Boolean.TRUE.equals(request.getIncludeCases()) && !items.isEmpty()) {
            relatedCases = generateRelatedCases(items.get(0).getTitle());
        }

        LegalSearchResponse response = new LegalSearchResponse();
        response.setTotal((long) items.size());
        response.setPage(request.getPage());
        response.setPageSize(request.getPageSize());
        response.setTookMs(45L);
        response.setItems(items);
        response.setRelatedCases(relatedCases);
        return response;
    }

    private boolean matchQueryWithSynonyms(String query, String content, String title, String lawTitle) {
        Map<String, List<String>> synonyms = Map.of(
            "欺诈", List.of("欺骗", "诈骗", "骗取"),
            "违约", List.of("违约行为", "违反合同", "不履行"),
            "解除", List.of("解除合同", "终止合同", "撤销"),
            "劳动", List.of("劳动合同", "劳动关系", "劳动争议"),
            "赔偿", List.of("赔偿", "补偿", "损失赔偿")
        );

        for (Map.Entry<String, List<String>> entry : synonyms.entrySet()) {
            if (query.contains(entry.getKey())) {
                for (String syn : entry.getValue()) {
                    if (content.contains(syn) || title.contains(syn) || lawTitle.contains(syn)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private String highlightKeyword(String query) {
        if (query == null || query.isEmpty()) return "法律";
        String[] words = query.replaceAll("[^\\u4e00-\u9fa5a-zA-Z0-9]", " ").split("\\s+");
        return words.length > 0 ? words[0] : "法律";
    }

    private List<LegalSearchResponse.RelatedCase> generateRelatedCases(String topic) {
        List<LegalSearchResponse.RelatedCase> cases = new ArrayList<>();

        String[][] mockCases = {
            {"CASE-2021-001", "(2021)沪01民终1234号", "某投资公司与张某合同纠纷案", "上海市第一中级人民法院", "法院认定被告在签订投资协议时存在欺诈行为，判决撤销合同。"},
            {"CASE-2022-001", "(2022)京02民终5678号", "李某与北京某公司劳动争议案", "北京市第二中级人民法院", "公司违法解除劳动合同，判决支付经济补偿金。"}
        };

        for (String[] caseData : mockCases) {
            LegalSearchResponse.RelatedCase rc = new LegalSearchResponse.RelatedCase();
            rc.setCaseUuid(caseData[0]);
            rc.setCaseNo(caseData[1]);
            rc.setTitle(caseData[2]);
            rc.setCourt(caseData[3]);
            rc.setSummary(caseData[4]);
            rc.setSourceUrl("https://wenshu.court.gov.cn/");
            rc.setSourceName("中国裁判文书网");
            cases.add(rc);
        }

        return cases;
    }

    public String generateAnswer(String query, List<LegalSearchResponse.SearchResultItem> items) {
        if (items == null || items.isEmpty()) {
            return "未检索到相关法律法规，建议您更换关键词后重试。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("根据检索到的法律法规，回答如下：\n\n");

        for (LegalSearchResponse.SearchResultItem item : items) {
            sb.append(String.format("【%s】%s %s\n%s\n来源：%s\n\n",
                item.getLawTitle(),
                item.getArticleNo(),
                item.getTitle(),
                item.getContent(),
                item.getSourceUrl()
            ));
        }

        sb.append("---\n\n");
        sb.append("**免责声明**：本回答基于检索到的法律法规生成，仅供参考，不构成法律意见。\n");
        sb.append("如需针对具体案件的法律建议，请咨询具有执业资格的专业律师。");

        return sb.toString();
    }

    public List<String> generateSuggestedQueries(String query) {
        List<String> suggestions = new ArrayList<>();

        if (query.contains("合同")) {
            suggestions.add("合同欺诈如何认定？");
            suggestions.add("合同违约责任有哪些？");
            suggestions.add("合同解除的条件是什么？");
        } else if (query.contains("劳动")) {
            suggestions.add("劳动合同解除的条件是什么？");
            suggestions.add("经济补偿金如何计算？");
            suggestions.add("加班费如何主张？");
        } else if (query.contains("借款")) {
            suggestions.add("民间借贷利息上限是多少？");
            suggestions.add("借款合同纠纷如何起诉？");
        } else {
            suggestions.add("如何签订一份有效的合同？");
            suggestions.add("遇到纠纷如何维权？");
            suggestions.add("诉讼时效是多长时间？");
        }

        return suggestions;
    }
}