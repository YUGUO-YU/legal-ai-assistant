package com.legalai.service;

import com.legalai.dto.ContractReviewRequest;
import com.legalai.dto.ContractReviewResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ContractServiceTest {

    @InjectMocks
    private ContractService contractService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(contractService, "mockEnabled", true);
    }

    @Test
    void testReviewContract_Basic() {
        ContractReviewRequest request = new ContractReviewRequest();
        request.setContractText("甲方与乙方签订软件开发合同，总金额50万元，开发周期6个月...");

        ContractReviewResponse response = contractService.reviewContract(request);

        assertNotNull(response);
        assertNotNull(response.getReviewUuid());
        assertNotNull(response.getRiskLevel());
        assertNotNull(response.getHighRiskItems());
        assertNotNull(response.getLowRiskItems());
    }

    @Test
    void testReviewContract_HasRiskItems() {
        ContractReviewRequest request = new ContractReviewRequest();
        request.setContractText("租赁合同：甲方将位于北京市朝阳区的商业用房出租给乙方...");

        ContractReviewResponse response = contractService.reviewContract(request);

        assertNotNull(response);
        assertNotNull(response.getOverallComment());
        assertNotNull(response.getDimensions());
        assertNotNull(response.getTotalScore());
    }

    @Test
    void testReviewContract_EmptyText() {
        ContractReviewRequest request = new ContractReviewRequest();
        request.setContractText("");

        assertThrows(Exception.class, () -> contractService.reviewContract(request));
    }

    @Test
    void testReviewContract_ShortText() {
        ContractReviewRequest request = new ContractReviewRequest();
        request.setContractText("短文本");

        ContractReviewResponse response = contractService.reviewContract(request);

        assertNotNull(response);
        assertNotNull(response.getReviewUuid());
    }

    @Test
    void testGetReview_AfterReview() {
        ContractReviewRequest request = new ContractReviewRequest();
        request.setContractText("测试合同内容...");

        ContractReviewResponse response = contractService.reviewContract(request);
        ContractReviewResponse saved = contractService.getReview(response.getReviewUuid());

        assertNotNull(saved);
        assertEquals(response.getReviewUuid(), saved.getReviewUuid());
    }
}
