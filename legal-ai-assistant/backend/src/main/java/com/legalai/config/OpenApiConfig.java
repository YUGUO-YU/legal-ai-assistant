package com.legalai.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("法律AI助手系统 API")
                        .version("1.0.0")
                        .description("法律AI助手系统，提供法规检索、案例分析、合同审查等功能")
                        .contact(new Contact()
                                .name("LegalAI Team")
                                .email("support@legalai.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("http://localhost:3001").description("开发环境"),
                        new Server().url("https://api.legalai.com").description("生产环境")
                ));
    }
}
