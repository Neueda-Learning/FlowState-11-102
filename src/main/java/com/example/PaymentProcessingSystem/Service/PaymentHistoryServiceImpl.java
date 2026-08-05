package com.example.PaymentProcessingSystem.Service;

import com.example.PaymentProcessingSystem.Dto.PaymentHistoryResponse;
import com.example.PaymentProcessingSystem.Repository.PaymentHistoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaymentHistoryServiceImpl implements PaymentHistoryService {
    private final PaymentHistoryRepository paymentHistoryRepository;

    public PaymentHistoryServiceImpl(PaymentHistoryRepository paymentHistoryRepository) {
        this.paymentHistoryRepository = paymentHistoryRepository;
    }

    @Override
    public List<PaymentHistoryResponse> getPaymentHistory(Long paymentId) {
        return paymentHistoryRepository.findByPaymentId(paymentId)
                .stream()
                .map(history -> new PaymentHistoryResponse(
                        history.status(),
                        history.message(),
                        history.created_at()
                ))
                .toList();
    }

}
