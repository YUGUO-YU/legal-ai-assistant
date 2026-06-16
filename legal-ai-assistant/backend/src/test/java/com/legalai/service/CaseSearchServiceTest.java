package com.legalai.service;

import com.legalai.dto.CaseSearchRequest;
import com.legalai.dto.CaseSearchResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CaseSearchServiceTest {

    @InjectMocks
    private CaseSearchService caseSearchService;

    @Test
    void testSearch_WithResults() {
        CaseSearchRequest request = new CaseSearchRequest();
        request.setKeyword("合同纠纷");
        request.setPage(1);
        request.setPageSize(10);

        CaseSearchResponse response = caseSearchService.searchCases(request);

        assertNotNull(response);
        assertTrue(response.getTotal() > 0);
        assertNotNull(response.getItems());
        assertFalse(response.getItems().isEmpty());
        assertTrue(response.getTookMs() >= 0);
    }

    @Test
    void testSearch_ResponseContainsCaseInfo() {
        CaseSearchRequest request = new CaseSearchRequest();
        request.setKeyword("劳动争议");
        request.setPage(1);
        request.setPageSize(5);

        CaseSearchResponse response = caseSearchService.searchCases(request);

        assertNotNull(response.getItems());
        if (!response.getItems().isEmpty()) {
            CaseSearchResponse.CaseSearchItem caseItem = response.getItems().get(0);
            assertNotNull(caseItem.getCaseUuid());
            assertNotNull(caseItem.getTitle());
            assertNotNull(caseItem.getCourt());
            assertNotNull(caseItem.getJudgeDate());
        }
    }

    @Test
    void testSearch_WithFilters() {
        CaseSearchRequest request = new CaseSearchRequest();
        request.setKeyword("合同");
        request.setCaseType(1);
        request.setCourtLevel(3);
        request.setPage(1);
        request.setPageSize(10);

        CaseSearchResponse response = caseSearchService.searchCases(request);

        assertNotNull(response);
        assertNotNull(response.getItems());
    }

    @Test
    void testSearch_Pagination() {
        CaseSearchRequest request1 = new CaseSearchRequest();
        request1.setKeyword("合同");
        request1.setPage(1);
        request1.setPageSize(2);

        CaseSearchRequest request2 = new CaseSearchRequest();
        request2.setKeyword("合同");
        request2.setPage(2);
        request2.setPageSize(2);

        CaseSearchResponse response1 = caseSearchService.searchCases(request1);
        CaseSearchResponse response2 = caseSearchService.searchCases(request2);

        assertEquals(response1.getPage(), 1);
        assertEquals(response2.getPage(), 2);
        assertEquals(response1.getPageSize(), 2);
        assertEquals(response2.getPageSize(), 2);
    }
}
