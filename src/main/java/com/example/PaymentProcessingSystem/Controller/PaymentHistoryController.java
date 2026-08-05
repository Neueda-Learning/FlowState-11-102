package com.example.PaymentProcessingSystem.Controller;

import com.example.PaymentProcessingSystem.Dto.PaymentHistoryResponse;
import com.example.PaymentProcessingSystem.Service.PaymentHistoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/payment-history")
public class PaymentHistoryController {

    private final PaymentHistoryService paymentHistoryService;

    public PaymentHistoryController(PaymentHistoryService paymentHistoryService) {
        this.paymentHistoryService = paymentHistoryService;
    }

    @GetMapping("/{paymentId}")
    public List<PaymentHistoryResponse> getHistory(@PathVariable Long paymentId) {
        return paymentHistoryService.getPaymentHistory(paymentId);
    }

}
