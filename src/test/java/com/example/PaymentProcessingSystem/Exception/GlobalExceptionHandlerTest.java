package com.example.PaymentProcessingSystem.Exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@Test
	void handleAccountNotFound_returnsNotFoundResponse() {
		ResponseEntity<ApiError> response = handler.handleAccountNotFound(new AccountNotFoundException("Account missing"));
		ApiError body = response.getBody();

		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertNotNull(body);
		assertEquals("Account Not Found", body.error());
		assertEquals("Account missing", body.message());
	}

	@Test
	void handlePaymentNotFound_returnsNotFoundResponse() {
		ResponseEntity<ApiError> response = handler.handlePaymentNotFound(new PaymentNotFoundException("Payment not found"));
		ApiError body = response.getBody();

		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertNotNull(body);
		assertEquals("Payment Not Found", body.error());
		assertEquals("Payment not found", body.message());
	}

	@Test
	void handleDuplicatePayment_returnsConflictResponse() {
		ResponseEntity<ApiError> response = handler.handleDuplicatePayment(new DuplicatePaymentEXception("Duplicate"));
		ApiError body = response.getBody();

		assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
		assertNotNull(body);
		assertEquals("Duplicate Payment", body.error());
		assertEquals("Duplicate", body.message());
	}

	@Test
	void handleInvalidPayment_returnsBadRequestResponse() {
		ResponseEntity<ApiError> response = handler.handleInvalidPayment(new InvalidPaymentException("Invalid amount"));
		ApiError body = response.getBody();

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertNotNull(body);
		assertEquals("Invalid Payment", body.error());
		assertEquals("Invalid amount", body.message());
	}

	@Test
	void handleInsufficientBalance_returnsBadRequestResponse() {
		ResponseEntity<ApiError> response = handler.handleInsufficientBalance(new InsufficientBalanceException("Not enough balance"));
		ApiError body = response.getBody();

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertNotNull(body);
		assertEquals("Insufficient Balance", body.error());
		assertEquals("Not enough balance", body.message());
	}

	@Test
	void handleGateway_returnsServiceUnavailableResponse() {
		ResponseEntity<ApiError> response = handler.handleGateway(new GatewayException("Gateway down"));
		ApiError body = response.getBody();

		assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
		assertNotNull(body);
		assertEquals("Gateway Error", body.error());
		assertEquals("Gateway down", body.message());
	}

	@Test
	void handleGenericException_returnsInternalServerErrorResponse() {
		ResponseEntity<ApiError> response = handler.handleGenericException(new RuntimeException("Unexpected failure"));
		ApiError body = response.getBody();

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertNotNull(body);
		assertEquals("Internal Server Error", body.error());
		assertEquals("Unexpected failure", body.message());
	}
}

