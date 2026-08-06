package com.example.PaymentProcessingSystem.Controller;

import com.example.PaymentProcessingSystem.Service.AccountService;
import com.example.PaymentProcessingSystem.model.Account;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountApiControllerTest {

	@Mock
	private AccountService accountService;

	@InjectMocks
	private AccountController accountController;

	@Test
	void getAccountById_missingAccount_returnsEmptyOptional() {
		when(accountService.getAccountById(404L)).thenReturn(Optional.empty());

		Optional<Account> result = accountController.getAccountById(404L);

		assertTrue(result.isEmpty());
	}
}

