package com.example.PaymentProcessingSystem.Service;

import com.example.PaymentProcessingSystem.Client.GatewayClient;
import com.example.PaymentProcessingSystem.Dto.GatewayResponse;
import com.example.PaymentProcessingSystem.Dto.PaymentRequest;
import com.example.PaymentProcessingSystem.Dto.PaymentResponse;
import com.example.PaymentProcessingSystem.Exception.DuplicatePaymentEXception;
import com.example.PaymentProcessingSystem.Exception.InsufficientBalanceException;
import com.example.PaymentProcessingSystem.Exception.InvalidPaymentException;
import com.example.PaymentProcessingSystem.Repository.AccountRepository;
import com.example.PaymentProcessingSystem.Repository.PaymentHistoryRepository;
import com.example.PaymentProcessingSystem.Repository.PaymentRepository;
import com.example.PaymentProcessingSystem.model.Account;
import com.example.PaymentProcessingSystem.model.Payment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

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
    void createPayment_success_updatesStatusAndBalances() {
        PaymentRequest request = new PaymentRequest(
                "ACC1001", "ACC1002", new BigDecimal("100.00"), "INR", "idem-success-1"
        );

        Account source = account(1L, "ACC1001", new BigDecimal("1000.00"));
        Account destination = account(2L, "ACC1002", new BigDecimal("500.00"));

        when(paymentRepository.findByIdempotencyKey("idem-success-1")).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumber("ACC1001")).thenReturn(Optional.of(source));
        when(accountRepository.findByAccountNumber("ACC1002")).thenReturn(Optional.of(destination));

        Payment created = payment(10L, null, 1L, 2L, new BigDecimal("100.00"), "INR", "CREATED", null, 0, "idem-success-1");
        when(paymentRepository.save(any(Payment.class))).thenReturn(created);
        when(gatewayClient.processPayment(any())).thenReturn(new GatewayResponse("SUCCESS", "Accepted"));

        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(source));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(destination));

        Payment completed = payment(10L, "PAY000010", 1L, 2L, new BigDecimal("100.00"), "INR", "COMPLETED", null, 0, "idem-success-1");
        when(paymentRepository.findById(10L)).thenReturn(Optional.of(completed));

        PaymentResponse response = paymentService.createPayment(request);

        assertEquals(10L, response.payment_id());
        assertEquals("PAY000010", response.payment_reference());
        assertEquals("COMPLETED", response.status());
        assertEquals("Accepted", response.message());

        verify(paymentRepository).updatePaymentReference(10L, "PAY000010");
        verify(paymentRepository).updatePaymentStatus(10L, "VALIDATED");
        verify(paymentRepository).updatePaymentStatus(10L, "SENT");
        verify(paymentRepository).updatePaymentStatus(10L, "COMPLETED");

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository, times(2)).update(accountCaptor.capture());
        List<Account> updatedAccounts = accountCaptor.getAllValues();

        Account updatedSource = updatedAccounts.stream()
                .filter(a -> a.account_id().equals(1L))
                .findFirst()
                .orElseThrow();
        Account updatedDestination = updatedAccounts.stream()
                .filter(a -> a.account_id().equals(2L))
                .findFirst()
                .orElseThrow();

        assertEquals(new BigDecimal("900.00"), updatedSource.balance());
        assertEquals(new BigDecimal("600.00"), updatedDestination.balance());
    }

    @Test
    void createPayment_duplicateIdempotency_throwsConflictException() {
        Payment existing = payment(1L, "PAY000001", 1L, 2L, new BigDecimal("50.00"), "INR", "COMPLETED", null, 0, "idem-dup");
        when(paymentRepository.findByIdempotencyKey("idem-dup")).thenReturn(Optional.of(existing));

        PaymentRequest request = new PaymentRequest(
                "ACC1001", "ACC1002", new BigDecimal("50.00"), "INR", "idem-dup"
        );

        assertThrows(DuplicatePaymentEXception.class, () -> paymentService.createPayment(request));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(gatewayClient, never()).processPayment(any());
    }

    @Test
    void createPayment_insufficientBalance_throwsBadRequestException() {
        Account source = account(1L, "ACC1001", new BigDecimal("10.00"));
        Account destination = account(2L, "ACC1002", new BigDecimal("500.00"));

        when(paymentRepository.findByIdempotencyKey("idem-low-balance")).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumber("ACC1001")).thenReturn(Optional.of(source));
        when(accountRepository.findByAccountNumber("ACC1002")).thenReturn(Optional.of(destination));

        PaymentRequest request = new PaymentRequest(
                "ACC1001", "ACC1002", new BigDecimal("100.00"), "INR", "idem-low-balance"
        );

        assertThrows(InsufficientBalanceException.class, () -> paymentService.createPayment(request));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void createPayment_gatewayFailure_marksPaymentFailedAndIncrementsRetry() {
        PaymentRequest request = new PaymentRequest(
                "ACC1001", "ACC1002", new BigDecimal("100.00"), "INR", "idem-gw-fail"
        );

        Account source = account(1L, "ACC1001", new BigDecimal("1000.00"));
        Account destination = account(2L, "ACC1002", new BigDecimal("500.00"));

        when(paymentRepository.findByIdempotencyKey("idem-gw-fail")).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumber("ACC1001")).thenReturn(Optional.of(source));
        when(accountRepository.findByAccountNumber("ACC1002")).thenReturn(Optional.of(destination));

        Payment created = payment(11L, null, 1L, 2L, new BigDecimal("100.00"), "INR", "CREATED", null, 0, "idem-gw-fail");
        when(paymentRepository.save(any(Payment.class))).thenReturn(created);
        when(gatewayClient.processPayment(any())).thenReturn(new GatewayResponse("FAILED", "Gateway timeout"));

        Payment failed = payment(11L, "PAY000011", 1L, 2L, new BigDecimal("100.00"), "INR", "FAILED", "FAILED", 1, "idem-gw-fail");
        when(paymentRepository.findById(11L)).thenReturn(Optional.of(failed));

        PaymentResponse response = paymentService.createPayment(request);

        verify(paymentRepository).updatePaymentStatusAndFailureReason(11L, "FAILED", "FAILED");
        verify(paymentRepository).incrementRetryCount(11L);
        verify(accountRepository, never()).update(any(Account.class));

        assertEquals("FAILED", response.status());
        assertEquals("Gateway timeout", response.message());
        assertEquals("FAILED", response.failure_reason());
    }

    @Test
    void cancelPayment_completedStatus_throwsInvalidPaymentException() {
        Payment completed = payment(12L, "PAY000012", 1L, 2L, new BigDecimal("100.00"), "INR", "COMPLETED", null, 0, "idem-x");
        when(paymentRepository.findById(12L)).thenReturn(Optional.of(completed));

        assertThrows(InvalidPaymentException.class, () -> paymentService.cancelPayment(12L));
        verify(paymentRepository, never()).cancelPayment(any(Long.class));
    }

    @Test
    void getPaymentByReference_returnsMappedResponse() {
        Payment payment = payment(13L, "PAY000013", 1L, 2L, new BigDecimal("80.00"), "INR", "COMPLETED", null, 0, "idem-r");
        when(paymentRepository.findByPaymentReference("PAY000013")).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.getPaymentByReference("PAY000013");

        assertEquals(13L, response.payment_id());
        assertEquals("PAY000013", response.payment_reference());
        assertEquals("COMPLETED", response.status());
        assertTrue(response.message().contains("Success"));
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

