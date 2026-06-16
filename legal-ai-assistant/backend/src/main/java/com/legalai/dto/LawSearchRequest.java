package com.legalai.dto;

import java.util.List;

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
}