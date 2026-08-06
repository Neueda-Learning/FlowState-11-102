package com.example.FakeErrorSimulator.Dto;

import java.math.BigDecimal;

public class GatewayRequest {
    private String payment_reference;
    private BigDecimal amount;

    public GatewayRequest() {
    }
    public String getPayment_reference()
    {
        return payment_reference;
    }
    public void setPayment_reference(String payment_reference)
    {
        this.payment_reference = payment_reference;
    }
    public BigDecimal getAmount() {
        return amount;
    }
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
