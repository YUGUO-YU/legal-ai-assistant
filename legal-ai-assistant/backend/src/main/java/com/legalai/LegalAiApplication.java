package com.legalai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@MapperScan("com.legalai.repository")
@EnableAsync
public class LegalAiApplication {
    public static void main(String[] args) {
        SpringApplication.run(LegalAiApplication.class, args);
    }
}