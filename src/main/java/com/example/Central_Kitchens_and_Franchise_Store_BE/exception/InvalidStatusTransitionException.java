package com.example.Central_Kitchens_and_Franchise_Store_BE.exception;

public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(String message) {
        super(message);
    }

}
