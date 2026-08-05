package com.example.PaymentProcessingSystem.Client;

import com.example.PaymentProcessingSystem.Dto.GatewayRequest;
import com.example.PaymentProcessingSystem.Dto.GatewayResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class GatewayClientImpl implements GatewayClient{
    private final RestTemplate restTemplate;
    public GatewayClientImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    @Override
    public GatewayResponse processPayment(GatewayRequest request) {
        return restTemplate.postForObject("http://localhost:8081/gateway/process", request, GatewayResponse.class);
    }

}
