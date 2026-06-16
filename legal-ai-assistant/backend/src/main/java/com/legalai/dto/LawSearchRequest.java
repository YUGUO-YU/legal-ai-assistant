package com.legalai.dto;

public class LawSearchRequest {
    private String keyword;
    private String categoryL1;
    private String categoryL2;
    private Integer status;
    private String effectiveDateRange;
    private String issuingAuthority;
    private Integer page = 1;
    private Integer pageSize = 10;

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getCategoryL1() { return categoryL1; }
    public void setCategoryL1(String categoryL1) { this.categoryL1 = categoryL1; }
    public String getCategoryL2() { return categoryL2; }
    public void setCategoryL2(String categoryL2) { this.categoryL2 = categoryL2; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getEffectiveDateRange() { return effectiveDateRange; }
    public void setEffectiveDateRange(String effectiveDateRange) { this.effectiveDateRange = effectiveDateRange; }
    public String getIssuingAuthority() { return issuingAuthority; }
    public void setIssuingAuthority(String issuingAuthority) { this.issuingAuthority = issuingAuthority; }
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }

    public String getStatusName() {
        if (status == null) return null;
        switch (status) {
            case 1: return "现行有效";
            case 2: return "已废止";
            case 3: return "修订中";
            case 4: return "尚未生效";
            case 5: return "部分失效";
            default: return "现行有效";
        }
    }
}
