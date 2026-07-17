package com.legalai.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalai.config.ElasticsearchConfig;
import com.legalai.dto.*;
import com.legalai.model.LegalCase;
import com.legalai.model.TbCase;
import com.legalai.repository.LegalCaseMapper;
import com.legalai.repository.TbCaseMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CaseSearchService {
    private static final Logger log = LoggerFactory.getLogger(CaseSearchService.class);

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Autowired
    private ElasticsearchConfig esConfig;

    @Autowired
    private AIService aiService;

    @Autowired
    private LegalCaseMapper legalCaseMapper;

    @Autowired
    private TbCaseMapper tbCaseMapper;

    public CaseSearchResponse searchCases(CaseSearchRequest request) {
        log.info("案例查询请求: keyword={}, caseType={}, courtLevel={}",
            request.getKeyword(), request.getCaseType(), request.getCourtLevel());

        validateRequest(request);

        CaseSearchResponse dbResponse = dbSearchCases(request);
        if (dbResponse != null && dbResponse.getTotal() > 0) {
            return dbResponse;
        }

        return esSearchCases(request);
    }

    private CaseSearchResponse dbSearchCases(CaseSearchRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            QueryWrapper<LegalCase> queryWrapper = new QueryWrapper<>();

            if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
                queryWrapper.and(w -> w.like("title", request.getKeyword())
                    .or()
                    .like("case_no", request.getKeyword())
                    .or()
                    .like("case_cause", request.getKeyword())
                    .or()
                    .like("court", request.getKeyword()));
            }

            if (request.getCaseType() != null && !request.getCaseTypeName().isEmpty()) {
                queryWrapper.eq("case_type", request.getCaseTypeName());
            }

            if (request.getCourtLevel() != null) {
                queryWrapper.eq("court_level", request.getCourtLevel());
            }

            queryWrapper.orderByDesc("judgment_date", "created_at");

            List<LegalCase> legalCases = legalCaseMapper.selectList(queryWrapper);

            if (legalCases == null || legalCases.isEmpty()) {
                return null;
            }

            List<CaseSearchResponse.CaseSearchItem> items = legalCases.stream()
                .map(this::convertToCaseSearchItem)
                .collect(Collectors.toList());

            int total = items.size();
            int from = (request.getPage() - 1) * request.getPageSize();
            int to = Math.min(from + request.getPageSize(), total);

            List<CaseSearchResponse.CaseSearchItem> pagedItems = total > from
                ? items.subList(from, to)
                : Collections.emptyList();

            CaseSearchResponse response = new CaseSearchResponse();
            response.setTotal((long) total);
            response.setPage(request.getPage());
            response.setPageSize(request.getPageSize());
            response.setTookMs(System.currentTimeMillis() - startTime);
            response.setItems(pagedItems);

            return response;

        } catch (Exception e) {
            log.error("数据库查询案例失败: {}", e.getMessage());
            return null;
        }
    }

    private CaseSearchResponse.CaseSearchItem convertToCaseSearchItem(LegalCase legalCase) {
        CaseSearchResponse.CaseSearchItem item = new CaseSearchResponse.CaseSearchItem();
        item.setCaseUuid(legalCase.getCaseUuid());
        item.setCaseNo(legalCase.getCaseNo());
        item.setTitle(legalCase.getTitle());
        item.setCourt(legalCase.getCourt());
        item.setCaseType(legalCase.getCaseType());
        item.setCaseCause(legalCase.getCaseCause());
        item.setJudgeDate(legalCase.getJudgmentDate() != null ? legalCase.getJudgmentDate().toString() : "");
        item.setSummary(legalCase.getSummary());
        item.setSourceUrl(legalCase.getSourceUrl());
        item.setSourceName(legalCase.getSourceName());
        return item;
    }

    private void validateRequest(CaseSearchRequest request) {
        if (request.getKeyword() != null && request.getKeyword().length() > 200) {
            throw new IllegalArgumentException("关键词长度不能超过200字");
        }
    }

    private CaseSearchResponse esSearchCases(CaseSearchRequest request) {
        long startTime = System.currentTimeMillis();

        if (!esConfig.isEnabled() || !elasticsearchService.isAvailable()) {
            log.info("ES不可用，使用AI生成案例检索结果");
            return aiGenerateCases(request, startTime);
        }

        Map<String, Object> filters = buildFilters(request);

        List<LegalSearchResponse.SearchResultItem> esResults;
        try {
            esResults = elasticsearchService.searchByES(
                request.getKeyword(),
                request.getPage(),
                request.getPageSize(),
                filters
            );
        } catch (Exception e) {
            log.warn("ES检索失败: {}，使用AI生成", e.getMessage());
            return aiGenerateCases(request, startTime);
        }

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

    private CaseSearchResponse aiGenerateCases(CaseSearchRequest request, long startTime) {
        log.info("使用AI生成案例检索结果: keyword={}", request.getKeyword());

        String prompt = buildCaseSearchPrompt(request);

        try {
            String aiResponse = aiService.chat(prompt);
            return parseAIResponse(aiResponse, request, startTime);
        } catch (IOException e) {
            log.error("AI生成案例失败: {}", e.getMessage());
            throw new IllegalStateException("AI生成案例失败，且数据库和Elasticsearch均无可用数据，请稍后重试");
        }
    }

    private String buildCaseSearchPrompt(CaseSearchRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位专业的法律案例检索专家。请使用网络搜索能力，查找与用户关键词相关的司法案例。\n\n");
        sb.append("【检索关键词】").append(request.getKeyword() != null ? request.getKeyword() : "").append("\n\n");

        if (request.getCaseType() != null) {
            sb.append("【案件类型】").append(getCaseTypeName(request.getCaseType())).append("\n\n");
        }
        if (request.getCourtLevel() != null) {
            sb.append("【法院层级】").append(getCourtLevelName(request.getCourtLevel())).append("\n\n");
        }

        sb.append("请执行搜索，查找真实的司法案例：\n");
        sb.append("1. 搜索相关案例的案号、法院、案件类型\n");
        sb.append("2. 搜索案例的裁判结果和日期\n\n");

        sb.append("请返回最相关的10个案例，采用JSON数组格式：\n\n");
        sb.append("[\n");
        sb.append("  {\n");
        sb.append("    \"caseUuid\": \"CASE-2023-001\",\n");
        sb.append("    \"caseNo\": \"(2023)沪01民终1234号\",\n");
        sb.append("    \"title\": \"某公司与某劳动者合同纠纷案\",\n");
        sb.append("    \"court\": \"上海市第一中级人民法院\",\n");
        sb.append("    \"caseType\": \"民事\",\n");
        sb.append("    \"caseCause\": \"合同纠纷\",\n");
        sb.append("    \"judgeDate\": \"2023-08-15\",\n");
        sb.append("    \"trialProcedure\": \"二审\",\n");
        sb.append("    \"judgmentResult\": 2,\n");
        sb.append("    \"litigationAmount\": 180000,\n");
        sb.append("    \"summary\": \"法院认定被告构成违约，判决支持原告诉讼请求。\",\n");
        sb.append("    \"sourceUrl\": \"https://wenshu.court.gov.cn/\",\n");
        sb.append("    \"sourceName\": \"中国裁判文书网\"\n");
        sb.append("  }\n");
        sb.append("]\n\n");
        sb.append("judgmentResult说明：1=全部支持，2=部分支持，3=驳回，4=调解，5=撤诉。\n");
        sb.append("只返回JSON数组，不要有其他解释性文字。");

        return sb.toString();
    }

    private String getCourtLevelName(Integer level) {
        if (level == null) return "全部";
        return switch (level) {
            case 1 -> "基层法院";
            case 2 -> "中级人民法院";
            case 3 -> "高级人民法院";
            case 4 -> "最高人民法院";
            default -> "全部";
        };
    }

    private CaseSearchResponse parseAIResponse(String aiResponse, CaseSearchRequest request, long startTime) {
        List<CaseSearchResponse.CaseSearchItem> items = new ArrayList<>();

        String jsonContent = extractJsonFromResponse(aiResponse);
        if (jsonContent == null || jsonContent.isEmpty()) {
            log.error("无法从AI响应中提取JSON内容");
            throw new IllegalStateException("AI响应无效，无法解析案例数据");
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(jsonContent);

            if (node.isArray()) {
                for (JsonNode item : node) {
                    CaseSearchResponse.CaseSearchItem caseItem = parseCaseItem(item);
                    if (caseItem != null && caseItem.getTitle() != null && !caseItem.getTitle().isEmpty()) {
                        items.add(caseItem);
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析AI案例响应失败: {}", e.getMessage());
        }

        if (items.isEmpty()) {
            throw new IllegalStateException("AI未返回有效案例数据");
        }

        for (CaseSearchResponse.CaseSearchItem item : items) {
            saveCaseToDatabase(item);
        }

        int from = (request.getPage() - 1) * request.getPageSize();
        int to = Math.min(from + request.getPageSize(), items.size());
        List<CaseSearchResponse.CaseSearchItem> pagedItems = from < items.size() ? items.subList(from, to) : java.util.Collections.emptyList();

        CaseSearchResponse response = new CaseSearchResponse();
        response.setTotal((long) items.size());
        response.setPage(request.getPage());
        response.setPageSize(request.getPageSize());
        response.setTookMs(System.currentTimeMillis() - startTime);
        response.setItems(pagedItems);

        return response;
    }

    private CaseSearchResponse.CaseSearchItem parseCaseItem(JsonNode item) {
        try {
            CaseSearchResponse.CaseSearchItem caseItem = new CaseSearchResponse.CaseSearchItem();
            caseItem.setCaseUuid(item.has("caseUuid") ? item.get("caseUuid").asText() : "AI-" + System.currentTimeMillis());
            caseItem.setCaseNo(item.has("caseNo") ? item.get("caseNo").asText() : "");
            caseItem.setTitle(item.has("title") ? item.get("title").asText() : "");
            caseItem.setCourt(item.has("court") ? item.get("court").asText() : "");
            caseItem.setCaseType(item.has("caseType") ? item.get("caseType").asText() : "民事");
            caseItem.setCaseCause(item.has("caseCause") ? item.get("caseCause").asText() : "");
            caseItem.setJudgeDate(item.has("judgeDate") ? item.get("judgeDate").asText() : "");
            caseItem.setTrialProcedure(item.has("trialProcedure") ? item.get("trialProcedure").asText() : "");
            caseItem.setJudgmentResult(item.has("judgmentResult") ? item.get("judgmentResult").asInt() : 1);
            caseItem.setLitigationAmount(item.has("litigationAmount") ? item.get("litigationAmount").asLong() : 0L);
            caseItem.setSummary(item.has("summary") ? item.get("summary").asText() : "");
            caseItem.setSourceUrl(item.has("sourceUrl") ? item.get("sourceUrl").asText() : "https://wenshu.court.gov.cn/");
            caseItem.setSourceName(item.has("sourceName") ? item.get("sourceName").asText() : "中国裁判文书网");
            return caseItem;
        } catch (Exception e) {
            log.error("解析案例项失败: {}", e.getMessage());
            return null;
        }
    }

    private String extractJsonFromResponse(String response) {
        if (response == null || response.isEmpty()) {
            return null;
        }

        String trimmed = response.trim();

        int jsonStart = trimmed.indexOf("[");
        int jsonEnd = trimmed.lastIndexOf("]");
        if (jsonStart == -1 || jsonEnd == -1 || jsonStart > jsonEnd) {
            jsonStart = trimmed.indexOf("{");
            jsonEnd = trimmed.lastIndexOf("}");
        }

        if (jsonStart != -1 && jsonEnd != -1 && jsonStart < jsonEnd) {
            String json = trimmed.substring(jsonStart, jsonEnd + 1);
            json = json.replaceAll("```json\\s*", "").replaceAll("```\\s*", "");
            return json;
        }

        return trimmed;
    }

    private void saveCaseToDatabase(CaseSearchResponse.CaseSearchItem item) {
        try {
            QueryWrapper<LegalCase> checkWrapper = new QueryWrapper<>();
            checkWrapper.eq("case_uuid", item.getCaseUuid());
            LegalCase existing = legalCaseMapper.selectOne(checkWrapper);

            if (existing != null) {
                log.info("案例已存在，跳过保存: {}", item.getTitle());
                return;
            }

            LegalCase legalCase = new LegalCase();
            legalCase.setCaseUuid(item.getCaseUuid());
            legalCase.setCaseNo(item.getCaseNo());
            legalCase.setTitle(item.getTitle());
            legalCase.setCourt(item.getCourt());
            legalCase.setCaseType(item.getCaseType());
            legalCase.setCaseCause(item.getCaseCause());
            if (item.getJudgeDate() != null && !item.getJudgeDate().isEmpty()) {
                try {
                    legalCase.setJudgmentDate(java.time.LocalDate.parse(item.getJudgeDate()));
                } catch (Exception e) {
                    log.debug("解析裁判日期失败: {}", item.getJudgeDate());
                }
            }
            legalCase.setSummary(item.getSummary());
            legalCase.setSourceUrl(item.getSourceUrl());
            legalCase.setSourceName(item.getSourceName());

            legalCaseMapper.insert(legalCase);
            log.info("案例已保存到数据库: {}", item.getTitle());

        } catch (Exception e) {
            log.error("保存案例到数据库失败: {}", e.getMessage());
        }
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

    public CaseSearchResponse.CaseSearchItem getCaseDetail(String caseUuid) {
        log.info("获取案例详情: caseUuid={}", caseUuid);

        try {
            QueryWrapper<TbCase> tbQuery = new QueryWrapper<>();
            tbQuery.eq("case_uuid", caseUuid);
            TbCase tbCase = tbCaseMapper.selectOne(tbQuery);

            if (tbCase != null) {
                return convertTbCaseToItem(tbCase);
            }
        } catch (Exception e) {
            log.error("查询tb_case详情失败: {}", e.getMessage());
        }

        try {
            QueryWrapper<LegalCase> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("case_uuid", caseUuid);
            LegalCase legalCase = legalCaseMapper.selectOne(queryWrapper);

            if (legalCase != null) {
                return convertToCaseSearchItem(legalCase);
            }
        } catch (Exception e) {
            log.error("数据库查询案例详情失败: {}", e.getMessage());
        }

        throw new IllegalStateException("案例未找到，caseUuid: " + caseUuid);
    }

    private CaseSearchResponse.CaseSearchItem convertTbCaseToItem(TbCase tbCase) {
        CaseSearchResponse.CaseSearchItem item = new CaseSearchResponse.CaseSearchItem();
        item.setCaseUuid(tbCase.getCaseUuid());
        item.setCaseNo(tbCase.getCaseNo());
        item.setTitle(tbCase.getCaseName());
        item.setCourt(tbCase.getCourtName());
        item.setCourtLevel(tbCase.getCourtLevel());
        item.setCaseType(getCaseTypeName(tbCase.getCaseType()));
        item.setCaseCause(tbCase.getCaseCause());
        item.setJudgeDate(tbCase.getJudgeDate() != null ? tbCase.getJudgeDate().toString() : "");
        item.setTrialProcedure(tbCase.getTrialProcedure());
        item.setJudgmentResult(tbCase.getJudgmentResult());
        item.setLitigationAmount(tbCase.getLitigationAmount() != null ? tbCase.getLitigationAmount().longValue() : 0L);
        item.setKeyFacts(tbCase.getKeyFacts());
        item.setJudgmentSummary(tbCase.getJudgmentSummary());
        item.setLegalBasis(tbCase.getLegalBasis());
        item.setSourceUrl("https://wenshu.court.gov.cn/");
        item.setSourceName("中国裁判文书网");
        return item;
    }
}