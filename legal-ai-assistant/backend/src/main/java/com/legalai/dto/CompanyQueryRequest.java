package com.legalai.dto;

import java.util.List;
import java.util.Map;

public class CompanyQueryRequest {
    private String companyName;
    private String unifiedSocialCreditCode;
    private Boolean enableRiskWarning = true;

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getUnifiedSocialCreditCode() { return unifiedSocialCreditCode; }
    public void setUnifiedSocialCreditCode(String unifiedSocialCreditCode) { this.unifiedSocialCreditCode = unifiedSocialCreditCode; }
    public Boolean getEnableRiskWarning() { return enableRiskWarning; }
    public void setEnableRiskWarning(Boolean enableRiskWarning) { this.enableRiskWarning = enableRiskWarning; }
}