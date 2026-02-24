package com.example.Central_Kitchens_and_Franchise_Store_BE.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ghn")
@Data
public class GhnConfig {
    private String baseUrl;
    private String token;
    private String shopId;
}
