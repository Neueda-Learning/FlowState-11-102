package com.example.PaymentProcessingSystem.Exception;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String message) {
        super(message);
    }

}
