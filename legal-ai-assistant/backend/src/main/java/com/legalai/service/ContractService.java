package com.legalai.service;

import com.legalai.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ContractService {
    private static final Logger log = LoggerFactory.getLogger(ContractService.class);

    public ContractReviewResponse reviewContract(ContractReviewRequest request) {
        log.info("合同审查请求: contractType={}", request.getContractType());

        List<ContractReviewResponse.DimensionReview> dimensions = new ArrayList<>();
        dimensions.add(createDimension("SUBJECT_QUALIFICATION", "主体资格", 85, "主体资格合法有效"));
        dimensions.add(createDimension("CONTRACT_VALIDITY", "合同效力", 70, "合同条款基本完整，效力待确认"));
        dimensions.add(createDimension("RIGHTS_OBLIGATIONS", "权利义务", 65, "双方权利义务约定基本对等"));
        dimensions.add(createDimension("BREACH_RESPONSIBILITY", "违约责任", 60, "违约责任约定不够具体"));
        dimensions.add(createDimension("DISPUTE_RESOLUTION", "争议解决", 80, "争议解决条款明确"));
        dimensions.add(createDimension("EXEMPTION_CLAUSE", "免责条款", 75, "免责条款基本合理"));
        dimensions.add(createDimension("INTELLECTUAL_PROPERTY", "知识产权", 90, "知识产权归属约定清晰"));
        dimensions.add(createDimension("PERSONAL_INFO", "个人信息", 85, "个人信息保护条款符合法规"));

        List<ContractReviewResponse.RiskItem> highRiskItems = new ArrayList<>();
        highRiskItems.add(createRiskItem("HIGH", "违约责任", "违约金约定过高",
            "可能超出法定标准，建议调整至1-2倍LPR", "降低违约金比例至法定范围内"));

        List<ContractReviewResponse.RiskItem> mediumRiskItems = new ArrayList<>();
        mediumRiskItems.add(createRiskItem("MEDIUM", "权利义务", "付款条件约定模糊",
            "可能导致争议", "明确付款条件和时间节点"));

        List<ContractReviewResponse.RiskItem> lowRiskItems = new ArrayList<>();
        lowRiskItems.add(createRiskItem("LOW", "合同效力", "部分条款表述不够清晰",
            "存在潜在隐患但风险可控", "优化条款表述"));

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
}