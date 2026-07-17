package com.legalai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalai.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ContractService {
    private static final Logger log = LoggerFactory.getLogger(ContractService.class);

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

    public ContractReviewResponse reviewContract(ContractReviewRequest request) {
        log.info("合同审查请求: contractType={}, contractName={}",
            request.getContractType(), request.getContractName());

        validateRequest(request);

        ContractReviewResponse response = aiReviewContract(request);
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

    private ContractReviewResponse aiReviewContract(ContractReviewRequest request) {
        log.info("调用MiniMax AI进行合同审查...");

        String prompt = buildContractReviewPrompt(request);

        try {
            String aiResponse = aiService.chat(prompt);
            return parseAIResponse(aiResponse, request);
        } catch (IOException e) {
            log.error("AI合同审查失败: {}", e.getMessage());
            throw new IllegalStateException("AI合同审查服务不可用，请稍后重试", e);
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
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(aiResponse);

            response.setTotalScore(node.has("totalScore") ? node.get("totalScore").asInt() : 75);
            response.setRiskLevel(node.has("riskLevel") ? node.get("riskLevel").asText() : "MEDIUM");
            response.setOverallComment(node.has("overallComment") ? node.get("overallComment").asText() : "AI审查完成");

            List<ContractReviewResponse.DimensionReview> dimensions = new ArrayList<>();
            if (node.has("dimensions")) {
                for (JsonNode dim : node.get("dimensions")) {
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
            throw new IllegalStateException("AI响应格式解析失败，请稍后重试", e);
        }

        return response;
    }

    private List<ContractReviewResponse.RiskItem> parseRiskItems(JsonNode node, String fieldName, String level) {
        List<ContractReviewResponse.RiskItem> items = new ArrayList<>();
        if (node.has(fieldName)) {
            for (JsonNode item : node.get(fieldName)) {
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

    private String getDimensionName(String code) {
        return switch (code) {
            case "SUBJECT_QUALIFICATION" -> "主体资格";
            case "CONTRACT_VALIDITY" -> "合同效力";
            case "RIGHTS_OBLIGATIONS" -> "权利义务";
            case "BREACH_RESPONSIBILITY" -> "违约责任";
            case "DISPUTE_RESOLUTION" -> "争议解决";
            case "EXEMPTION_CLAUSE" -> "免责条款";
            case "INTELLECTIAL_PROPERTY" -> "知识产权";
            case "PERSONAL_INFO" -> "个人信息";
            default -> code;
        };
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