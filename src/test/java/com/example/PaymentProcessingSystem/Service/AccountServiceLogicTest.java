package com.example.PaymentProcessingSystem.Service;

import com.example.PaymentProcessingSystem.Exception.AccountNotFoundException;
import com.example.PaymentProcessingSystem.Repository.AccountRepository;
import com.example.PaymentProcessingSystem.model.Account;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceLogicTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountServiceLogic accountService;

    // ----------------------------------------------------------------
    // getAllAccounts
    // ----------------------------------------------------------------

    @Test
    void getAllAccounts_returnsAllAccountsFromRepository() {
        List<Account> accounts = List.of(
                account(1L, "ACC1001", new BigDecimal("1000.00")),
                account(2L, "ACC1002", new BigDecimal("500.00"))
        );
        when(accountRepository.findAll()).thenReturn(accounts);

        List<Account> result = accountService.getAllAccounts();

        assertEquals(2, result.size());
        assertEquals("ACC1001", result.get(0).account_number());
        assertEquals("ACC1002", result.get(1).account_number());
    }

    @Test
    void getAllAccounts_emptyList_returnsEmpty() {
        when(accountRepository.findAll()).thenReturn(List.of());

        List<Account> result = accountService.getAllAccounts();

        assertTrue(result.isEmpty());
    }

    // ----------------------------------------------------------------
    // getAccountById
    // ----------------------------------------------------------------

    @Test
    void getAccountById_existingId_returnsAccount() {
        Account acc = account(1L, "ACC1001", new BigDecimal("1000.00"));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(acc));

        Optional<Account> result = accountService.getAccountById(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().account_id());
        assertEquals("ACC1001", result.get().account_number());
    }

    @Test
    void getAccountById_notFound_returnsEmpty() {
        when(accountRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<Account> result = accountService.getAccountById(999L);

        assertTrue(result.isEmpty());
    }

    // ----------------------------------------------------------------
    // getAccountByAccountNumber
    // ----------------------------------------------------------------

    @Test
    void getAccountByAccountNumber_existingNumber_returnsAccount() {
        Account acc = account(1L, "ACC1001", new BigDecimal("1000.00"));
        when(accountRepository.findByAccountNumber("ACC1001")).thenReturn(Optional.of(acc));

        Optional<Account> result = accountService.getAccountByAccountNumber("ACC1001");

        assertTrue(result.isPresent());
        assertEquals("ACC1001", result.get().account_number());
    }

    @Test
    void getAccountByAccountNumber_notFound_returnsEmpty() {
        when(accountRepository.findByAccountNumber("UNKNOWN")).thenReturn(Optional.empty());

        Optional<Account> result = accountService.getAccountByAccountNumber("UNKNOWN");

        assertTrue(result.isEmpty());
    }

    // ----------------------------------------------------------------
    // createAccount
    // ----------------------------------------------------------------

    @Test
    void createAccount_delegatesToRepositoryAndReturnsResult() {
        Account input = account(null, "ACC1003", new BigDecimal("0.00"));
        Account saved = account(3L, "ACC1003", new BigDecimal("0.00"));
        when(accountRepository.save(input)).thenReturn(saved);

        Account result = accountService.createAccount(input);

        assertEquals(3L, result.account_id());
        assertEquals("ACC1003", result.account_number());
        verify(accountRepository).save(input);
    }

    // ----------------------------------------------------------------
    // updateAccount
    // ----------------------------------------------------------------

    @Test
    void updateAccount_updatesWithCorrectAccountIdAndReturnsLatest() {
        Account incoming = account(null, "ACC1001", new BigDecimal("800.00"));
        Account stored  = account(1L,   "ACC1001", new BigDecimal("800.00"));

        when(accountRepository.findById(1L)).thenReturn(Optional.of(stored));

        Account result = accountService.updateAccount(1L, incoming);

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).update(captor.capture());

        Account updatedArg = captor.getValue();
        assertEquals(1L, updatedArg.account_id());             // id injected from path param
        assertEquals("ACC1001", updatedArg.account_number());
        assertEquals(new BigDecimal("800.00"), updatedArg.balance());

        assertEquals(1L, result.account_id());
    }

    @Test
    void updateAccount_accountNotFoundAfterUpdate_throwsException() {
        Account incoming = account(null, "ACC1001", new BigDecimal("800.00"));
        when(accountRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> accountService.updateAccount(1L, incoming));
    }

    @Test
    void updateAccount_preservesAllFieldsExceptId() {
        LocalDateTime now = LocalDateTime.now();
        Account incoming = new Account(
                null, "ACC1001", "Alice Updated", "alice@new.com",
                "9876543210", new BigDecimal("750.00"), "USD", "ACTIVE", 2, now, now
        );
        Account stored = account(1L, "ACC1001", new BigDecimal("750.00"));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(stored));

        accountService.updateAccount(1L, incoming);

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).update(captor.capture());

        Account updated = captor.getValue();
        assertEquals(1L,                      updated.account_id());
        assertEquals("Alice Updated",         updated.account_holder_name());
        assertEquals("alice@new.com",         updated.email());
        assertEquals("9876543210",            updated.phone_number());
        assertEquals(new BigDecimal("750.00"),updated.balance());
        assertEquals("USD",                   updated.currency());
        assertEquals("ACTIVE",                updated.status());
    }

    // ----------------------------------------------------------------
    // Helper
    // ----------------------------------------------------------------

    private static Account account(Long id, String number, BigDecimal balance) {
        return new Account(
                id,
                number,
                "Holder-" + number,
                (number != null ? number.toLowerCase() : "test") + "@example.com",
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

