package com.example.FakeErrorSimulator.Dto;

import com.example.FakeErrorSimulator.Enums.GatewayStatus;

public class GatewayResponse {
    private GatewayStatus status;
    private String message;
    public GatewayResponse() {

    }
    public GatewayResponse(GatewayStatus status, String message) {
        this.status = status;
        this.message = message;
    }
    public GatewayStatus getStatus() {
        return status;
    }
    public void setStatus(GatewayStatus status) {
        this.status = status;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
}
