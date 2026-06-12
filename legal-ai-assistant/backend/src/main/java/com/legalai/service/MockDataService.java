package com.legalai.service;

import com.legalai.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MockDataService {
    private static final Logger log = LoggerFactory.getLogger(MockDataService.class);

    private static final Random RANDOM = new Random();

    public LegalSearchResponse searchLaws(LegalSearchRequest request) {
        log.info("Mock searching laws for query: {}", request.getQuery());

        List<LegalSearchResponse.SearchResultItem> items = new ArrayList<>();
        for (int i = 0; i < Math.min(request.getPageSize(), 5); i++) {
            LegalSearchResponse.SearchResultItem item = new LegalSearchResponse.SearchResultItem();
            item.setArticleId("ART-2023-" + String.format("%03d", i + 1));
            item.setLawId("LAW-2023-001");
            item.setLawTitle("中华人民共和国民法典");
            item.setArticleNo("第" + (148 + i) + "条");
            item.setTitle(getTitleForQuery(request.getQuery(), i));
            item.setContent(getContentForQuery(request.getQuery(), i));
            item.setHighlights(List.of("<em>" + extractKeyword(request.getQuery()) + "</em>"));
            item.setSourceUrl("https://flk.npc.gov.cn/detail2.html?ZmY4MDgxODE3OTZhNjMyYTAxNzk3YWIzYzIyYzA2M2I");
            item.setSourceName("国家法律法规信息库");
            item.setScore(18.56 - i * 2);
            item.setRelatedCasesCount(RANDOM.nextInt(10));
            items.add(item);
        }

        List<LegalSearchResponse.RelatedCase> relatedCases = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            LegalSearchResponse.RelatedCase rc = new LegalSearchResponse.RelatedCase();
            rc.setCaseUuid("CASE-2021-" + (12345 + i));
            rc.setCaseNo("(2021)沪01民终" + (1234 + i) + "号");
            rc.setTitle("某投资公司与张某合同纠纷案");
            rc.setCourt("上海市第一中级人民法院");
            rc.setSummary("法院认定被告在签订投资协议时存在欺诈行为，判决撤销合同。");
            rc.setSourceUrl("https://wenshu.court.gov.cn/");
            rc.setSourceName("中国裁判文书网");
            relatedCases.add(rc);
        }

        LegalSearchResponse response = new LegalSearchResponse();
        response.setTotal(128L);
        response.setPage(request.getPage());
        response.setPageSize(request.getPageSize());
        response.setTookMs(45L);
        response.setItems(items);
        response.setRelatedCases(relatedCases);
        return response;
    }

    public CaseSimilarSearchResponse searchSimilarCases(CaseSimilarSearchRequest request) {
        log.info("Mock searching similar cases for: {}", request.getCaseDescription());

        List<CaseSimilarSearchResponse.SimilarCaseItem> items = new ArrayList<>();
        for (int i = 0; i < Math.min(request.getTopK(), 5); i++) {
            CaseSimilarSearchResponse.SimilarCaseItem item = new CaseSimilarSearchResponse.SimilarCaseItem();
            item.setCaseId(12345L + i);
            item.setCaseNo("(2023)沪01民终" + (4567 + i) + "号");
            item.setCaseName("李某与上海某装饰公司装饰装修合同纠纷案");
            item.setCourtLevel(3);
            item.setCourtName("上海市第一中级人民法院");
            item.setJudgeDate("2023-08-" + (10 + i));
            item.setJudgmentResult(RANDOM.nextInt(3) + 1);
            item.setLitigationAmount(new java.math.BigDecimal("180000"));
            item.setSimilarityScore(0.92 - i * 0.05);
            item.setMatchingFeatures(Map.of(
                "fact_similarity", 0.95 - i * 0.05,
                "claim_similarity", 0.88 - i * 0.03,
                "dispute_similarity", 0.90 - i * 0.04
            ));
            item.setKeyFacts("原告与被告签订装修合同，被告擅自变更材料品牌且进度滞后...");
            item.setJudgmentSummary("法院认定被告构成违约，判决解除合同，退还已付款项...");
            item.setLegalBasis(List.of("《民法典》第577条", "《建设工程施工合同司法解释》第12条"));
            item.setSourceUrl("https://wenshu.court.gov.cn/");
            item.setSourceName("中国裁判文书网");
            items.add(item);
        }

        CaseSimilarSearchResponse.CaseStatistics statistics = new CaseSimilarSearchResponse.CaseStatistics();
        statistics.setTotalCount(156);
        statistics.setWinRate(0.73);
        statistics.setAvgCompensation(new java.math.BigDecimal("156000"));

        CaseSimilarSearchResponse response = new CaseSimilarSearchResponse();
        response.setSourceCaseHash("案件描述向量指纹");
        response.setTotalSimilar(156);
        response.setItems(items);
        response.setStatistics(statistics);
        return response;
    }

    public DocumentDraftResponse draftDocument(DocumentDraftRequest request) {
        log.info("Mock drafting document with template: {}", request.getTemplateCode());

        DocumentDraftResponse response = new DocumentDraftResponse();
        response.setDocumentContent(generateMockDocument(request.getTemplateCode()));
        response.setRiskPrompt("【风险提示】\n1. 诉讼时效：建议在法定期限内提起诉讼\n2. 证据保全：建议保留好相关证据材料\n3. 管辖法院：根据被告住所地或合同约定确定");
        response.setDisclaimer("本法律文书由AI辅助生成，仅供参考。使用前请务必由具有执业资格的律师进行审核和修改。");
        response.setReferencedLaws(List.of("《中华人民共和国民法典》", "《中华人民共和国民事诉讼法》"));
        return response;
    }

    public CompanyQueryResponse queryCompany(CompanyQueryRequest request) {
        log.info("Mock querying company: {}", request.getCompanyName());

        CompanyQueryResponse response = new CompanyQueryResponse();
        response.setCompanyName(request.getCompanyName() != null ? request.getCompanyName() : "示例科技有限公司");
        response.setUnifiedSocialCreditCode("91110000XXXXXXXXXX");
        response.setLegalRepresentative("张三");
        response.setRegisteredCapital(new java.math.BigDecimal("1000"));
        response.setBusinessStatus("存续");
        response.setRegistrationAuthority("北京市市场监督管理局");
        response.setEstablishDate("2020-01-15");
        response.setDataSource("企查查 | 查询时间：" + java.time.LocalDateTime.now());

        List<CompanyQueryResponse.ShareholderInfo> shareholders = new ArrayList<>();
        shareholders.add(createShareholder("李四", "500", "50%"));
        shareholders.add(createShareholder("王五", "300", "30%"));
        shareholders.add(createShareholder("赵六", "200", "20%"));
        response.setShareholders(shareholders);

        List<CompanyQueryResponse.RiskWarning> warnings = new ArrayList<>();
        warnings.add(createRiskWarning("LOW", "经营异常", "暂时性经营异常，已申请移出", "2024-06-01"));
        response.setRiskWarnings(warnings);
        return response;
    }

    public ContractReviewResponse reviewContract(ContractReviewRequest request) {
        log.info("Mock reviewing contract, type: {}", request.getContractType());

        List<ContractReviewResponse.DimensionReview> dimensions = new ArrayList<>();
        dimensions.add(createDimension("SUBJECT_QUALIFICATION", "主体资格", 85, "主体资格合法有效"));
        dimensions.add(createDimension("CONTRACT_VALIDITY", "合同效力", 70, "合同条款基本完整，效力待确认"));
        dimensions.add(createDimension("RIGHTS_OBLIGATIONS", "权利义务", 65, "双方权利义务约定基本对等"));
        dimensions.add(createDimension("BREACH_RESPONSIBILITY", "违约责任", 60, "违约责任约定不够具体，建议补充"));
        dimensions.add(createDimension("DISPUTE_RESOLUTION", "争议解决", 80, "争议解决条款明确"));
        dimensions.add(createDimension("EXEMPTION_CLAUSE", "免责条款", 75, "免责条款基本合理"));
        dimensions.add(createDimension("INTELLECTUAL_PROPERTY", "知识产权", 90, "知识产权归属约定清晰"));
        dimensions.add(createDimension("PERSONAL_INFO", "个人信息", 85, "个人信息保护条款符合法规"));

        List<ContractReviewResponse.RiskItem> highRiskItems = new ArrayList<>();
        highRiskItems.add(createRiskItem("HIGH", "违约责任", "违约金约定过高", "可能超出法定标准，建议调整至1-2倍LPR", "降低违约金比例"));

        List<ContractReviewResponse.RiskItem> mediumRiskItems = new ArrayList<>();
        mediumRiskItems.add(createRiskItem("MEDIUM", "权利义务", "付款条件约定模糊", "可能导致争议", "明确付款条件和时间节点"));

        List<ContractReviewResponse.RiskItem> lowRiskItems = new ArrayList<>();
        lowRiskItems.add(createRiskItem("LOW", "合同效力", "部分条款表述不够清晰", "存在潜在隐患但风险可控", "优化条款表述"));

        ContractReviewResponse response = new ContractReviewResponse();
        response.setTotalScore(65);
        response.setRiskLevel("中风险");
        response.setDimensions(dimensions);
        response.setHighRiskItems(highRiskItems);
        response.setMediumRiskItems(mediumRiskItems);
        response.setLowRiskItems(lowRiskItems);
        response.setOverallComment("合同整体结构完整，但存在部分条款需要优化。建议重点关注违约金条款和付款条件的约定。");
        return response;
    }

    public DocQaResponse answerQuestion(DocQaRequest request) {
        log.info("Mock answering question: {}", request.getQuestion());

        DocQaResponse response = new DocQaResponse();
        response.setAnswer("根据检索到的法律资料，对于您提出的问题回答如下：\n\n" +
            "1. 合同欺诈的认定需要满足以下要件：\n" +
            "   - 一方存在欺诈故意\n" +
            "   - 实施了欺诈行为\n" +
            "   - 对方因此陷入错误认识\n" +
            "   - 对方基于错误认识做出意思表示\n\n" +
            "2. 相关法律依据：《民法典》第一百四十八条规定，一方以欺诈手段，使对方在违背真实意思的情况下订立的合同，受欺诈方有权请求人民法院或者仲裁机构予以撤销。");
        response.setSessionId(request.getSessionId() != null ? request.getSessionId() : UUID.randomUUID().toString());

        List<DocQaResponse.Citation> citations = new ArrayList<>();
        citations.add(createCitation("DOC-001", "CHUNK-001", "《民法典》第一百四十八条规定...", "https://flk.npc.gov.cn/", 0.95));
        response.setCitations(citations);
        return response;
    }

    private String extractKeyword(String query) {
        if (query == null || query.isEmpty()) return "法律";
        String[] words = query.replaceAll("[^\\u4e00-\u9fa5a-zA-Z0-9]", " ").split("\\s+");
        return words.length > 0 ? words[0] : "法律";
    }

    private String getTitleForQuery(String query, int index) {
        String[] titles = {"欺诈的认定", "合同的撤销", "损失赔偿", "过错责任", "履行抗辩"};
        return titles[index % titles.length];
    }

    private String getContentForQuery(String query, int index) {
        String[] contents = {
            "一方以欺诈手段，使对方在违背真实意思的情况下订立的合同，受欺诈方有权请求人民法院或者仲裁机构予以撤销。",
            "第三人实施欺诈行为，使一方陷入错误认识的，适用欺诈规定。",
            "行为人对行为内容有重大误解的，有权请求人民法院或者仲裁机构予以撤销。",
            "合同无效或者被撤销后，因该合同取得的财产，应当予以返还。",
            "当事人一方不履行合同义务或者履行合同义务不符合约定的，应当承担违约责任。"
        };
        return contents[index % contents.length];
    }

    private String generateMockDocument(String templateCode) {
        return "民事起诉状\n\n" +
            "原告：李四，男，19XX年XX月XX日出生，汉族，住北京市朝阳区。\n\n" +
            "被告：北京XX科技有限公司，住所地北京市海淀区。\n\n" +
            "法定代表人：张三。\n\n" +
            "诉讼请求：\n" +
            "1. 判令被告支付货款人民币XXXXXX元；\n" +
            "2. 判令被告支付逾期付款利息XXXX元；\n" +
            "3. 判令被告承担本案诉讼费用。\n\n" +
            "事实与理由：\n" +
            "XXXX年XX月XX日，原告与被告签订《采购合同》，约定原告向被告供应货物。\n" +
            "合同签订后，原告依约履行了供货义务，但被告尚欠货款XXXXXX元未付。\n\n" +
            "此致\n\n" +
            "北京市朝阳区人民法院\n\n" +
            "具状人：李四\n" +
            "XXXX年XX月XX日";
    }

    private CompanyQueryResponse.ShareholderInfo createShareholder(String name, String capital, String ratio) {
        CompanyQueryResponse.ShareholderInfo info = new CompanyQueryResponse.ShareholderInfo();
        info.setName(name);
        info.setCapitalContribution(capital + "万元");
        info.setRatio(ratio);
        return info;
    }

    private CompanyQueryResponse.RiskWarning createRiskWarning(String level, String type, String desc, String date) {
        CompanyQueryResponse.RiskWarning warning = new CompanyQueryResponse.RiskWarning();
        warning.setLevel(level);
        warning.setType(type);
        warning.setDescription(desc);
        warning.setDate(date);
        return warning;
    }

    private ContractReviewResponse.DimensionReview createDimension(String code, String name, int score, String comment) {
        ContractReviewResponse.DimensionReview dim = new ContractReviewResponse.DimensionReview();
        dim.setDimensionCode(code);
        dim.setDimensionName(name);
        dim.setScore(score);
        dim.setComment(comment);
        return dim;
    }

    private ContractReviewResponse.RiskItem createRiskItem(String level, String dim, String title, String desc, String suggestion) {
        ContractReviewResponse.RiskItem item = new ContractReviewResponse.RiskItem();
        item.setLevel(level);
        item.setDimension(dim);
        item.setTitle(title);
        item.setDescription(desc);
        item.setSuggestion(suggestion);
        return item;
    }

    private DocQaResponse.Citation createCitation(String docId, String chunkId, String content, String url, double score) {
        DocQaResponse.Citation citation = new DocQaResponse.Citation();
        citation.setDocumentId(docId);
        citation.setChunkId(chunkId);
        citation.setContent(content);
        citation.setSourceUrl(url);
        citation.setScore(score);
        return citation;
    }
}