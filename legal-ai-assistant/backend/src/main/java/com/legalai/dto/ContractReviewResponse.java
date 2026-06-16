package com.legalai.dto;

import java.util.List;

public class ContractReviewResponse {
    private String contractName;
    private String contractType;
    private Integer totalScore;
    private String riskLevel;
    private List<DimensionReview> dimensions;
    private List<RiskItem> highRiskItems;
    private List<RiskItem> mediumRiskItems;
    private List<RiskItem> lowRiskItems;
    private String overallComment;

    public String getContractName() { return contractName; }
    public void setContractName(String contractName) { this.contractName = contractName; }
    public String getContractType() { return contractType; }
    public void setContractType(String contractType) { this.contractType = contractType; }
    public Integer getTotalScore() { return totalScore; }
    public void setTotalScore(Integer totalScore) { this.totalScore = totalScore; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public List<DimensionReview> getDimensions() { return dimensions; }
    public void setDimensions(List<DimensionReview> dimensions) { this.dimensions = dimensions; }
    public List<RiskItem> getHighRiskItems() { return highRiskItems; }
    public void setHighRiskItems(List<RiskItem> highRiskItems) { this.highRiskItems = highRiskItems; }
    public List<RiskItem> getMediumRiskItems() { return mediumRiskItems; }
    public void setMediumRiskItems(List<RiskItem> mediumRiskItems) { this.mediumRiskItems = mediumRiskItems; }
    public List<RiskItem> getLowRiskItems() { return lowRiskItems; }
    public void setLowRiskItems(List<RiskItem> lowRiskItems) { this.lowRiskItems = lowRiskItems; }
    public String getOverallComment() { return overallComment; }
    public void setOverallComment(String overallComment) { this.overallComment = overallComment; }

    public static class DimensionReview {
        private String dimensionCode;
        private String dimensionName;
        private Integer score;
        private String comment;

        public String getDimensionCode() { return dimensionCode; }
        public void setDimensionCode(String dimensionCode) { this.dimensionCode = dimensionCode; }
        public String getDimensionName() { return dimensionName; }
        public void setDimensionName(String dimensionName) { this.dimensionName = dimensionName; }
        public Integer getScore() { return score; }
        public void setScore(Integer score) { this.score = score; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
    }

    public static class RiskItem {
        private String level;
        private String dimension;
        private String title;
        private String description;
        private String suggestion;

        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }
        public String getDimension() { return dimension; }
        public void setDimension(String dimension) { this.dimension = dimension; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getSuggestion() { return suggestion; }
        public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    }
}