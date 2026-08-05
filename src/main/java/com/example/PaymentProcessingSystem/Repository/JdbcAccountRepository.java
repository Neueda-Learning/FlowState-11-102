package com.example.PaymentProcessingSystem.Repository;

import com.example.PaymentProcessingSystem.model.Account;
import com.example.PaymentProcessingSystem.model.AuditRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public class JdbcAccountRepository implements  AccountRepository {
    private final JdbcTemplate jdbc;
    public JdbcAccountRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<Account> accountRowMapper = (rs, rowNum) ->
        new Account(
                rs.getLong("account_id"),
                rs.getString("account_number"),
                rs.getString("account_holder_name"),
                rs.getString("email"),
                rs.getString("phone_number"),
                rs.getBigDecimal("balance"),
                rs.getString("currency"),
                rs.getString("status"),
                rs.getInt("version"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime()
        );

    private final RowMapper<AuditRecord> auditRowMapper = (rs, rowNum) ->
            new AuditRecord(
                    rs.getLong("id"),
                    rs.getString("aggregate_type"),
                    rs.getString("aggregate_id"),
                    rs.getString("event_type"),
                    rs.getString("message"),
                    rs.getTimestamp("created_at").toLocalDateTime()
            );

    @Override
    public List<Account> findAll() {
        return jdbc.query("SELECT * FROM account", accountRowMapper);
    }

    @Override
    public Optional<Account> findById(Long account_id) {
        List<Account> results = jdbc.query(
                "SELECT * FROM account WHERE account_id = ?",
                accountRowMapper, account_id);
        return results.stream().findFirst();
    }

    @Override
    public Optional<Account> findByAccountNumber(String account_number) {
        List<Account> results = jdbc.query(
                "SELECT * FROM account WHERE account_number = ?",
                accountRowMapper, account_number);
        return results.stream().findFirst();
    }

    @Override
    public Account save(Account account) {
        jdbc.update(
                "INSERT INTO account (account_number, account_holder_name, email, phone_number, balance, currency, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)",
                account.account_number(), account.account_holder_name(), account.email(),
                account.phone_number(), account.balance(), account.currency(), account.status()
        );
        return findByAccountNumber(account.account_number()).orElseThrow();
    }

    @Override
    public Account update(Account account) {
        jdbc.update(
                "UPDATE account SET account_holder_name = ?, email = ?, phone_number = ?, balance = ?, currency = ?, status = ?, version = version + 1 " +
                "WHERE account_id = ?",
                account.account_holder_name(), account.email(), account.phone_number(),
                account.balance(), account.currency(), account.status(), account.account_id()
        );
        return account;
    }

    @Override
    public void updateStatus(Long account_id, String status) {
        jdbc.update(
                "UPDATE account SET status = ?, version = version + 1 WHERE account_id = ?",
                status, account_id
        );
    }

    @Override
    public Optional<Account>findByIdForUpdate(Long account_id) {

        String sql = "SELECT * FROM account WHERE account_id = ? FOR UPDATE";
        List<Account> accounts = jdbc.query(sql, accountRowMapper, account_id);
        return accounts.stream().findFirst();
    }

    @Override
    public List<AuditRecord> findAuditByAccountId(Long account_id) {
        return jdbc.query(
                "SELECT id, aggregate_type, aggregate_id, event_type, message, created_at FROM audit_log WHERE aggregate_type = ? AND aggregate_id = ? ORDER BY created_at DESC",
                auditRowMapper,
                "ACCOUNT",
                String.valueOf(account_id)
        );
    }

    private void insertAccountAudit(String aggregateId, String eventType, String message) {
        jdbc.update(
                "INSERT INTO audit_log (aggregate_type, aggregate_id, event_type, message) VALUES (?, ?, ?, ?)",
                "ACCOUNT",
                aggregateId,
                eventType,
                message
        );
    }

}
