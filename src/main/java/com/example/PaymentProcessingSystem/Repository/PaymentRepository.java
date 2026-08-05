package com.example.PaymentProcessingSystem.Repository;

import com.example.PaymentProcessingSystem.model.Payment;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository {
    //create payment
    Payment save(Payment payment);

    //view all payments
    List<Payment> findAll();

    //find payment by id
    Optional<Payment> findById(Long payment_id);

    //find payment by paymentreference
    Optional<Payment> findByPaymentReference(String payment_reference);

    //find payment using idempotency key
    Optional<Payment> findByIdempotencyKey(String idempotency_key);

    //find all payments of an account
    List<Payment> findBySourceAccountId(Long source_account_id);

    //find all payments of an account
    List<Payment> findByAccountId(Long destination_account_id);

    //update payment reference after generating payment_id
    void updatePaymentReference(Long payment_id, String payment_reference);

    //update complete payment
    void updatePayment(Payment payment);

    //update only payment status
    void updatePaymentStatus(Long payment_id, String status);

    //update only payment failure reason
    void updatePaymentFailureReason(Long payment_id, String failure_reason);

    void updatePaymentStatusAndFailureReason(Long payment_id, String status, String failure_reason);

    //increment retry count
    void incrementRetryCount(Long payment_id);

    //cancel payment (can be cancelled only if status is created,validated)
    void cancelPayment(Long payment_id);

}
