package com.example.FakeErrorSimulator.Simulator;

import com.example.FakeErrorSimulator.Dto.GatewayResponse;
import com.example.FakeErrorSimulator.Enums.GatewayStatus;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class RandomFailureSimulator {
    private final Random random = new Random();
    public GatewayResponse simulate()
    {
        int chance= random.nextInt(100)+1;
        if(chance<=90)
        {
            return new GatewayResponse(GatewayStatus.SUCCESS, "Payment processed successfully");
        }
        if(chance<=93)
        {
            return new GatewayResponse(GatewayStatus.NETWORK_ERROR, "Network error occurred while processing the payment");
        }
        if(chance<=95)
        {
            return new GatewayResponse(GatewayStatus.GATEWAY_TIMEOUT, "Payment processing timed out");
        }
        if(chance<=98)
        {
            return new GatewayResponse(GatewayStatus.BANK_SERVER_DOWN, "Bank server is down, please try again later");
        }
        return new GatewayResponse(GatewayStatus.UNKNOWN_ERROR, "An unknown error occurred while processing the payment");

    }


}
