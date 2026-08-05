package com.example.PaymentProcessingSystem.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Account(
        Long account_id, String account_number, String account_holder_name, String email, String phone_number,
        BigDecimal balance, String currency, String status, Integer version, LocalDateTime created_at, LocalDateTime updated_at
        ) {
}
