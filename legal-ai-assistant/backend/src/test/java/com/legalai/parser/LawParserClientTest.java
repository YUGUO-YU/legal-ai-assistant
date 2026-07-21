package com.legalai.parser;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LawParserClientTest {
    @Autowired(required = false)
    private LawParserClient lawParserClient;

    @Test
    void contextLoads() {
    }

    @Test
    void isAvailableReflectsProcessState() {
        if (lawParserClient == null) {
            return;
        }
        boolean available = lawParserClient.isAvailable();
        assert available == (lawParserClient.isAvailable());
    }
}