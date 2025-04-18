package com.foodmap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;


@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class SwaggerOnlyApp {
    public static void main(String[] args) {
        SpringApplication.run(SwaggerOnlyApp.class, args);
    }
}