package com.legalai.service;

import com.legalai.dto.LegalSearchRequest;
import com.legalai.dto.LegalSearchResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class LegalSearchServiceTest {

    @InjectMocks
    private LegalSearchService legalSearchService;

    @Test
    void testSearch_WithResults() {
        LegalSearchRequest request = new LegalSearchRequest();
        request.setQuery("合同");
        request.setPage(1);
        request.setPageSize(10);

        LegalSearchResponse response = legalSearchService.search(request);

        assertNotNull(response);
        assertTrue(response.getTotal() > 0);
        assertNotNull(response.getItems());
        assertFalse(response.getItems().isEmpty());
        assertTrue(response.getTookMs() >= 0);
    }

    @Test
    void testSearch_ResponseContainsLawInfo() {
        LegalSearchRequest request = new LegalSearchRequest();
        request.setQuery("欺诈");
        request.setPage(1);
        request.setPageSize(5);

        LegalSearchResponse response = legalSearchService.search(request);

        assertNotNull(response.getItems());
        if (!response.getItems().isEmpty()) {
            LegalSearchResponse.SearchResultItem article = response.getItems().get(0);
            assertNotNull(article.getArticleId());
            assertNotNull(article.getLawTitle());
            assertNotNull(article.getTitle());
            assertNotNull(article.getContent());
        }
    }

    @Test
    void testSearch_WithValidQuery() {
        LegalSearchRequest request = new LegalSearchRequest();
        request.setQuery("a");
        request.setPage(1);
        request.setPageSize(10);

        LegalSearchResponse response = legalSearchService.search(request);

        assertNotNull(response);
        assertNotNull(response.getItems());
    }

    @Test
    void testSearch_Pagination() {
        LegalSearchRequest request1 = new LegalSearchRequest();
        request1.setQuery("合同");
        request1.setPage(1);
        request1.setPageSize(2);

        LegalSearchRequest request2 = new LegalSearchRequest();
        request2.setQuery("合同");
        request2.setPage(2);
        request2.setPageSize(2);

        LegalSearchResponse response1 = legalSearchService.search(request1);
        LegalSearchResponse response2 = legalSearchService.search(request2);

        assertNotNull(response1);
        assertNotNull(response2);
        assertEquals(response1.getPage(), 1);
        assertEquals(response2.getPage(), 2);
        assertEquals(response1.getPageSize(), 2);
        assertEquals(response2.getPageSize(), 2);
    }
}
