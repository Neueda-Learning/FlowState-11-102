package com.example.PaymentProcessingSystem.Controller;

import com.example.PaymentProcessingSystem.Dto.PaymentHistoryResponse;
import com.example.PaymentProcessingSystem.Service.PaymentHistoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentHistoryControllerTest {

	@Mock
	private PaymentHistoryService paymentHistoryService;

	@InjectMocks
	private PaymentHistoryController paymentHistoryController;

	@Test
	void getHistory_returnsPaymentHistoryResponseList() {
		LocalDateTime createdAt = LocalDateTime.now();
		List<PaymentHistoryResponse> expected = List.of(
				new PaymentHistoryResponse("CREATED", "Payment created", createdAt),
				new PaymentHistoryResponse("COMPLETED", "Payment completed", createdAt.plusSeconds(1))
		);
		when(paymentHistoryService.getPaymentHistory(20L)).thenReturn(expected);

		List<PaymentHistoryResponse> actual = paymentHistoryController.getHistory(20L);

		assertEquals(2, actual.size());
		assertEquals("CREATED", actual.get(0).status());
		assertEquals("COMPLETED", actual.get(1).status());
	}
}

