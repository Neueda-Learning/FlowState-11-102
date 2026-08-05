package com.example.PaymentProcessingSystem.Service;
import com.example.PaymentProcessingSystem.Dto.PaymentHistoryResponse;
import java.util.List;

public interface PaymentHistoryService {
    List<PaymentHistoryResponse> getPaymentHistory(Long paymentId);
}
