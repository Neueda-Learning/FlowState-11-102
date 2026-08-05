package com.example.PaymentProcessingSystem.Repository;

import com.example.PaymentProcessingSystem.model.PaymentHistory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JdbcPaymentHistoryRepository implements PaymentHistoryRepository {

    private final JdbcTemplate jdbc;

    public JdbcPaymentHistoryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<PaymentHistory> rowMapper = (rs, rowNum) ->
        new PaymentHistory(
                rs.getLong("history_id"),
                rs.getLong("payment_id"),
                rs.getString("status"),
                rs.getString("message"),
                rs.getTimestamp("created_at").toLocalDateTime()
        );

    @Override
    public PaymentHistory save(PaymentHistory paymentHistory) {
        jdbc.update( """
            INSERT INTO payment_history (payment_id, status, message)
            VALUES (?, ?, ?)
            """,
                paymentHistory.payment_id(),
                paymentHistory.status(),
                paymentHistory.message()
        );
        return paymentHistory;
    }

    @Override
    public List<PaymentHistory> findByPaymentId(Long paymentId) {
        return jdbc.query(
                "SELECT * FROM payment_history WHERE payment_id = ? ORDER BY created_at",
                rowMapper,
                paymentId
        );
    }

}
