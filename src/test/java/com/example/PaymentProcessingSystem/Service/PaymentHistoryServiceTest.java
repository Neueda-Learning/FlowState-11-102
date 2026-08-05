package com.example.PaymentProcessingSystem.Service;

import com.example.PaymentProcessingSystem.Client.GatewayClient;
import com.example.PaymentProcessingSystem.Dto.PaymentResponse;
import com.example.PaymentProcessingSystem.Exception.AccountNotFoundException;
import com.example.PaymentProcessingSystem.Repository.AccountRepository;
import com.example.PaymentProcessingSystem.Repository.PaymentHistoryRepository;
import com.example.PaymentProcessingSystem.Repository.PaymentRepository;
import com.example.PaymentProcessingSystem.model.Account;
import com.example.PaymentProcessingSystem.model.Payment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentHistoryServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private GatewayClient gatewayClient;

    @Mock
    private PaymentHistoryRepository paymentHistoryRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    void getPaymentsByAccountId_existingAccount_returnsPaymentHistory() {
        Account account = account(1L, "ACC1001", new BigDecimal("1000.00"));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        List<Payment> history = List.of(
                payment(21L, "PAY000021", 1L, 2L, new BigDecimal("150.00"), "INR", "COMPLETED", null, 0, "idem-h-1"),
                payment(22L, "PAY000022", 3L, 1L, new BigDecimal("75.00"), "INR", "FAILED", "FAILED", 1, "idem-h-2")
        );
        when(paymentRepository.findByAccountId(1L)).thenReturn(history);

        List<PaymentResponse> result = paymentService.getPaymentsByAccountId(1L);

        assertEquals(2, result.size());
        assertEquals(21L, result.get(0).payment_id());
        assertEquals("PAY000021", result.get(0).payment_reference());
        assertEquals("COMPLETED", result.get(0).status());
        assertEquals("Success", result.get(0).message());

        assertEquals(22L, result.get(1).payment_id());
        assertEquals("FAILED", result.get(1).status());
        assertEquals("FAILED", result.get(1).failure_reason());
    }

    @Test
    void getPaymentsByAccountId_existingAccountNoHistory_returnsEmptyList() {
        Account account = account(1L, "ACC1001", new BigDecimal("1000.00"));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(paymentRepository.findByAccountId(1L)).thenReturn(List.of());

        List<PaymentResponse> result = paymentService.getPaymentsByAccountId(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    void getPaymentsByAccountId_accountMissing_throwsAccountNotFound() {
        when(accountRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> paymentService.getPaymentsByAccountId(999L));
        verify(paymentRepository, never()).findByAccountId(any(Long.class));
    }

    private static Account account(Long id, String number, BigDecimal balance) {
        return new Account(
                id,
                number,
                "Holder-" + number,
                number.toLowerCase() + "@example.com",
                "9999999999",
                balance,
                "INR",
                "ACTIVE",
                0,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private static Payment payment(
            Long id,
            String reference,
            Long sourceId,
            Long destinationId,
            BigDecimal amount,
            String currency,
            String status,
            String failureReason,
            Integer retryCount,
            String idempotencyKey
    ) {
        return new Payment(
                id,
                reference,
                sourceId,
                destinationId,
                amount,
                currency,
                status,
                failureReason,
                retryCount,
                idempotencyKey,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}

