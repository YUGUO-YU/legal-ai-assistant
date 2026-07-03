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
    private List<String> searchSources;
    private String queryTime;
    private List<EquityChain> equityChain;
    private List<RelatedCompany> relatedCompanies;
    private BeneficialOwner beneficialOwner;
    private BusinessAnalysis businessAnalysis;
    private Boolean subscribed;

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
    public List<String> getSearchSources() { return searchSources; }
    public void setSearchSources(List<String> searchSources) { this.searchSources = searchSources; }
    public String getQueryTime() { return queryTime; }
    public void setQueryTime(String queryTime) { this.queryTime = queryTime; }
    public List<EquityChain> getEquityChain() { return equityChain; }
    public void setEquityChain(List<EquityChain> equityChain) { this.equityChain = equityChain; }
    public List<RelatedCompany> getRelatedCompanies() { return relatedCompanies; }
    public void setRelatedCompanies(List<RelatedCompany> relatedCompanies) { this.relatedCompanies = relatedCompanies; }
    public BeneficialOwner getBeneficialOwner() { return beneficialOwner; }
    public void setBeneficialOwner(BeneficialOwner beneficialOwner) { this.beneficialOwner = beneficialOwner; }
    public BusinessAnalysis getBusinessAnalysis() { return businessAnalysis; }
    public void setBusinessAnalysis(BusinessAnalysis businessAnalysis) { this.businessAnalysis = businessAnalysis; }
    public Boolean getSubscribed() { return subscribed; }
    public void setSubscribed(Boolean subscribed) { this.subscribed = subscribed; }

    public static class ShareholderInfo {
        private String name;
        private String capitalContribution;
        private String ratio;
        private String type;
        private Double actualRatio;
        private Boolean isVerified;
        private String verifiedDate;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCapitalContribution() { return capitalContribution; }
        public void setCapitalContribution(String capitalContribution) { this.capitalContribution = capitalContribution; }
        public String getRatio() { return ratio; }
        public void setRatio(String ratio) { this.ratio = ratio; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Double getActualRatio() { return actualRatio; }
        public void setActualRatio(Double actualRatio) { this.actualRatio = actualRatio; }
        public Boolean getIsVerified() { return isVerified; }
        public void setIsVerified(Boolean isVerified) { this.isVerified = isVerified; }
        public String getVerifiedDate() { return verifiedDate; }
        public void setVerifiedDate(String verifiedDate) { this.verifiedDate = verifiedDate; }
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

    public static class EquityChain {
        private Integer level;
        private String companyName;
        private String ratio;
        private Double amount;
        private String type;
        private List<EquityChain> children;

        public Integer getLevel() { return level; }
        public void setLevel(Integer level) { this.level = level; }
        public String getCompanyName() { return companyName; }
        public void setCompanyName(String companyName) { this.companyName = companyName; }
        public String getRatio() { return ratio; }
        public void setRatio(String ratio) { this.ratio = ratio; }
        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public List<EquityChain> getChildren() { return children; }
        public void setChildren(List<EquityChain> children) { this.children = children; }
    }

    public static class RelatedCompany {
        private String name;
        private String relation;
        private String unifiedSocialCreditCode;
        private String businessStatus;
        private String legalRepresentative;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getRelation() { return relation; }
        public void setRelation(String relation) { this.relation = relation; }
        public String getUnifiedSocialCreditCode() { return unifiedSocialCreditCode; }
        public void setUnifiedSocialCreditCode(String unifiedSocialCreditCode) { this.unifiedSocialCreditCode = unifiedSocialCreditCode; }
        public String getBusinessStatus() { return businessStatus; }
        public void setBusinessStatus(String businessStatus) { this.businessStatus = businessStatus; }
        public String getLegalRepresentative() { return legalRepresentative; }
        public void setLegalRepresentative(String legalRepresentative) { this.legalRepresentative = legalRepresentative; }
    }

    public static class BeneficialOwner {
        private String name;
        private String type;
        private Double actualRatio;
        private String idCard;
        private String nationality;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Double getActualRatio() { return actualRatio; }
        public void setActualRatio(Double actualRatio) { this.actualRatio = actualRatio; }
        public String getIdCard() { return idCard; }
        public void setIdCard(String idCard) { this.idCard = idCard; }
        public String getNationality() { return nationality; }
        public void setNationality(String nationality) { this.nationality = nationality; }
    }

    public static class BusinessAnalysis {
        private Integer employeeCount;
        private String employeeTrend;
        private BigDecimal paidInCapital;
        private String industry;
        private String industryAvgRatio;
        private Integer patentCount;
        private Integer trademarkCount;
        private Integer copyrightCount;
        private String businessScope;
        private String mainBusiness;
        private List<YearData> yearlyData;

        public Integer getEmployeeCount() { return employeeCount; }
        public void setEmployeeCount(Integer employeeCount) { this.employeeCount = employeeCount; }
        public String getEmployeeTrend() { return employeeTrend; }
        public void setEmployeeTrend(String employeeTrend) { this.employeeTrend = employeeTrend; }
        public BigDecimal getPaidInCapital() { return paidInCapital; }
        public void setPaidInCapital(BigDecimal paidInCapital) { this.paidInCapital = paidInCapital; }
        public String getIndustry() { return industry; }
        public void setIndustry(String industry) { this.industry = industry; }
        public String getIndustryAvgRatio() { return industryAvgRatio; }
        public void setIndustryAvgRatio(String industryAvgRatio) { this.industryAvgRatio = industryAvgRatio; }
        public Integer getPatentCount() { return patentCount; }
        public void setPatentCount(Integer patentCount) { this.patentCount = patentCount; }
        public Integer getTrademarkCount() { return trademarkCount; }
        public void setTrademarkCount(Integer trademarkCount) { this.trademarkCount = trademarkCount; }
        public Integer getCopyrightCount() { return copyrightCount; }
        public void setCopyrightCount(Integer copyrightCount) { this.copyrightCount = copyrightCount; }
        public String getBusinessScope() { return businessScope; }
        public void setBusinessScope(String businessScope) { this.businessScope = businessScope; }
        public String getMainBusiness() { return mainBusiness; }
        public void setMainBusiness(String mainBusiness) { this.mainBusiness = mainBusiness; }
        public List<YearData> getYearlyData() { return yearlyData; }
        public void setYearlyData(List<YearData> yearlyData) { this.yearlyData = yearlyData; }
    }

    public static class YearData {
        private Integer year;
        private BigDecimal capital;
        private Integer employee;
        private String revenue;

        public YearData() {}

        public YearData(Integer year, BigDecimal capital, Integer employee, String revenue) {
            this.year = year;
            this.capital = capital;
            this.employee = employee;
            this.revenue = revenue;
        }

        public Integer getYear() { return year; }
        public void setYear(Integer year) { this.year = year; }
        public BigDecimal getCapital() { return capital; }
        public void setCapital(BigDecimal capital) { this.capital = capital; }
        public Integer getEmployee() { return employee; }
        public void setEmployee(Integer employee) { this.employee = employee; }
        public String getRevenue() { return revenue; }
        public void setRevenue(String revenue) { this.revenue = revenue; }
    }
}
