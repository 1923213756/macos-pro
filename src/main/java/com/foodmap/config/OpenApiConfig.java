package com.foodmap.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI foodMapOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FoodMap 商铺管理API")
                        .description("美食地图商铺管理系统API文档")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("开发团队")
                                .email("contact@example.com"))
                        .license(new License()
                                .name("API License")
                                .url("https://example.com/license")));
    }
}