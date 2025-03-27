package com.foodmap.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

// AppConfig.java
@Configuration
@ComponentScan(basePackages = "com.example")
@EnableTransactionManagement
@ImportResource("classpath:applicationContext.xml")
public class Appconfig {
}