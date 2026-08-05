package com.example.PaymentProcessingSystem.model;

import java.time.LocalDateTime;

public record PaymentHistory(
        Long history_id,
        Long payment_id,
        String status,
        String message,
        LocalDateTime created_at
) {
}
