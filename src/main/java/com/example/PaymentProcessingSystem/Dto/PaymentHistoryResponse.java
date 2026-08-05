package com.example.PaymentProcessingSystem.Dto;

import java.time.LocalDateTime;

public record PaymentHistoryResponse(
        String status,
        String message,
        LocalDateTime created_at
) {
}
