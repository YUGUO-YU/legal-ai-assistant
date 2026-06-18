package com.legalai.dto;

import java.math.BigDecimal;
import java.util.List;

public class CompanyQueryResponse {
    private String queryUuid;
    private String companyName;
    private String unifiedSocialCreditCode;
    private String legalRepresentative;
    private BigDecimal registeredCapital;
    private String businessStatus;
    private String registrationAuthority;
    private String establishDate;
    private List<ShareholderInfo> shareholders;
    private List<RiskWarning> riskWarnings;
    private String riskLevel;
    private String dataSource;
    private String queryTime;

    public String getQueryUuid() { return queryUuid; }
    public void setQueryUuid(String queryUuid) { this.queryUuid = queryUuid; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getUnifiedSocialCreditCode() { return unifiedSocialCreditCode; }
    public void setUnifiedSocialCreditCode(String unifiedSocialCreditCode) { this.unifiedSocialCreditCode = unifiedSocialCreditCode; }
    public String getLegalRepresentative() { return legalRepresentative; }
    public void setLegalRepresentative(String legalRepresentative) { this.legalRepresentative = legalRepresentative; }
    public BigDecimal getRegisteredCapital() { return registeredCapital; }
    public void setRegisteredCapital(BigDecimal registeredCapital) { this.registeredCapital = registeredCapital; }
    public String getBusinessStatus() { return businessStatus; }
    public void setBusinessStatus(String businessStatus) { this.businessStatus = businessStatus; }
    public String getRegistrationAuthority() { return registrationAuthority; }
    public void setRegistrationAuthority(String registrationAuthority) { this.registrationAuthority = registrationAuthority; }
    public String getEstablishDate() { return establishDate; }
    public void setEstablishDate(String establishDate) { this.establishDate = establishDate; }
    public List<ShareholderInfo> getShareholders() { return shareholders; }
    public void setShareholders(List<ShareholderInfo> shareholders) { this.shareholders = shareholders; }
    public List<RiskWarning> getRiskWarnings() { return riskWarnings; }
    public void setRiskWarnings(List<RiskWarning> riskWarnings) { this.riskWarnings = riskWarnings; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }
    public String getQueryTime() { return queryTime; }
    public void setQueryTime(String queryTime) { this.queryTime = queryTime; }

    public static class ShareholderInfo {
        private String name;
        private String capitalContribution;
        private String ratio;
        private String type;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCapitalContribution() { return capitalContribution; }
        public void setCapitalContribution(String capitalContribution) { this.capitalContribution = capitalContribution; }
        public String getRatio() { return ratio; }
        public void setRatio(String ratio) { this.ratio = ratio; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }

    public static class RiskWarning {
        private String level;
        private String type;
        private String description;
        private String date;
        private Integer count;

        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public Integer getCount() { return count; }
        public void setCount(Integer count) { this.count = count; }
    }
}