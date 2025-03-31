package com.foodmap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@ComponentScan(
        basePackages = "com.foodmap",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {com.foodmap.service.impl.ShopServiceImpl.class}
        )
)
public class SwaggerOnlyApp {
    public static void main(String[] args) {
        SpringApplication.run(SwaggerOnlyApp.class, args);
    }
}