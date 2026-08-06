package com.example.PaymentProcessingSystem.Controller;

import com.example.PaymentProcessingSystem.Dto.PaymentRequest;
import com.example.PaymentProcessingSystem.Dto.PaymentResponse;
import com.example.PaymentProcessingSystem.Service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    @Test
    void createPayment_returnsCreatedPaymentResponse() {
        PaymentRequest request = new PaymentRequest("ACC1001", "ACC1002", new BigDecimal("100.00"), "INR", "idem-1");
        PaymentResponse expected = response(1L, "PAY000001", "COMPLETED", "Success");

        when(paymentService.createPayment(request)).thenReturn(expected);

        PaymentResponse actual = paymentController.createPayment(request);

        assertEquals(expected, actual);
    }

    @Test
    void getPayment_delegatesToServiceById() {
        PaymentResponse expected = response(2L, "PAY000002", "CREATED", "Queued");
        when(paymentService.getPaymentById(2L)).thenReturn(expected);

        PaymentResponse actual = paymentController.getPayment(2L);

        assertEquals(expected, actual);
    }

    @Test
    void getPaymentByReference_delegatesToService() {
        PaymentResponse expected = response(3L, "PAY000003", "FAILED", "Gateway failure");
        when(paymentService.getPaymentByReference("PAY000003")).thenReturn(expected);

        PaymentResponse actual = paymentController.getPaymentByReference("PAY000003");

        assertEquals(expected, actual);
    }

    @Test
    void getPaymentByAccountId_returnsAccountPayments() {
        List<PaymentResponse> expected = List.of(
                response(10L, "PAY000010", "COMPLETED", "Success"),
                response(11L, "PAY000011", "FAILED", "Gateway failure")
        );
        when(paymentService.getPaymentsByAccountId(1L)).thenReturn(expected);

        List<PaymentResponse> actual = paymentController.getPaymentByAccountId(1L);

        assertEquals(2, actual.size());
        assertEquals("PAY000010", actual.get(0).payment_reference());
        assertEquals("PAY000011", actual.get(1).payment_reference());
    }

    @Test
    void cancelPayment_callsServiceOnce() {
        paymentController.cancelPayment(15L);

        verify(paymentService).cancelPayment(15L);
    }

    private static PaymentResponse response(Long paymentId, String reference, String status, String message) {
        return new PaymentResponse(
                paymentId,
                reference,
                1L,
                2L,
                new BigDecimal("100.00"),
                "INR",
                status,
                null,
                0,
                message
        );
    }
}


