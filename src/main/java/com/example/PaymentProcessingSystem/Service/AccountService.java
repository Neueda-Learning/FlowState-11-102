package com.example.PaymentProcessingSystem.Service;

import com.example.PaymentProcessingSystem.model.Account;
import com.example.PaymentProcessingSystem.model.AuditRecord;

import java.util.List;
import java.util.Optional;

public interface AccountService {
    List<Account> getAllAccounts();

    Optional<Account> getAccountById(Long accountId);

    Optional<Account> getAccountByAccountNumber(String accountNumber);

    Account createAccount(Account account);

    Account updateAccount(Long accountId, Account account);

    List<AuditRecord> getAccountHistory(Long accountId);
}
