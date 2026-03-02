package com.example.Central_Kitchens_and_Franchise_Store_BE.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;


@Component
public class RandomGeneratorUtil {
    private static final String CHAR_POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public String randomSix() {
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(CHAR_POOL.charAt(RANDOM.nextInt(CHAR_POOL.length())));
        }
        return sb.toString();
    }
}
