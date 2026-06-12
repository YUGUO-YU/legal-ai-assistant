package com.legalai.dto;

import java.util.List;

public class ContractReviewRequest {
    private String contractText;
    private String contractType;
    private String contractAmount;

    public String getContractText() { return contractText; }
    public void setContractText(String contractText) { this.contractText = contractText; }
    public String getContractType() { return contractType; }
    public void setContractType(String contractType) { this.contractType = contractType; }
    public String getContractAmount() { return contractAmount; }
    public void setContractAmount(String contractAmount) { this.contractAmount = contractAmount; }
}