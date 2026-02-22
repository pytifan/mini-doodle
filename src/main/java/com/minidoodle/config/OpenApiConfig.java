package com.minidoodle.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI miniDoodleOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Mini Doodle API")
                        .description("Meeting Scheduling Platform - manage time slots, schedule meetings, and view calendar availability")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Mini Doodle")
                                .email("support@minidoodle.com")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local development")
                ));
    }
}
