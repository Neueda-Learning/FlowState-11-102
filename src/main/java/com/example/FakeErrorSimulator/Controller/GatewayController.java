package com.example.FakeErrorSimulator.Controller;

import com.example.FakeErrorSimulator.Dto.GatewayRequest;
import com.example.FakeErrorSimulator.Dto.GatewayResponse;
import com.example.FakeErrorSimulator.Service.GatewayService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/gateway")
public class GatewayController {
    private final GatewayService service;
    public GatewayController(GatewayService service) {
        this.service = service;
    }
    @PostMapping("/process")
    public GatewayResponse process(@RequestBody GatewayRequest request) {
        return service.processPayment(request);
    }

}
