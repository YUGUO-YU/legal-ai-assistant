package com.legalai.service;

import com.legalai.dto.CaseSimilarSearchRequest;
import com.legalai.dto.CaseSimilarSearchResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CaseServiceTest {

    @InjectMocks
    private CaseService caseService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(caseService, "mockEnabled", true);
    }

    @Test
    void testSearchSimilarCases_Basic() {
        CaseSimilarSearchRequest request = new CaseSimilarSearchRequest();
        request.setCaseDescription("原告与被告签订装修合同，被告擅自变更材料品牌且进度滞后");
        request.setCaseType(1);
        request.setTopK(5);

        CaseSimilarSearchResponse response = caseService.searchSimilarCases(request);

        assertNotNull(response);
        assertNotNull(response.getItems());
        assertTrue(response.getItems().size() <= 5);
        assertNotNull(response.getStatistics());
    }

    @Test
    void testSearchSimilarCases_WithTopK() {
        CaseSimilarSearchRequest request = new CaseSimilarSearchRequest();
        request.setCaseDescription("买卖合同纠纷");
        request.setTopK(3);

        CaseSimilarSearchResponse response = caseService.searchSimilarCases(request);

        assertNotNull(response);
        assertTrue(response.getItems().size() <= 3);
    }

    @Test
    void testSearchSimilarCases_EmptyDescription() {
        CaseSimilarSearchRequest request = new CaseSimilarSearchRequest();
        request.setCaseDescription("");
        request.setTopK(5);

        assertThrows(Exception.class, () -> caseService.searchSimilarCases(request));
    }

    @Test
    void testGetCaseDetail_MockMode() {
        CaseSimilarSearchResponse.SimilarCaseItem detail = caseService.getCaseDetail("123");

        assertNotNull(detail);
        assertEquals("(2023)沪01民终4567号", detail.getCaseNo());
        assertEquals("上海市第一中级人民法院", detail.getCourtName());
        assertNotNull(detail.getKeyFacts());
        assertNotNull(detail.getLegalBasis());
        assertTrue(detail.getLegalBasis().size() >= 1);
    }

    @Test
    void testGetCaseDetail_ReturnsKeyFields() {
        CaseSimilarSearchResponse.SimilarCaseItem detail = caseService.getCaseDetail("456");

        assertNotNull(detail);
        assertNotNull(detail.getCaseId());
        assertNotNull(detail.getCaseNo());
        assertNotNull(detail.getCaseName());
        assertNotNull(detail.getCourtName());
        assertNotNull(detail.getJudgeDate());
        assertNotNull(detail.getJudgmentResult());
        assertNotNull(detail.getKeyFacts());
        assertNotNull(detail.getJudgmentSummary());
        assertNotNull(detail.getLegalBasis());
    }

    @Test
    void testCaseElements_InMockMode() {
        List<java.util.Map<String, Object>> elements = caseService.getCaseElements("123");

        assertNotNull(elements);
    }

    @Test
    void testElementStats_InMockMode() {
        java.util.Map<String, Object> stats = caseService.getElementStats();

        assertNotNull(stats);
        assertNotNull(stats.get("totalCases"));
        assertNotNull(stats.get("elementTypes"));
    }
}
