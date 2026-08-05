package com.example.PaymentProcessingSystem.model;

import java.time.LocalDateTime;

public record AuditRecord(
        Long id,
        String aggregate_type,
        String aggregate_id,
        String event_type,
        String message,
        LocalDateTime created_at
) {
}
