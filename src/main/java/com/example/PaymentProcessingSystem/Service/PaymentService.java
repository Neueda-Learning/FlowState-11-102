package com.example.PaymentProcessingSystem.Service;

import com.example.PaymentProcessingSystem.Dto.PaymentRequest;
import com.example.PaymentProcessingSystem.Dto.PaymentResponse;
import org.springframework.stereotype.Service;

import java.util.List;

public interface PaymentService {
    PaymentResponse createPayment(PaymentRequest request);
    List<PaymentResponse> getAllPayments();
    PaymentResponse getPaymentById(Long payment_id);
    PaymentResponse getPaymentByReference(String Payment_reference);
    List<PaymentResponse>getPaymentsByAccountId(Long account_id);
    void cancelPayment(Long payment_id);

}
