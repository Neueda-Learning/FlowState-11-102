package com.example.PaymentProcessingSystem.Dto;

import java.math.BigDecimal;

public record GatewayRequest(
        String payment_reference, BigDecimal amount
) {
}
