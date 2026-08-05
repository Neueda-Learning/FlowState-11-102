package com.example.PaymentProcessingSystem.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Payment(Long payment_id, String payment_reference, Long source_account_id, Long destination_account_id,
                      BigDecimal amount, String currency, String status, String failure_reason, Integer retry_count, String idempotency_key,
                      LocalDateTime created_at, LocalDateTime updated_at) {

}
