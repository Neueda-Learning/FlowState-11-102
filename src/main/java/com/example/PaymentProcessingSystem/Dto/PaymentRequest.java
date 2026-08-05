package com.example.PaymentProcessingSystem.Dto;

import java.math.BigDecimal;

public record PaymentRequest(
        String source_account_number, String destination_account_number, BigDecimal amount, String currency,String idempotency_key) {
}
