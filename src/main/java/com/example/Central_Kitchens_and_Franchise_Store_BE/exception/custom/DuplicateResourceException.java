package com.example.Central_Kitchens_and_Franchise_Store_BE.exception.custom;

public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
