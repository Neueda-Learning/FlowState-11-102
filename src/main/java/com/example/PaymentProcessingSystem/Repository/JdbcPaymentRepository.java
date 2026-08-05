package com.example.PaymentProcessingSystem.Repository;

import com.example.PaymentProcessingSystem.model.Payment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcPaymentRepository implements PaymentRepository{
    private final JdbcTemplate jdbc;
    public JdbcPaymentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }
    private final RowMapper<Payment> paymentRowMapper = (rs, rowNum) ->
            new Payment(
                    rs.getLong("payment_id"),
                    rs.getString("payment_reference"),
                    rs.getLong("source_account_id"),
                    rs.getLong("destination_account_id"),
                    rs.getBigDecimal("amount"),
                    rs.getString("currency"),
                    rs.getString("status"),
                    rs.getString("failure_reason"),
                    rs.getInt("retry_count"),
                    rs.getString("idempotency_key"),
                    rs.getTimestamp("created_at").toLocalDateTime(),
                    rs.getTimestamp("updated_at").toLocalDateTime()
            );
    @Override
    public Payment save(Payment payment)
    {
            String sql= """
                INSERT INTO payment (source_account_id, destination_account_id, amount, currency, status, failure_reason, retry_count, idempotency_key)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setLong(1, payment.source_account_id());
            ps.setLong(2, payment.destination_account_id());
            ps.setBigDecimal(3, payment.amount());
            ps.setString(4, payment.currency());
            ps.setString(5, payment.status());
            ps.setString(6, payment.failure_reason());
            ps.setInt(7, payment.retry_count());
            ps.setString(8, payment.idempotency_key());
            return ps;


        },keyHolder);
        Long generatedId = keyHolder.getKey().longValue();
        return new Payment(
                generatedId,
                null,
                payment.source_account_id(),
                payment.destination_account_id(),
                payment.amount(),
                payment.currency(),
                payment.status(),
                payment.failure_reason(),
                payment.retry_count(),
                payment.idempotency_key(),
                null,
                null );
    }

    @Override
    public List<Payment> findAll() {
        String sql ="SELECT * FROM payment ORDER BY payment_id DESC";
        return jdbc.query(sql, paymentRowMapper);

    }
    @Override
    public Optional<Payment> findById(Long payment_id) {
        String sql = "SELECT * FROM payment WHERE payment_id = ?";
        List<Payment> payments = jdbc.query(sql, paymentRowMapper, payment_id);
        return payments.stream().findFirst();
    }
    @Override
    public Optional<Payment> findByPaymentReference(String payment_reference) {
        String sql = "SELECT * FROM payment WHERE payment_reference = ?";
        List<Payment> payments = jdbc.query(sql, paymentRowMapper, payment_reference);
        return payments.stream().findFirst();
    }
    @Override
    public Optional<Payment>findByIdempotencyKey(String idempotency_key) {
        String sql = "SELECT * FROM payment WHERE idempotency_key = ?";
        List<Payment> payments = jdbc.query(sql, paymentRowMapper, idempotency_key);
        return payments.stream().findFirst();
    }
    @Override
    public List<Payment>findBySourceAccountId(Long source_account_id) {
        String sql = "SELECT * FROM payment WHERE source_account_id = ? ORDER BY created_at DESC";
        return jdbc.query(sql, paymentRowMapper, source_account_id);

    }

    @Override
    public List<Payment> findByAccountId(Long account_id) {
        String sql = "SELECT * FROM payment WHERE source_account_id=? OR destination_account_id = ? ORDER BY created_at DESC";
        return jdbc.query(sql, paymentRowMapper, account_id, account_id);

    }

    @Override
    public void updatePaymentReference(Long payment_id,String payment_reference) {
        String sql = "UPDATE payment SET payment_reference = ? WHERE payment_id = ?";
        jdbc.update(sql, payment_reference, payment_id);
    }

    @Override
    public void updatePayment(Payment payment) {
        String sql = "UPDATE payment SET payment_reference= ?,source_account_id= ?,destination_account_id= ?,amount= ?,currency=?,status = ?, failure_reason = ?, retry_count = ?,idempotency_key= ? WHERE payment_id = ?";
        jdbc.update(sql, payment.payment_reference(), payment.source_account_id(), payment.destination_account_id(), payment.amount(), payment.currency(), payment.status(), payment.failure_reason(), payment.retry_count(), payment.idempotency_key(), payment.payment_id());
    }

    @Override
    public void updatePaymentStatus(Long payment_id, String status) {
        String sql = "UPDATE payment SET status = ? WHERE payment_id = ?";
        jdbc.update(sql, status, payment_id);
    }
    @Override
    public void updatePaymentFailureReason(Long payment_id, String failure_reason) {
        String sql = "UPDATE payment SET failure_reason = ? WHERE payment_id = ?";
        jdbc.update(sql, failure_reason, payment_id);
    }
    @Override
    public void updatePaymentStatusAndFailureReason(Long payment_id, String status, String failure_reason) {
        String sql = "UPDATE payment SET status = ?, failure_reason = ? WHERE payment_id = ?";
        jdbc.update(sql, status, failure_reason, payment_id);
    }
    @Override
    public void incrementRetryCount(Long payment_id) {
        String sql = "UPDATE payment SET retry_count = retry_count + 1 WHERE payment_id = ?";
        jdbc.update(sql, payment_id);
    }
    @Override
    public void cancelPayment(Long payment_id) {
        String sql = "UPDATE payment SET status = 'FAILED', failure_reason = 'PAYMENT_CANCELLED' WHERE payment_id = ? AND status IN('CREATED','VALIDATED')";
        jdbc.update(sql, payment_id);
    }
}
