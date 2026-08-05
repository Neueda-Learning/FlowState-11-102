package com.example.PaymentProcessingSystem.Service;

import com.example.PaymentProcessingSystem.Repository.AccountRepository;
import com.example.PaymentProcessingSystem.model.Account;
import com.example.PaymentProcessingSystem.model.AuditRecord;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AccountServiceLogic implements AccountService {
    private final AccountRepository accountRepository;
    public AccountServiceLogic(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }
    @Override
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }
    @Override
    public Optional<Account> getAccountById(Long accountId) {
        return accountRepository.findById(accountId);
    }
    @Override
    public Optional<Account> getAccountByAccountNumber( String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber);
    }
    @Override
    public Account createAccount(Account account) {
        return accountRepository.save(account);
    }
    @Override
    public Account updateAccount(Long accountId, Account account) {

        Account existing = accountRepository.findById(accountId).orElseThrow();
        Account updated = new Account(
                accountId,
                account.account_number(),
                account.account_holder_name(),
                account.email(),
                account.phone_number(),
                account.balance(),
                account.currency(),
                account.status(),
                account.version(),
                account.created_at(),
                account.updated_at()
        );
        accountRepository.update(updated);
        return accountRepository.findById(accountId).orElseThrow();
    }

    @Override
    public List<AuditRecord> getAccountHistory(Long accountId) {
        return accountRepository.findAuditByAccountId(accountId);
    }


}
