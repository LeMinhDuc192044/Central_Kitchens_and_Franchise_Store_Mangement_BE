package com.example.Central_Kitchens_and_Franchise_Store_BE.exception.custom;

public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(String message) {
        super(message);
    }

    public InvalidStatusTransitionException(String currentStatus, String newStatus) {
        super(String.format("Cannot transition from %s to %s", currentStatus, newStatus));
    }
}
