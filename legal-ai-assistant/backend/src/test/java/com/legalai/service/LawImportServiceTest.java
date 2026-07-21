package com.legalai.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.legalai.dto.LawImportPreview;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LawImportServiceTest {
    @Autowired(required = false)
    private LawImportService lawImportService;

    @Test
    void contextLoads() {
        assertNotNull(lawImportService);
    }
}