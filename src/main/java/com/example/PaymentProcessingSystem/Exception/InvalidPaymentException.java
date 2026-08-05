package com.example.PaymentProcessingSystem.Exception;

public class InvalidPaymentException extends RuntimeException {
    public InvalidPaymentException(String message) {
            super(message);
        }
}
