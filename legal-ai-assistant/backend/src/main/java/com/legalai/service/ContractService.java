package com.legalai.service;

import com.legalai.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ContractService {
    private static final Logger log = LoggerFactory.getLogger(ContractService.class);

    @Value("${mock.enabled:true}")
    private boolean mockEnabled;

    @Autowired
    private AIService aiService;

    @Autowired
    private ContractReviewStore reviewStore;

    private static final Map<String, Integer> DIMENSION_WEIGHTS = Map.of(
        "SUBJECT_QUALIFICATION", 15,
        "CONTRACT_VALIDITY", 20,
        "RIGHTS_OBLIGATIONS", 15,
        "BREACH_RESPONSIBILITY", 15,
        "DISPUTE_RESOLUTION", 10,
        "EXEMPTION_CLAUSE", 10,
        "INTELLECTUAL_PROPERTY", 8,
        "PERSONAL_INFO", 7
    );

    private static final Pattern PARTY_PATTERN = Pattern.compile("(甲方|乙方|出租方|承租方|买方|卖方|债权人|债务人)\\s*[：:]?\\s*(\\S+)");
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(\\d+(?:,\\d{3})*(?:\\.\\d+)?)\\s*(?:元|万元|万)");
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4})\\s*年\\s*(\\d{1,2})\\s*月\\s*(\\d{1,2})\\s*日");

    public ContractReviewResponse reviewContract(ContractReviewRequest request) {
        log.info("合同审查请求: contractType={}, contractName={}",
            request.getContractType(), request.getContractName());

        validateRequest(request);

        ContractReviewResponse response;
        if (mockEnabled) {
            response = mockReviewContract(request);
        } else {
            response = aiReviewContract(request);
        }
        response.setCreatedAt(System.currentTimeMillis());
        reviewStore.save(response);
        return response;
    }

    public ContractReviewResponse getReview(String uuid) {
        return reviewStore.get(uuid);
    }

    public List<ContractReviewResponse> listRecent(int limit) {
        return reviewStore.listRecent(limit);
    }

    private void validateRequest(ContractReviewRequest request) {
        if (request.getContractText() == null || request.getContractText().trim().isEmpty()) {
            throw new IllegalArgumentException("合同内容不能为空");
        }
        if (request.getContractText().length() > 50000) {
            throw new IllegalArgumentException("合同内容过长，不能超过50000字");
        }
    }

    private ContractReviewResponse mockReviewContract(ContractReviewRequest request) {
        String contractText = request.getContractText();

        Map<String, Integer> dimensionScores = analyzeContractDimensions(contractText, request.getContractType());

        List<ContractReviewResponse.DimensionReview> dimensions = buildDimensionReviews(dimensionScores);

        List<ContractReviewResponse.RiskItem> riskItems = identifyRiskItems(contractText, dimensionScores);

        List<ContractReviewResponse.RiskItem> highRiskItems = riskItems.stream()
            .filter(r -> "HIGH".equals(r.getLevel()))
            .collect(Collectors.toList());
        List<ContractReviewResponse.RiskItem> mediumRiskItems = riskItems.stream()
            .filter(r -> "MEDIUM".equals(r.getLevel()))
            .collect(Collectors.toList());
        List<ContractReviewResponse.RiskItem> lowRiskItems = riskItems.stream()
            .filter(r -> "LOW".equals(r.getLevel()))
            .collect(Collectors.toList());

        int totalScore = calculateTotalScore(dimensionScores);
        String riskLevel = determineRiskLevel(totalScore);

        ContractReviewResponse response = new ContractReviewResponse();
        response.setContractName(request.getContractName());
        response.setContractType(request.getContractType());
        response.setTotalScore(totalScore);
        response.setRiskLevel(riskLevel);
        response.setDimensions(dimensions);
        response.setHighRiskItems(highRiskItems);
        response.setMediumRiskItems(mediumRiskItems);
        response.setLowRiskItems(lowRiskItems);
        response.setOverallComment(generateOverallComment(totalScore, riskItems));

        return response;
    }

    private ContractReviewResponse aiReviewContract(ContractReviewRequest request) {
        log.info("调用MiniMax AI进行合同审查...");

        String prompt = buildContractReviewPrompt(request);

        try {
            String aiResponse = aiService.chat(prompt);
            return parseAIResponse(aiResponse, request);
        } catch (IOException e) {
            log.error("AI合同审查失败: {}", e.getMessage());
            return mockReviewContract(request);
        }
    }

    private String buildContractReviewPrompt(ContractReviewRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位专业的合同法律审查专家。请审查以下合同文本，并给出专业的审查意见。\n\n");
        sb.append("【合同类型】").append(request.getContractType() != null ? request.getContractType() : "一般合同").append("\n\n");
        sb.append("【合同名称】").append(request.getContractName() != null ? request.getContractName() : "未命名合同").append("\n\n");
        sb.append("【合同文本】\n").append(request.getContractText()).append("\n\n");
        sb.append("请从以下维度进行审查并输出JSON格式结果：\n");
        sb.append("1. 主体资格(SUBJECT_QUALIFICATION)：合同当事人的资质和能力\n");
        sb.append("2. 合同效力(CONTRACT_VALIDITY)：合同的有效性和生效条件\n");
        sb.append("3. 权利义务(RIGHTS_OBLIGATIONS)：双方权利义务约定的合理性\n");
        sb.append("4. 违约责任(BREACH_RESPONSIBILITY)：违约责任条款的完整性\n");
        sb.append("5. 争议解决(DISPUTE_RESOLUTION)：争议解决条款的约定\n");
        sb.append("6. 免责条款(EXEMPTION_CLAUSE)：免责条款的合理性\n");
        sb.append("7. 知识产权(INTELLECTUAL_PROPERTY)：知识产权相关约定\n");
        sb.append("8. 个人信息网(PERSONAL_INFO)：个人信息保护条款\n\n");
        sb.append("输出格式要求：\n");
        sb.append("请直接输出JSON格式，不要有其他内容：\n");
        sb.append("{\n");
        sb.append("  \"totalScore\": 85,\n");
        sb.append("  \"riskLevel\": \"LOW\",\n");
        sb.append("  \"dimensions\": [\n");
        sb.append("    {\"dimensionCode\": \"SUBJECT_QUALIFICATION\", \"score\": 85, \"comment\": \"评价\"},\n");
        sb.append("    ...\n");
        sb.append("  ],\n");
        sb.append("  \"highRiskItems\": [{\"level\": \"HIGH\", \"dimension\": \"违约责任\", \"title\": \"问题标题\", \"description\": \"问题描述\", \"suggestion\": \"修改建议\"}],\n");
        sb.append("  \"mediumRiskItems\": [...],\n");
        sb.append("  \"lowRiskItems\": [...],\n");
        sb.append("  \"overallComment\": \"总体评价\"\n");
        sb.append("}\n\n");
        sb.append("评分标准：80-100为LOW风险，60-79为MEDIUM风险，60以下为HIGH风险。");

        return sb.toString();
    }

    private ContractReviewResponse parseAIResponse(String aiResponse, ContractReviewRequest request) {
        ContractReviewResponse response = new ContractReviewResponse();
        response.setContractName(request.getContractName());
        response.setContractType(request.getContractType());

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(aiResponse);

            response.setTotalScore(node.has("totalScore") ? node.get("totalScore").asInt() : 75);
            response.setRiskLevel(node.has("riskLevel") ? node.get("riskLevel").asText() : "MEDIUM");
            response.setOverallComment(node.has("overallComment") ? node.get("overallComment").asText() : "AI审查完成");

            java.util.List<ContractReviewResponse.DimensionReview> dimensions = new java.util.ArrayList<>();
            if (node.has("dimensions")) {
                for (com.fasterxml.jackson.databind.JsonNode dim : node.get("dimensions")) {
                    ContractReviewResponse.DimensionReview d = new ContractReviewResponse.DimensionReview();
                    d.setDimensionCode(dim.get("dimensionCode").asText());
                    d.setDimensionName(getDimensionName(dim.get("dimensionCode").asText()));
                    d.setScore(dim.has("score") ? dim.get("score").asInt() : 70);
                    d.setComment(dim.has("comment") ? dim.get("comment").asText() : "");
                    dimensions.add(d);
                }
            }
            response.setDimensions(dimensions);

            response.setHighRiskItems(parseRiskItems(node, "highRiskItems", "HIGH"));
            response.setMediumRiskItems(parseRiskItems(node, "mediumRiskItems", "MEDIUM"));
            response.setLowRiskItems(parseRiskItems(node, "lowRiskItems", "LOW"));

        } catch (Exception e) {
            log.error("解析AI响应失败: {}", e.getMessage());
            return mockReviewContract(request);
        }

        return response;
    }

    private java.util.List<ContractReviewResponse.RiskItem> parseRiskItems(com.fasterxml.jackson.databind.JsonNode node, String fieldName, String level) {
        java.util.List<ContractReviewResponse.RiskItem> items = new java.util.ArrayList<>();
        if (node.has(fieldName)) {
            for (com.fasterxml.jackson.databind.JsonNode item : node.get(fieldName)) {
                ContractReviewResponse.RiskItem ri = new ContractReviewResponse.RiskItem();
                ri.setLevel(level);
                ri.setDimension(item.has("dimension") ? item.get("dimension").asText() : "");
                ri.setTitle(item.has("title") ? item.get("title").asText() : "");
                ri.setDescription(item.has("description") ? item.get("description").asText() : "");
                ri.setSuggestion(item.has("suggestion") ? item.get("suggestion").asText() : "");
                items.add(ri);
            }
        }
        return items;
    }

    private Map<String, Integer> analyzeContractDimensions(String text, String contractType) {
        Map<String, Integer> scores = new HashMap<>();

        scores.put("SUBJECT_QUALIFICATION", analyzeSubjectQualification(text));
        scores.put("CONTRACT_VALIDITY", analyzeContractValidity(text));
        scores.put("RIGHTS_OBLIGATIONS", analyzeRightsObligations(text));
        scores.put("BREACH_RESPONSIBILITY", analyzeBreachResponsibility(text));
        scores.put("DISPUTE_RESOLUTION", analyzeDisputeResolution(text));
        scores.put("EXEMPTION_CLAUSE", analyzeExemptionClause(text));
        scores.put("INTELLECTUAL_PROPERTY", analyzeIntellectualProperty(text));
        scores.put("PERSONAL_INFO", analyzePersonalInfo(text));

        return scores;
    }

    private int analyzeSubjectQualification(String text) {
        var partyMatcher = PARTY_PATTERN.matcher(text);
        int partyCount = 0;
        while (partyMatcher.find()) {
            partyCount++;
        }

        if (partyCount >= 2) {
            return 85 + (int)(Math.random() * 10);
        }
        return 60 + (int)(Math.random() * 20);
    }

    private int analyzeContractValidity(String text) {
        boolean hasAmount = AMOUNT_PATTERN.matcher(text).find();
        boolean hasDate = DATE_PATTERN.matcher(text).find();

        int score = 70;
        if (hasAmount) score += 10;
        if (hasDate) score += 10;

        return Math.min(score + (int)(Math.random() * 10), 95);
    }

    private int analyzeRightsObligations(String text) {
        int score = 65 + (int)(Math.random() * 15);

        if (text.contains("权利") && text.contains("义务")) {
            score += 10;
        }
        if (text.contains("甲方") && text.contains("乙方")) {
            score += 5;
        }

        return Math.min(score, 90);
    }

    private int analyzeBreachResponsibility(String text) {
        int score = 60 + (int)(Math.random() * 15);

        if (text.contains("违约金")) {
            if (text.contains("1倍") || text.contains("2倍") || text.contains("LPR")) {
                score += 15;
            } else {
                score -= 10;
            }
        }

        if (text.contains("损失赔偿")) {
            score += 10;
        }

        return Math.min(Math.max(score, 0), 100);
    }

    private int analyzeDisputeResolution(String text) {
        int score = 70 + (int)(Math.random() * 15);

        if (text.contains("仲裁")) {
            score += 10;
        } else if (text.contains("起诉") || text.contains("诉讼")) {
            score += 5;
        }

        if (text.contains("管辖")) {
            score += 5;
        }

        return Math.min(score, 95);
    }

    private int analyzeExemptionClause(String text) {
        int score = 75 + (int)(Math.random() * 10);

        if (text.contains("不可抗力")) {
            score += 10;
        }

        if (text.contains("免责")) {
            score += 5;
        }

        return Math.min(score, 95);
    }

    private int analyzeIntellectualProperty(String text) {
        int score = 85 + (int)(Math.random() * 10);

        if (text.contains("知识产权") || text.contains("专利") || text.contains("商标")) {
            score += 5;
        }

        return Math.min(score, 98);
    }

    private int analyzePersonalInfo(String text) {
        int score = 80 + (int)(Math.random() * 10);

        if (text.contains("个人信息") || text.contains("隐私")) {
            score += 10;
        }

        return Math.min(score, 95);
    }

    private List<ContractReviewResponse.DimensionReview> buildDimensionReviews(Map<String, Integer> scores) {
        return scores.entrySet().stream()
            .map(entry -> {
                ContractReviewResponse.DimensionReview dim = new ContractReviewResponse.DimensionReview();
                dim.setDimensionCode(entry.getKey());
                dim.setDimensionName(getDimensionName(entry.getKey()));
                dim.setScore(entry.getValue());
                dim.setComment(getDimensionComment(entry.getKey(), entry.getValue()));
                return dim;
            })
            .collect(Collectors.toList());
    }

    private String getDimensionName(String code) {
        return switch (code) {
            case "SUBJECT_QUALIFICATION" -> "主体资格";
            case "CONTRACT_VALIDITY" -> "合同效力";
            case "RIGHTS_OBLIGATIONS" -> "权利义务";
            case "BREACH_RESPONSIBILITY" -> "违约责任";
            case "DISPUTE_RESOLUTION" -> "争议解决";
            case "EXEMPTION_CLAUSE" -> "免责条款";
            case "INTELLECTUAL_PROPERTY" -> "知识产权";
            case "PERSONAL_INFO" -> "个人信息";
            default -> code;
        };
    }

    private String getDimensionComment(String code, int score) {
        if (score >= 80) {
            return "该维度条款约定合理，风险较低";
        } else if (score >= 60) {
            return "该维度存在一定风险，建议优化";
        } else {
            return "该维度风险较高，建议重点关注";
        }
    }

    private List<ContractReviewResponse.RiskItem> identifyRiskItems(String text, Map<String, Integer> scores) {
        List<ContractReviewResponse.RiskItem> items = new ArrayList<>();

        if (scores.getOrDefault("BREACH_RESPONSIBILITY", 0) < 70) {
            if (text.contains("违约金") && !text.contains("LPR") && !text.contains("1倍") && !text.contains("2倍")) {
                items.add(createRiskItem("HIGH", "违约责任", "违约金约定不规范",
                    "违约金约定可能超出法定标准，存在被法院调整的风险",
                    "建议将违约金约定为不超实际损失30%或按LPR的1-4倍计算"));
            }
        }

        if (scores.getOrDefault("RIGHTS_OBLIGATIONS", 0) < 65) {
            items.add(createRiskItem("MEDIUM", "权利义务", "付款条件约定模糊",
                "付款条件和时间节点不够明确，可能导致争议",
                "建议明确付款条件、付款方式和付款时间节点"));
        }

        if (scores.getOrDefault("CONTRACT_VALIDITY", 0) < 70) {
            items.add(createRiskItem("MEDIUM", "合同效力", "合同生效条件不明确",
                "合同生效条件约定不够清晰",
                "建议明确合同生效的时间和条件"));
        }

        if (scores.getOrDefault("DISPUTE_RESOLUTION", 0) < 70) {
            items.add(createRiskItem("LOW", "争议解决", "争议解决条款待完善",
                "争议解决方式约定不够具体",
                "建议明确争议解决方式（诉讼/仲裁）和管辖法院"));
        }

        return items;
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

    private int calculateTotalScore(Map<String, Integer> scores) {
        int weightedSum = 0;
        int totalWeight = 0;

        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            int weight = DIMENSION_WEIGHTS.getOrDefault(entry.getKey(), 10);
            weightedSum += entry.getValue() * weight;
            totalWeight += weight;
        }

        return totalWeight > 0 ? weightedSum / totalWeight : 0;
    }

    private String determineRiskLevel(int score) {
        if (score >= 80) {
            return "LOW";
        } else if (score >= 60) {
            return "MEDIUM";
        } else {
            return "HIGH";
        }
    }

    private String generateOverallComment(int score, List<ContractReviewResponse.RiskItem> risks) {
        StringBuilder sb = new StringBuilder();

        if (score >= 80) {
            sb.append("合同整体质量良好，风险较低。");
        } else if (score >= 60) {
            sb.append("合同整体结构基本完整，但存在部分条款需要优化。");
        } else {
            sb.append("合同存在较多风险点，建议进行全面修订。");
        }

        long highRiskCount = risks.stream().filter(r -> "HIGH".equals(r.getLevel())).count();
        if (highRiskCount > 0) {
            sb.append("其中").append(highRiskCount).append("项高风险需要重点关注。");
        }

        return sb.toString();
    }

    public List<Map<String, String>> getDimensions() {
        return DIMENSION_WEIGHTS.entrySet().stream()
            .map(entry -> {
                Map<String, String> dim = new HashMap<>();
                dim.put("code", entry.getKey());
                dim.put("name", getDimensionName(entry.getKey()));
                dim.put("weight", entry.getValue() + "%");
                return dim;
            })
            .collect(Collectors.toList());
    }
}