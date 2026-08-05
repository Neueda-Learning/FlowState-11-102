package com.example.PaymentProcessingSystem.Repository;

import com.example.PaymentProcessingSystem.model.Account;
import com.example.PaymentProcessingSystem.model.AuditRecord;

import java.util.List;
import java.util.Optional;

public interface AccountRepository {
    List<Account> findAll();
    Optional<Account> findById(Long account_id);
    Optional<Account> findByAccountNumber(String account_number);
    Account save(Account account);
    Account update(Account account);
    Optional<Account> findByIdForUpdate(Long account_id);
    void updateStatus(Long account_id, String status);
    List<AuditRecord> findAuditByAccountId(Long account_id);
}
