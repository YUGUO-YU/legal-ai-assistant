package com.legalai.service;

import com.legalai.dto.CompanyQueryRequest;
import com.legalai.dto.CompanyQueryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @InjectMocks
    private CompanyService companyService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(companyService, "mockEnabled", true);
    }

    @Test
    void testQueryCompany_BasicInfo() {
        CompanyQueryRequest request = new CompanyQueryRequest();
        request.setCompanyName("测试科技有限公司");

        CompanyQueryResponse response = companyService.queryCompany(request);

        assertNotNull(response);
        assertNotNull(response.getCompanyName());
        assertNotNull(response.getUuid());
        assertNotNull(response.getUnifiedSocialCreditCode());
        assertNotNull(response.getLegalRepresentative());
        assertNotNull(response.getRegisteredCapital());
        assertNotNull(response.getBusinessStatus());
    }

    @Test
    void testQueryCompany_Shareholders() {
        CompanyQueryRequest request = new CompanyQueryRequest();
        request.setCompanyName("测试科技");

        CompanyQueryResponse response = companyService.queryCompany(request);

        assertNotNull(response.getShareholders());
        assertFalse(response.getShareholders().isEmpty());
        CompanyQueryResponse.ShareholderInfo shareholder = response.getShareholders().get(0);
        assertNotNull(shareholder.getName());
        assertNotNull(shareholder.getRatio());
    }

    @Test
    void testQueryCompany_EquityChain() {
        CompanyQueryRequest request = new CompanyQueryRequest();
        request.setCompanyName("测试科技");

        CompanyQueryResponse response = companyService.queryCompany(request);

        assertNotNull(response.getEquityChain());
        assertFalse(response.getEquityChain().isEmpty());
    }

    @Test
    void testQueryCompany_RelatedCompanies() {
        CompanyQueryRequest request = new CompanyQueryRequest();
        request.setCompanyName("测试科技");

        CompanyQueryResponse response = companyService.queryCompany(request);

        assertNotNull(response.getRelatedCompanies());
        assertFalse(response.getRelatedCompanies().isEmpty());
    }

    @Test
    void testQueryCompany_BeneficialOwner() {
        CompanyQueryRequest request = new CompanyQueryRequest();
        request.setCompanyName("测试科技");

        CompanyQueryResponse response = companyService.queryCompany(request);

        assertNotNull(response.getBeneficialOwner());
        assertNotNull(response.getBeneficialOwner().getName());
    }

    @Test
    void testQueryCompany_BusinessAnalysis() {
        CompanyQueryRequest request = new CompanyQueryRequest();
        request.setCompanyName("测试科技");

        CompanyQueryResponse response = companyService.queryCompany(request);

        assertNotNull(response.getBusinessAnalysis());
        assertNotNull(response.getBusinessAnalysis().getIndustry());
        assertNotNull(response.getBusinessAnalysis().getMainBusiness());
    }

    @Test
    void testQueryCompany_WithRiskWarning() {
        CompanyQueryRequest request = new CompanyQueryRequest();
        request.setCompanyName("测试科技");
        request.setEnableRiskWarning(true);

        CompanyQueryResponse response = companyService.queryCompany(request);

        assertNotNull(response.getRiskWarnings());
        assertNotNull(response.getRiskLevel());
    }

    @Test
    void testQueryCompany_WithoutRiskWarning() {
        CompanyQueryRequest request = new CompanyQueryRequest();
        request.setCompanyName("测试科技");
        request.setEnableRiskWarning(false);

        CompanyQueryResponse response = companyService.queryCompany(request);

        assertNotNull(response.getRiskWarnings());
        assertTrue(response.getRiskWarnings().isEmpty());
        assertEquals("NONE", response.getRiskLevel());
    }

    @Test
    void testQueryCompany_EmptyName() {
        CompanyQueryRequest request = new CompanyQueryRequest();
        request.setCompanyName("");

        assertThrows(IllegalArgumentException.class, () -> companyService.queryCompany(request));
    }

    @Test
    void testQueryCompany_NullName() {
        CompanyQueryRequest request = new CompanyQueryRequest();

        assertThrows(IllegalArgumentException.class, () -> companyService.queryCompany(request));
    }

    @Test
    void testGetCompanyDetail() {
        CompanyQueryResponse response = companyService.getCompanyDetail("测试科技");

        assertNotNull(response);
        assertNotNull(response.getCompanyName());
    }

    @Test
    void testAnalyzeShareholdingStructure() {
        java.util.List<CompanyQueryResponse.ShareholderInfo> shareholders =
            companyService.analyzeShareholdingStructure("测试科技");

        assertNotNull(shareholders);
        assertFalse(shareholders.isEmpty());
    }
}
