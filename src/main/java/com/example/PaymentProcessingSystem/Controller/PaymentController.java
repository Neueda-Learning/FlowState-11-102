package com.example.PaymentProcessingSystem.Controller;

import com.example.PaymentProcessingSystem.Dto.PaymentRequest;
import com.example.PaymentProcessingSystem.Dto.PaymentResponse;
import com.example.PaymentProcessingSystem.Service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
public class PaymentController {
    private final PaymentService paymentService;
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse createPayment(@RequestBody PaymentRequest request) {
        return paymentService.createPayment(request);
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse getPayment(@PathVariable Long paymentId) {
        return paymentService.getPaymentById(paymentId);
    }

    @GetMapping("/reference/{paymentReference}")
    public PaymentResponse getPaymentByReference(@PathVariable String paymentReference) {
        return paymentService.getPaymentByReference(paymentReference);
    }

    @GetMapping("/account/{accountId}")
    public List<PaymentResponse> getPaymentByAccountId(@PathVariable Long accountId) {
        return paymentService.getPaymentsByAccountId(accountId);
    }
    @PatchMapping("/{paymentId}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelPayment(@PathVariable Long paymentId) {
        paymentService.cancelPayment(paymentId);
    }

}
