package com.example.PaymentProcessingSystem.Controller;

import com.example.PaymentProcessingSystem.Service.AccountService;
import com.example.PaymentProcessingSystem.model.Account;
import com.example.PaymentProcessingSystem.model.AuditRecord;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

	@Mock
	private AccountService accountService;

	@InjectMocks
	private AccountController accountController;

	@Test
	void getAllAccounts_returnsAllAccounts() {
		List<Account> expected = List.of(
				account(1L, "ACC1001", new BigDecimal("1000.00")),
				account(2L, "ACC1002", new BigDecimal("500.00"))
		);
		when(accountService.getAllAccounts()).thenReturn(expected);

		List<Account> actual = accountController.getAllAccounts();

		assertEquals(2, actual.size());
		assertEquals("ACC1001", actual.get(0).account_number());
	}

	@Test
	void getAccountById_returnsOptionalAccount() {
		Account expected = account(3L, "ACC1003", new BigDecimal("50.00"));
		when(accountService.getAccountById(3L)).thenReturn(Optional.of(expected));

		Optional<Account> actual = accountController.getAccountById(3L);

		assertTrue(actual.isPresent());
		assertEquals("ACC1003", actual.get().account_number());
	}

	@Test
	void getAccountByNumber_returnsOptionalAccount() {
		Account expected = account(4L, "ACC1004", new BigDecimal("75.00"));
		when(accountService.getAccountByAccountNumber("ACC1004")).thenReturn(Optional.of(expected));

		Optional<Account> actual = accountController.getAccountByNumber("ACC1004");

		assertTrue(actual.isPresent());
		assertEquals(4L, actual.get().account_id());
	}

	@Test
	void createAccount_returnsSavedAccount() {
		Account input = account(null, "ACC2001", new BigDecimal("0.00"));
		Account saved = account(9L, "ACC2001", new BigDecimal("0.00"));
		when(accountService.createAccount(input)).thenReturn(saved);

		Account actual = accountController.createAccount(input);

		assertEquals(9L, actual.account_id());
	}

	@Test
	void updateAccount_delegatesWithPathId() {
		Account input = account(null, "ACC1001", new BigDecimal("800.00"));
		Account updated = account(1L, "ACC1001", new BigDecimal("800.00"));
		when(accountService.updateAccount(1L, input)).thenReturn(updated);

		Account actual = accountController.updateAccount(1L, input);

		assertEquals(1L, actual.account_id());
		verify(accountService).updateAccount(1L, input);
	}

	@Test
	void getAccountHistory_returnsAuditRecords() {
		List<AuditRecord> expected = List.of(
				new AuditRecord(1L, "ACCOUNT", "1", "UPDATED", "Balance changed", LocalDateTime.now())
		);
		when(accountService.getAccountHistory(1L)).thenReturn(expected);

		List<AuditRecord> actual = accountController.getAccountHistory(1L);

		assertEquals(1, actual.size());
		assertEquals("UPDATED", actual.get(0).event_type());
	}

	private static Account account(Long id, String number, BigDecimal balance) {
		return new Account(
				id,
				number,
				"Holder-" + number,
				(number == null ? "holder" : number.toLowerCase()) + "@example.com",
				"9999999999",
				balance,
				"INR",
				"ACTIVE",
				0,
				LocalDateTime.now(),
				LocalDateTime.now()
		);
	}
}

