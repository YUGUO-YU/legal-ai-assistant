package com.legalai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.legalai.repository")
public class LegalAiApplication {
    public static void main(String[] args) {
        SpringApplication.run(LegalAiApplication.class, args);
    }
}