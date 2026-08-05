package com.example.PaymentProcessingSystem.Service;

import com.example.PaymentProcessingSystem.Dto.PaymentHistoryResponse;
import com.example.PaymentProcessingSystem.Repository.PaymentHistoryRepository;
import com.example.PaymentProcessingSystem.model.PaymentHistory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentHistoryServiceImplTest {

    @Mock
    private PaymentHistoryRepository paymentHistoryRepository;

    @InjectMocks
    private PaymentHistoryServiceImpl paymentHistoryService;

    @Test
    void getPaymentHistory_returnsMappedHistoryEntries() {
        LocalDateTime t1 = LocalDateTime.now().minusMinutes(2);
        LocalDateTime t2 = LocalDateTime.now().minusMinutes(1);

        List<PaymentHistory> rows = List.of(
                new PaymentHistory(1L, 10L, "CREATED", "Payment created", t1),
                new PaymentHistory(2L, 10L, "COMPLETED", "Payment completed", t2)
        );

        when(paymentHistoryRepository.findByPaymentId(10L)).thenReturn(rows);

        List<PaymentHistoryResponse> result = paymentHistoryService.getPaymentHistory(10L);

        assertEquals(2, result.size());
        assertEquals("CREATED", result.get(0).status());
        assertEquals("Payment created", result.get(0).message());
        assertEquals(t1, result.get(0).created_at());

        assertEquals("COMPLETED", result.get(1).status());
        assertEquals("Payment completed", result.get(1).message());
        assertEquals(t2, result.get(1).created_at());
    }

    @Test
    void getPaymentHistory_noHistory_returnsEmptyList() {
        when(paymentHistoryRepository.findByPaymentId(99L)).thenReturn(List.of());

        List<PaymentHistoryResponse> result = paymentHistoryService.getPaymentHistory(99L);

        assertTrue(result.isEmpty());
    }
}

