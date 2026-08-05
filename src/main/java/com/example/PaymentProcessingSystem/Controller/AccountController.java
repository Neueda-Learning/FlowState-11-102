package com.example.PaymentProcessingSystem.Controller;

import com.example.PaymentProcessingSystem.Service.AccountService;
import com.example.PaymentProcessingSystem.model.Account;
import org.springframework.web.bind.annotation.*;
import com.example.PaymentProcessingSystem.model.AuditRecord;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/account")
public class AccountController {
private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }
    @GetMapping("/")
    //get all accounts
    public List<Account> getAllAccounts() {
        return accountService.getAllAccounts();
    }

    //get account by id
    @GetMapping("/{accountId}")
    public Optional<Account> getAccountById(@PathVariable Long accountId) {
        return accountService.getAccountById(accountId);
    }

    //get account by account number
    @GetMapping("/number/{accountNumber}")
    public Optional<Account> getAccountByNumber(@PathVariable String accountNumber) {
        return accountService.getAccountByAccountNumber(accountNumber);
    }
    //create account
    @PostMapping("/")
    public Account createAccount(@RequestBody Account account) {
        return accountService.createAccount(account);
    }
    @PutMapping("/{accountId}")
    public Account updateAccount(@PathVariable Long accountId, @RequestBody Account account) {
        return accountService.updateAccount(accountId, account);}

    @GetMapping("/{accountId}/history")
    public List<AuditRecord> getAccountHistory(@PathVariable Long accountId) {
        return accountService.getAccountHistory(accountId);
    }


}
