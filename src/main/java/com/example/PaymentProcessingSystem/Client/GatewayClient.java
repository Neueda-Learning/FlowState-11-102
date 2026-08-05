package com.example.PaymentProcessingSystem.Client;

import com.example.PaymentProcessingSystem.Dto.GatewayRequest;
import com.example.PaymentProcessingSystem.Dto.GatewayResponse;

public interface GatewayClient {
    GatewayResponse processPayment(GatewayRequest request);
}
