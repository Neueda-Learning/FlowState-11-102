package com.example.PaymentProcessingSystem.Service;

import com.example.PaymentProcessingSystem.Client.GatewayClient;
import com.example.PaymentProcessingSystem.Dto.GatewayResponse;
import com.example.PaymentProcessingSystem.Dto.PaymentRequest;
import com.example.PaymentProcessingSystem.Dto.PaymentResponse;
import com.example.PaymentProcessingSystem.Exception.AccountNotFoundException;
import com.example.PaymentProcessingSystem.Exception.DuplicatePaymentEXception;
import com.example.PaymentProcessingSystem.Exception.InsufficientBalanceException;
import com.example.PaymentProcessingSystem.Exception.InvalidPaymentException;
import com.example.PaymentProcessingSystem.Exception.PaymentNotFoundException;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
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

    @SuppressWarnings("unchecked")
    private static ScheduledFuture<?> completedFuture() {
        return (ScheduledFuture<?>) org.mockito.Mockito.mock(ScheduledFuture.class);
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
    void createPayment_sourceAccountNotFound_throwsAccountNotFoundException() {
        when(paymentRepository.findByIdempotencyKey("idem-src-missing")).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumber("ACC404")).thenReturn(Optional.empty());

        PaymentRequest request = new PaymentRequest(
                "ACC404", "ACC1002", new BigDecimal("50.00"), "INR", "idem-src-missing"
        );

        assertThrows(AccountNotFoundException.class, () -> paymentService.createPayment(request));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void createPayment_sameSourceAndDestination_throwsInvalidPaymentException() {
        Account same = account(1L, "ACC1001", new BigDecimal("500.00"));
        when(paymentRepository.findByIdempotencyKey("idem-same-account")).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumber("ACC1001")).thenReturn(Optional.of(same));

        PaymentRequest request = new PaymentRequest(
                "ACC1001", "ACC1001", new BigDecimal("50.00"), "INR", "idem-same-account"
        );

        assertThrows(InvalidPaymentException.class, () -> paymentService.createPayment(request));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void createPayment_inactiveDestination_throwsInvalidPaymentException() {
        Account source = account(1L, "ACC1001", new BigDecimal("1000.00"));
        Account destination = accountWithStatus(2L, "ACC1002", new BigDecimal("500.00"), "INACTIVE");

        when(paymentRepository.findByIdempotencyKey("idem-inactive-destination")).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumber("ACC1001")).thenReturn(Optional.of(source));
        when(accountRepository.findByAccountNumber("ACC1002")).thenReturn(Optional.of(destination));

        PaymentRequest request = new PaymentRequest(
                "ACC1001", "ACC1002", new BigDecimal("100.00"), "INR", "idem-inactive-destination"
        );

        assertThrows(InvalidPaymentException.class, () -> paymentService.createPayment(request));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void createPayment_destinationAccountNotFound_throwsAccountNotFoundException() {
        Account source = account(1L, "ACC1001", new BigDecimal("1000.00"));

        when(paymentRepository.findByIdempotencyKey("idem-dst-missing")).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumber("ACC1001")).thenReturn(Optional.of(source));
        when(accountRepository.findByAccountNumber("ACC404")).thenReturn(Optional.empty());

        PaymentRequest request = new PaymentRequest(
                "ACC1001", "ACC404", new BigDecimal("100.00"), "INR", "idem-dst-missing"
        );

        assertThrows(AccountNotFoundException.class, () -> paymentService.createPayment(request));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(gatewayClient, never()).processPayment(any());
    }

    @Test
    void createPayment_amountNull_throwsInvalidPaymentException() {
        Account source = account(1L, "ACC1001", new BigDecimal("1000.00"));
        Account destination = account(2L, "ACC1002", new BigDecimal("500.00"));

        when(paymentRepository.findByIdempotencyKey("idem-null-amount")).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumber("ACC1001")).thenReturn(Optional.of(source));
        when(accountRepository.findByAccountNumber("ACC1002")).thenReturn(Optional.of(destination));

        PaymentRequest request = new PaymentRequest(
                "ACC1001", "ACC1002", null, "INR", "idem-null-amount"
        );

        assertThrows(InvalidPaymentException.class, () -> paymentService.createPayment(request));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(gatewayClient, never()).processPayment(any());
    }

    @Test
    void createPayment_amountZero_throwsInvalidPaymentException() {
        Account source = account(1L, "ACC1001", new BigDecimal("1000.00"));
        Account destination = account(2L, "ACC1002", new BigDecimal("500.00"));

        when(paymentRepository.findByIdempotencyKey("idem-zero-amount")).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumber("ACC1001")).thenReturn(Optional.of(source));
        when(accountRepository.findByAccountNumber("ACC1002")).thenReturn(Optional.of(destination));

        PaymentRequest request = new PaymentRequest(
                "ACC1001", "ACC1002", BigDecimal.ZERO, "INR", "idem-zero-amount"
        );

        assertThrows(InvalidPaymentException.class, () -> paymentService.createPayment(request));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(gatewayClient, never()).processPayment(any());
    }

    @Test
    void createPayment_amountNegative_throwsInvalidPaymentException() {
        Account source = account(1L, "ACC1001", new BigDecimal("1000.00"));
        Account destination = account(2L, "ACC1002", new BigDecimal("500.00"));

        when(paymentRepository.findByIdempotencyKey("idem-negative-amount")).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumber("ACC1001")).thenReturn(Optional.of(source));
        when(accountRepository.findByAccountNumber("ACC1002")).thenReturn(Optional.of(destination));

        PaymentRequest request = new PaymentRequest(
                "ACC1001", "ACC1002", new BigDecimal("-1.00"), "INR", "idem-negative-amount"
        );

        assertThrows(InvalidPaymentException.class, () -> paymentService.createPayment(request));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(gatewayClient, never()).processPayment(any());
    }

    @Test
    void createPayment_sourceInactive_throwsInvalidPaymentException() {
        Account source = accountWithStatus(1L, "ACC1001", new BigDecimal("1000.00"), "INACTIVE");
        Account destination = account(2L, "ACC1002", new BigDecimal("500.00"));

        when(paymentRepository.findByIdempotencyKey("idem-source-inactive")).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumber("ACC1001")).thenReturn(Optional.of(source));
        when(accountRepository.findByAccountNumber("ACC1002")).thenReturn(Optional.of(destination));

        PaymentRequest request = new PaymentRequest(
                "ACC1001", "ACC1002", new BigDecimal("100.00"), "INR", "idem-source-inactive"
        );

        assertThrows(InvalidPaymentException.class, () -> paymentService.createPayment(request));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(gatewayClient, never()).processPayment(any());
    }

    @Test
    void createPayment_currencyMismatch_throwsInvalidPaymentException() {
        Account source = account(1L, "ACC1001", new BigDecimal("1000.00"));
        Account destination = account(2L, "ACC1002", new BigDecimal("500.00"));

        when(paymentRepository.findByIdempotencyKey("idem-currency-mismatch")).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumber("ACC1001")).thenReturn(Optional.of(source));
        when(accountRepository.findByAccountNumber("ACC1002")).thenReturn(Optional.of(destination));

        PaymentRequest request = new PaymentRequest(
                "ACC1001", "ACC1002", new BigDecimal("100.00"), "USD", "idem-currency-mismatch"
        );

        assertThrows(InvalidPaymentException.class, () -> paymentService.createPayment(request));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(gatewayClient, never()).processPayment(any());
    }

    @Test
    void createPayment_schedulesContinuationAfterTenSeconds() {
        ScheduledExecutorService scheduler = org.mockito.Mockito.mock(ScheduledExecutorService.class);
        ReflectionTestUtils.setField(paymentService, "scheduler", scheduler);

        Account source = account(1L, "ACC1001", new BigDecimal("1000.00"));
        Account destination = account(2L, "ACC1002", new BigDecimal("500.00"));
        Payment created = payment(20L, null, 1L, 2L, new BigDecimal("100.00"), "INR", "CREATED", null, 0, "idem-schedule-only");

        when(paymentRepository.findByIdempotencyKey("idem-schedule-only")).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumber("ACC1001")).thenReturn(Optional.of(source));
        when(accountRepository.findByAccountNumber("ACC1002")).thenReturn(Optional.of(destination));
        when(paymentRepository.save(any(Payment.class))).thenReturn(created);
        doReturn(completedFuture()).when(scheduler).schedule(any(Runnable.class), eq(10L), eq(TimeUnit.SECONDS));

        PaymentResponse response = paymentService.createPayment(new PaymentRequest(
                "ACC1001", "ACC1002", new BigDecimal("100.00"), "INR", "idem-schedule-only"
        ));

        assertEquals("CREATED", response.status());
        assertTrue(response.message().contains("processing started"));
        verify(scheduler).schedule(any(Runnable.class), eq(10L), eq(TimeUnit.SECONDS));
    }

    @Test
    void scheduledContinuation_successFlow_updatesStatusesAndBalances() {
        ScheduledExecutorService scheduler = org.mockito.Mockito.mock(ScheduledExecutorService.class);
        ReflectionTestUtils.setField(paymentService, "scheduler", scheduler);

        Account source = account(1L, "ACC1001", new BigDecimal("1000.00"));
        Account destination = account(2L, "ACC1002", new BigDecimal("500.00"));
        Payment created = payment(30L, null, 1L, 2L, new BigDecimal("100.00"), "INR", "CREATED", null, 0, "idem-scheduled-success");
        Payment completed = payment(30L, "PAY000030", 1L, 2L, new BigDecimal("100.00"), "INR", "COMPLETED", null, 0, "idem-scheduled-success");

        when(paymentRepository.findByIdempotencyKey("idem-scheduled-success")).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumber("ACC1001")).thenReturn(Optional.of(source));
        when(accountRepository.findByAccountNumber("ACC1002")).thenReturn(Optional.of(destination));
        when(paymentRepository.save(any(Payment.class))).thenReturn(created);
        doReturn(completedFuture()).when(scheduler).schedule(any(Runnable.class), eq(10L), eq(TimeUnit.SECONDS));

        when(paymentRepository.findById(30L)).thenReturn(Optional.of(created), Optional.of(completed));
        when(gatewayClient.processPayment(any())).thenReturn(new GatewayResponse("SUCCESS", "Accepted"));
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(source));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(destination));

        paymentService.createPayment(new PaymentRequest(
                "ACC1001", "ACC1002", new BigDecimal("100.00"), "INR", "idem-scheduled-success"
        ));

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).schedule(runnableCaptor.capture(), eq(10L), eq(TimeUnit.SECONDS));

        runnableCaptor.getValue().run();

        verify(paymentRepository).updatePaymentReference(30L, "PAY000030");
        verify(paymentRepository).updatePaymentStatus(30L, "VALIDATED");
        verify(paymentRepository).updatePaymentStatus(30L, "SENT");
        verify(paymentRepository).updatePaymentStatus(30L, "COMPLETED");

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository, times(2)).update(accountCaptor.capture());
        List<Account> updatedAccounts = accountCaptor.getAllValues();

        Account updatedSource = updatedAccounts.stream().filter(a -> a.account_id().equals(1L)).findFirst().orElseThrow();
        Account updatedDestination = updatedAccounts.stream().filter(a -> a.account_id().equals(2L)).findFirst().orElseThrow();

        assertEquals(new BigDecimal("900.00"), updatedSource.balance());
        assertEquals(new BigDecimal("600.00"), updatedDestination.balance());
    }

    @Test
    void scheduledContinuation_gatewayFailure_updatesFailureWithoutBalanceChange() {
        ScheduledExecutorService scheduler = org.mockito.Mockito.mock(ScheduledExecutorService.class);
        ReflectionTestUtils.setField(paymentService, "scheduler", scheduler);

        Account source = account(1L, "ACC1001", new BigDecimal("1000.00"));
        Account destination = account(2L, "ACC1002", new BigDecimal("500.00"));
        Payment created = payment(31L, null, 1L, 2L, new BigDecimal("100.00"), "INR", "CREATED", null, 0, "idem-scheduled-fail");
        Payment failed = payment(31L, "PAY000031", 1L, 2L, new BigDecimal("100.00"), "INR", "FAILED", "FAILED", 1, "idem-scheduled-fail");

        when(paymentRepository.findByIdempotencyKey("idem-scheduled-fail")).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumber("ACC1001")).thenReturn(Optional.of(source));
        when(accountRepository.findByAccountNumber("ACC1002")).thenReturn(Optional.of(destination));
        when(paymentRepository.save(any(Payment.class))).thenReturn(created);
        doReturn(completedFuture()).when(scheduler).schedule(any(Runnable.class), eq(10L), eq(TimeUnit.SECONDS));

        when(paymentRepository.findById(31L)).thenReturn(Optional.of(created), Optional.of(failed));
        when(gatewayClient.processPayment(any())).thenReturn(new GatewayResponse("FAILED", "Gateway timeout"));

        paymentService.createPayment(new PaymentRequest(
                "ACC1001", "ACC1002", new BigDecimal("100.00"), "INR", "idem-scheduled-fail"
        ));

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).schedule(runnableCaptor.capture(), eq(10L), eq(TimeUnit.SECONDS));

        runnableCaptor.getValue().run();

        verify(paymentRepository).updatePaymentReference(31L, "PAY000031");
        verify(paymentRepository).updatePaymentStatus(31L, "VALIDATED");
        verify(paymentRepository).updatePaymentStatus(31L, "SENT");
        verify(paymentRepository).updatePaymentStatusAndFailureReason(31L, "FAILED", "FAILED");
        verify(paymentRepository).incrementRetryCount(31L);
        verify(accountRepository, never()).update(any(Account.class));
    }


    @Test
    void cancelPayment_createdStatus_cancelsPayment() {
        Payment created = payment(14L, "PAY000014", 1L, 2L, new BigDecimal("50.00"), "INR", "CREATED", null, 0, "idem-created-cancel");
        when(paymentRepository.findById(14L)).thenReturn(Optional.of(created));

        paymentService.cancelPayment(14L);

        verify(paymentRepository).cancelPayment(14L);
        verify(paymentHistoryRepository).save(any());
    }

    @Test
    void cancelPayment_validatedStatus_cancelsPayment() {
        Payment validated = payment(15L, "PAY000015", 1L, 2L, new BigDecimal("60.00"), "INR", "VALIDATED", null, 0, "idem-validated-cancel");
        when(paymentRepository.findById(15L)).thenReturn(Optional.of(validated));

        paymentService.cancelPayment(15L);

        verify(paymentRepository).cancelPayment(15L);
        verify(paymentHistoryRepository).save(any());
    }

    @Test
    void cancelPayment_sentStatus_throwsInvalidPaymentException() {
        Payment sent = payment(16L, "PAY000016", 1L, 2L, new BigDecimal("60.00"), "INR", "SENT", null, 0, "idem-sent");
        when(paymentRepository.findById(16L)).thenReturn(Optional.of(sent));

        assertThrows(InvalidPaymentException.class, () -> paymentService.cancelPayment(16L));
        verify(paymentRepository, never()).cancelPayment(any(Long.class));
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

    @Test
    void getPaymentById_notFound_throwsPaymentNotFoundException() {
        when(paymentRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () -> paymentService.getPaymentById(404L));
    }

    @Test
    void cancelPayment_notFound_throwsPaymentNotFoundException() {
        when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () -> paymentService.cancelPayment(999L));
        verify(paymentRepository, never()).cancelPayment(999L);
    }

    private static Account account(Long id, String number, BigDecimal balance) {
        return accountWithStatus(id, number, balance, "ACTIVE");
    }

    private static Account accountWithStatus(Long id, String number, BigDecimal balance, String status) {
        return new Account(
                id,
                number,
                "Holder-" + number,
                number.toLowerCase() + "@example.com",
                "9999999999",
                balance,
                "INR",
                status,
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


