package com.example.PaymentProcessingSystem.Repository;

import com.example.PaymentProcessingSystem.model.PaymentHistory;

import java.util.List;

public interface PaymentHistoryRepository {
    PaymentHistory save(PaymentHistory paymentHistory);
    List<PaymentHistory> findByPaymentId(Long paymentId);
}