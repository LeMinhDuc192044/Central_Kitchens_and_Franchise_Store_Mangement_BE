package com.example.Central_Kitchens_and_Franchise_Store_BE.exception.custom;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
