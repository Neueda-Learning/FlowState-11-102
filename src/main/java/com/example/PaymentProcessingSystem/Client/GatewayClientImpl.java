package com.example.PaymentProcessingSystem.Client;

import com.example.PaymentProcessingSystem.Dto.GatewayRequest;
import com.example.PaymentProcessingSystem.Dto.GatewayResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class GatewayClientImpl implements GatewayClient{
    private final RestTemplate restTemplate;
    private final String gatewayBaseUrl;

    public GatewayClientImpl(RestTemplate restTemplate,
                             @Value("${gateway.base-url:http://localhost:8081}") String gatewayBaseUrl) {
        this.restTemplate = restTemplate;
        this.gatewayBaseUrl = gatewayBaseUrl;
    }

    @Override
    public GatewayResponse processPayment(GatewayRequest request) {
        String endpoint = gatewayBaseUrl.replaceAll("/$", "") + "/gateway/process";
        return restTemplate.postForObject(endpoint, request, GatewayResponse.class);
    }

}
