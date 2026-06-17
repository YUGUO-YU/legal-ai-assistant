package com.legalai.dto;

import java.math.BigDecimal;
import java.util.List;

public class DocumentDraftRequest {
    private String templateCode;
    private String caseType;
    private DocumentData caseData;
    private Boolean includeRiskPrompt = true;

    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }
    public String getCaseType() { return caseType; }
    public void setCaseType(String caseType) { this.caseType = caseType; }
    public DocumentData getCaseData() { return caseData; }
    public void setCaseData(DocumentData caseData) { this.caseData = caseData; }
    public Boolean getIncludeRiskPrompt() { return includeRiskPrompt; }
    public void setIncludeRiskPrompt(Boolean includeRiskPrompt) { this.includeRiskPrompt = includeRiskPrompt; }

    public static class DocumentData {
        private String plaintiffName;
        private String plaintiffPhone;
        private String plaintiffIdCard;
        private String plaintiffAddress;

        private String defendantName;
        private String defendantPhone;
        private String defendantIdCard;
        private String defendantAddress;
        private String defendantCompany;

        private BigDecimal claimAmount;
        private String claimDescription;
        private Object facts;
        private Object evidence;
        private String courtName;
        private String caseCause;

        @SuppressWarnings("unchecked")
        public List<String> getFacts() {
            if (facts == null) return null;
            if (facts instanceof List) return (List<String>) facts;
            if (facts instanceof String) {
                return List.of(((String) facts).split("\n"));
            }
            return null;
        }

        public void setFacts(Object facts) { this.facts = facts; }

        @SuppressWarnings("unchecked")
        public List<String> getEvidence() {
            if (evidence == null) return null;
            if (evidence instanceof List) return (List<String>) evidence;
            if (evidence instanceof String) {
                return List.of(((String) evidence).split("\n"));
            }
            return null;
        }

        public void setEvidence(Object evidence) { this.evidence = evidence; }

        public String getPlaintiffName() { return plaintiffName; }
        public void setPlaintiffName(String plaintiffName) { this.plaintiffName = plaintiffName; }
        public String getPlaintiffPhone() { return plaintiffPhone; }
        public void setPlaintiffPhone(String plaintiffPhone) { this.plaintiffPhone = plaintiffPhone; }
        public String getPlaintiffIdCard() { return plaintiffIdCard; }
        public void setPlaintiffIdCard(String plaintiffIdCard) { this.plaintiffIdCard = plaintiffIdCard; }
        public String getPlaintiffAddress() { return plaintiffAddress; }
        public void setPlaintiffAddress(String plaintiffAddress) { this.plaintiffAddress = plaintiffAddress; }
        public String getDefendantName() { return defendantName; }
        public void setDefendantName(String defendantName) { this.defendantName = defendantName; }
        public String getDefendantPhone() { return defendantPhone; }
        public void setDefendantPhone(String defendantPhone) { this.defendantPhone = defendantPhone; }
        public String getDefendantIdCard() { return defendantIdCard; }
        public void setDefendantIdCard(String defendantIdCard) { this.defendantIdCard = defendantIdCard; }
        public String getDefendantAddress() { return defendantAddress; }
        public void setDefendantAddress(String defendantAddress) { this.defendantAddress = defendantAddress; }
        public String getDefendantCompany() { return defendantCompany; }
        public void setDefendantCompany(String defendantCompany) { this.defendantCompany = defendantCompany; }
        public BigDecimal getClaimAmount() { return claimAmount; }
        public void setClaimAmount(BigDecimal claimAmount) { this.claimAmount = claimAmount; }
        public String getClaimDescription() { return claimDescription; }
        public void setClaimDescription(String claimDescription) { this.claimDescription = claimDescription; }
        public String getCourtName() { return courtName; }
        public void setCourtName(String courtName) { this.courtName = courtName; }
        public String getCaseCause() { return caseCause; }
        public void setCaseCause(String caseCause) { this.caseCause = caseCause; }
    }
}
