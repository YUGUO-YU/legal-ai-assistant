package com.legalai.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SourceVerificationServiceTest {

    private final SourceVerificationService service = new SourceVerificationService();

    @Test
    void isAllowedSource_WithNpcGovCn_ShouldReturnTrue() {
        assertTrue(service.isAllowedSource("https://flk.npc.gov.cn/"));
        assertTrue(service.isAllowedSource("https://www.npc.gov.cn/"));
        assertTrue(service.isAllowedSource("http://npc.gov.cn/test"));
    }

    @Test
    void isAllowedSource_WithCourtGovCn_ShouldReturnTrue() {
        assertTrue(service.isAllowedSource("https://www.court.gov.cn/"));
        assertTrue(service.isAllowedSource("https://wenshu.court.gov.cn/"));
    }

    @Test
    void isAllowedSource_WithPkulaw_ShouldReturnTrue() {
        assertTrue(service.isAllowedSource("https://www.pkulaw.cn/"));
    }

    @Test
    void isAllowedSource_WithUnknownDomain_ShouldReturnFalse() {
        assertFalse(service.isAllowedSource("https://www.unknown.com/"));
        assertFalse(service.isAllowedSource("https://example.com/"));
    }

    @Test
    void isAllowedSource_WithNullOrEmpty_ShouldReturnFalse() {
        assertFalse(service.isAllowedSource(null));
        assertFalse(service.isAllowedSource(""));
        assertFalse(service.isAllowedSource("  "));
    }

    @Test
    void extractDomain_WithHttpsUrl_ShouldExtractDomain() {
        assertEquals("www.example.com", service.extractDomain("https://www.example.com/path"));
        assertEquals("www.example.com", service.extractDomain("https://www.example.com"));
    }

    @Test
    void extractDomain_WithHttpUrl_ShouldExtractDomain() {
        assertEquals("www.example.com", service.extractDomain("http://www.example.com/path"));
        assertEquals("www.example.com", service.extractDomain("http://www.example.com"));
    }

    @Test
    void extractDomain_WithNoProtocol_ShouldExtractDomain() {
        assertEquals("www.example.com", service.extractDomain("www.example.com/path"));
        assertEquals("www.example.com", service.extractDomain("www.example.com"));
    }

    @Test
    void extractDomain_WithNull_ShouldReturnNull() {
        assertNull(service.extractDomain(null));
    }

    @Test
    void containsSensitiveKeywords_WithSensitiveWords_ShouldReturnTrue() {
        assertTrue(service.containsSensitiveKeywords("包含色情内容"));
        assertTrue(service.containsSensitiveKeywords("赌博网站"));
        assertTrue(service.containsSensitiveKeywords("毒品交易"));
    }

    @Test
    void containsSensitiveKeywords_WithNormalText_ShouldReturnFalse() {
        assertFalse(service.containsSensitiveKeywords("这是一个正常的法律文本"));
        assertFalse(service.containsSensitiveKeywords("合同法规定"));
    }

    @Test
    void containsSensitiveKeywords_WithNullOrEmpty_ShouldReturnFalse() {
        assertFalse(service.containsSensitiveKeywords(null));
        assertFalse(service.containsSensitiveKeywords(""));
    }

    @Test
    void isQuerySensitive_ShouldDelegateToContainsSensitiveKeywords() {
        assertTrue(service.isQuerySensitive("查找色情内容"));
        assertFalse(service.isQuerySensitive("查找合同法条款"));
    }

    @Test
    void getSourceConfidence_WithFlkNpcGovCn_ShouldReturnHigh() {
        assertEquals("HIGH", service.getSourceConfidence("https://flk.npc.gov.cn/"));
        assertEquals("HIGH", service.getSourceConfidence("https://www.npc.gov.cn/"));
    }

    @Test
    void getSourceConfidence_WithCourtGovCn_ShouldReturnHigh() {
        assertEquals("HIGH", service.getSourceConfidence("https://court.gov.cn/"));
        assertEquals("HIGH", service.getSourceConfidence("https://wenshu.court.gov.cn/"));
    }

    @Test
    void getSourceConfidence_WithPkulaw_ShouldReturnMedium() {
        assertEquals("MEDIUM", service.getSourceConfidence("https://www.pkulaw.cn/"));
    }

    @Test
    void getSourceConfidence_WithUnknownSource_ShouldReturnUnknown() {
        assertEquals("UNKNOWN", service.getSourceConfidence("https://www.unknown.com/"));
    }

    @Test
    void getAllowedDomains_ShouldReturnSortedList() {
        var domains = service.getAllowedDomains();
        assertNotNull(domains);
        assertFalse(domains.isEmpty());
        assertTrue(domains.contains("flk.npc.gov.cn"));
        assertTrue(domains.contains("npc.gov.cn"));
        assertTrue(domains.contains("court.gov.cn"));
    }
}
