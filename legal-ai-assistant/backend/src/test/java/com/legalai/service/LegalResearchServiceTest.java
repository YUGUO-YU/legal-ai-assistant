package com.legalai.service;

import com.legalai.dto.LegalResearchRequest;
import com.legalai.dto.LegalResearchResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalResearchServiceTest {

    @Mock
    private AIService aiService;

    @Mock
    private LegalSearchService legalSearchService;

    @Mock
    private ElasticsearchService elasticsearchService;

    @Mock
    private MilvusService milvusService;

    @InjectMocks
    private LegalResearchService legalResearchService;

    private LegalResearchRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new LegalResearchRequest();
        validRequest.setQuestion("合同违约的法律后果");
        validRequest.setDepth("standard");
    }

    @Test
    void shouldRejectEmptyTopic() {
        validRequest.setQuestion("");
        assertThrows(IllegalArgumentException.class,
            () -> legalResearchService.createResearchTask(validRequest));
    }

    @Test
    void shouldRejectNullTopic() {
        validRequest.setQuestion(null);
        assertThrows(IllegalArgumentException.class,
            () -> legalResearchService.createResearchTask(validRequest));
    }

    @Test
    void shouldCreateTaskWithValidRequest() {
        String taskId = legalResearchService.createResearchTask(validRequest);
        assertNotNull(taskId);
        assertFalse(taskId.isEmpty());
    }

    @Test
    void shouldReturnReportForValidTask() throws Exception {
        String fakeReport = "## 研究报告\n### 概述\n违约的法律后果包括损害赔偿和继续履行。";
        when(aiService.chat(any())).thenReturn(fakeReport);

        String taskId = legalResearchService.createResearchTask(validRequest);
        LegalResearchResponse report = legalResearchService.getResearchReport(taskId);

        assertNotNull(report);
    }

    @Test
    void shouldHandleAIServiceFailure() throws Exception {
        doThrow(new RuntimeException("AI服务不可用"))
            .when(aiService).chat(any());

        legalResearchService.createResearchTask(validRequest);
    }
}
