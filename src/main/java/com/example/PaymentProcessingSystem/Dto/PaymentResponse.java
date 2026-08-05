package com.example.PaymentProcessingSystem.Dto;

import java.math.BigDecimal;

public record PaymentResponse(Long payment_id, String payment_reference, Long source_account_id, Long destination_account_id , BigDecimal amount, String currency , String status, String failure_reason , Integer retry_count ,String message) {
}
