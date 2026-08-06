package com.example.FakeErrorSimulator.Service;

import com.example.FakeErrorSimulator.Dto.GatewayRequest;
import com.example.FakeErrorSimulator.Dto.GatewayResponse;
import com.example.FakeErrorSimulator.Enums.GatewayStatus;
import com.example.FakeErrorSimulator.Simulator.RandomFailureSimulator;
import org.springframework.stereotype.Service;

@Service
public class GatewayService {
    private final RandomFailureSimulator simulator;
    public GatewayService(RandomFailureSimulator simulator) {
        this.simulator = simulator;
    }
    public GatewayResponse processPayment(GatewayRequest request)
    {
        return simulator.simulate();
    }

}
