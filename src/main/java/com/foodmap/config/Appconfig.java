package com.foodmap.config;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;


// AppConfig.java
@SpringBootApplication
@ImportResource("classpath:applicationContext.xml")
public class Appconfig {
}