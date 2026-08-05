package com.example.PaymentProcessingSystem.Exception;

import java.time.LocalDateTime;

public record ApiError(LocalDateTime timestamp, int status, String error, String message) {
}
