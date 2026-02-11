package com.example.Central_Kitchens_and_Franchise_Store_BE.exception.custom;

public class AuthenticationException extends RuntimeException {
    public AuthenticationException(String message) {
        super(message);
    }
}
