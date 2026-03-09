package com.example.Central_Kitchens_and_Franchise_Store_BE.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "vnpay")
public class VNPayProperties {
    private String tmnCode;
    private String hashSecret;
    private String url;
    private String apiUrl;
    private String returnUrl;
    private String ipnUrl;
    private String version;
    private String command;
    private String locale;
    private String currencyCode;
    private String refundUrl;
}
