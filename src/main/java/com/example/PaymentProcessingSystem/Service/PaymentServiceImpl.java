package com.example.PaymentProcessingSystem.Service;

import com.example.PaymentProcessingSystem.Client.GatewayClient;
import com.example.PaymentProcessingSystem.Dto.GatewayRequest;
import com.example.PaymentProcessingSystem.Dto.GatewayResponse;
import com.example.PaymentProcessingSystem.Dto.PaymentRequest;
import com.example.PaymentProcessingSystem.Dto.PaymentResponse;
import com.example.PaymentProcessingSystem.Exception.*;
import com.example.PaymentProcessingSystem.Repository.AccountRepository;
import com.example.PaymentProcessingSystem.Repository.PaymentHistoryRepository;
import com.example.PaymentProcessingSystem.Repository.PaymentRepository;
import com.example.PaymentProcessingSystem.model.Account;
import com.example.PaymentProcessingSystem.model.AccountPair;
import com.example.PaymentProcessingSystem.model.Payment;
import com.example.PaymentProcessingSystem.model.PaymentHistory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    public  final AccountRepository accountRepository;
    private final GatewayClient gatewayClient;
    private final PaymentHistoryRepository paymentHistoryRepository;

    public PaymentServiceImpl(PaymentRepository paymentRepository, AccountRepository accountRepository,
                              GatewayClient gatewayClient,PaymentHistoryRepository paymentHistoryRepository) {
        this.paymentRepository = paymentRepository;
        this.accountRepository = accountRepository;
        this.gatewayClient = gatewayClient;
        this.paymentHistoryRepository = paymentHistoryRepository;
    }
    @Override
    @Transactional
    public PaymentResponse createPayment(PaymentRequest request)
    {
        //step 1:validate request
        AccountPair accountPair = validateRequestAndGetAccounts(request);
        Account sourceAccount = accountPair.sourceAccount();
        Account destinationAccount = accountPair.destinationAccount();

        //step 2:create payment object
        Payment payment =createPaymentEntity(request,accountPair);

        //step 3:save payment(status=CREATED)
        Payment savedPayment = paymentRepository.save(payment);
        recordHistory(savedPayment.payment_id(), "CREATED", "Payment created");

        //step 4:Generate payment reference
        String paymentReference = generatePaymentReference(savedPayment.payment_id());
        paymentRepository.updatePaymentReference(savedPayment.payment_id(), paymentReference);

        //step 5:update status to validated
        paymentRepository.updatePaymentStatus(savedPayment.payment_id(), "VALIDATED");
        recordHistory(savedPayment.payment_id(), "VALIDATED", "Payment validated");

        //step 6:update status to sent
        paymentRepository.updatePaymentStatus(savedPayment.payment_id(), "SENT");
        recordHistory(savedPayment.payment_id(), "SENT", "Payment sent to gateway");

        //step 7:send payment to gateway
        GatewayResponse gatewayResponse = sendToGateway(savedPayment, paymentReference);

        //step 8:process gateway response
        return processGatewayResponse(savedPayment,paymentReference, gatewayResponse);


    }

    private AccountPair validateRequestAndGetAccounts(PaymentRequest request) {

        //check duplicate payment
        paymentRepository.findByIdempotencyKey(request.idempotency_key()).ifPresent(payment -> {
            throw new DuplicatePaymentEXception("Duplicate payment request .");
        });

        Account sourceAccount=accountRepository.findByAccountNumber(request.source_account_number())
                .orElseThrow(()->new AccountNotFoundException("Source account not found"));
        Account destinationAccount=accountRepository.findByAccountNumber(request.destination_account_number())
                .orElseThrow(()->new AccountNotFoundException("Destination account not found"));

        if(sourceAccount.account_id().equals(destinationAccount.account_id())){
            throw new InvalidPaymentException("Source and destination accounts cannot be the same");
        }

        if(request.amount()==null || request.amount().signum()<=0)
        {
            throw new InvalidPaymentException("Amount must be greater than zero.");
        }
        if(!"ACTIVE".equals(sourceAccount.status()) || !"ACTIVE".equals(destinationAccount.status())){
            throw new InvalidPaymentException("Both source and destination accounts must be active.");
        }
        if(!sourceAccount.currency().equals(request.currency())){
            throw new InvalidPaymentException("currency mismatch with source account.");
        }
        if(sourceAccount.balance().compareTo(request.amount())<0){
            throw new InsufficientBalanceException("Insufficient balance in source account.");
        }
        return new AccountPair(sourceAccount,destinationAccount);

    }

    private Payment createPaymentEntity(PaymentRequest request,AccountPair accountPair) {
        return new Payment(
                null,
                null,
                accountPair.sourceAccount().account_id(),
                accountPair.destinationAccount().account_id(),
                request.amount(),
                request.currency(),
                "CREATED",
                null,
                0,
                request.idempotency_key(),
                null,
                null
        );
    }
    private String generatePaymentReference(Long paymentId) {
        return "PAY" + String.format("%06d", paymentId);
    }
    private GatewayResponse sendToGateway(Payment payment, String paymentReference) {
        GatewayRequest gatewayRequest=new GatewayRequest(paymentReference,payment.amount());
        return gatewayClient.processPayment(gatewayRequest);
    }
    private PaymentResponse processGatewayResponse(Payment payment, String paymentReference, GatewayResponse gatewayResponse) {
        if("SUCCESS".equalsIgnoreCase(gatewayResponse.status())){

            return completePayment(payment, paymentReference, gatewayResponse.message());
        }
        return failPayment(payment, paymentReference,gatewayResponse.status(), gatewayResponse.message());
    }
    private PaymentResponse completePayment(Payment payment, String paymentReference, String message) {
        // update status to completed and persist history
        paymentRepository.updatePaymentStatus(payment.payment_id(), "COMPLETED");
        recordHistory(payment.payment_id(), "COMPLETED", message == null ? "Payment completed" : message);

        // update account balances
        updateBalances(payment.source_account_id(), payment.destination_account_id(), payment.amount());
        Payment updatedPayment = paymentRepository.findById(payment.payment_id())
                .orElseThrow(() -> new RuntimeException("Payment not found after completion."));
        return buildPaymentResponse(updatedPayment, paymentReference, message);
    }
    private PaymentResponse failPayment(Payment payment, String paymentReference,String failureReason,String message) {

        paymentRepository.updatePaymentStatusAndFailureReason(payment.payment_id(),"FAILED", failureReason);
        paymentRepository.incrementRetryCount(payment.payment_id());
        recordHistory(payment.payment_id(), "FAILED", message == null ? "Payment failed" : message);

        Payment updatedPayment = paymentRepository.findById(payment.payment_id())
                .orElseThrow(() -> new RuntimeException("Payment not found."));
        return buildPaymentResponse(updatedPayment, paymentReference, message);

    }

    private void updateBalances(Long sourceAccountId, Long destinationAccountId, BigDecimal amount) {
        Long first=Math.min(sourceAccountId,destinationAccountId);
        Long second=Math.max(sourceAccountId,destinationAccountId);
        Account firstAccount = accountRepository.findByIdForUpdate(first)
                .orElseThrow(() -> new AccountNotFoundException("Source account not found."));
        Account secondAccount = accountRepository.findByIdForUpdate(second)
                .orElseThrow(() -> new AccountNotFoundException("Destination account not found."));
        Account sourceAccount =first.equals(sourceAccountId)?firstAccount:secondAccount;
        Account destinationAccount =first.equals(destinationAccountId)?firstAccount:secondAccount;
        if (sourceAccount.balance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient balance in source account.");
        }
        Account updatedSource=new Account(
                sourceAccount.account_id(),
                sourceAccount.account_number(),
                sourceAccount.account_holder_name(),
                sourceAccount.email(),
                sourceAccount.phone_number(),
                sourceAccount.balance().subtract(amount),
                sourceAccount.currency(),
                sourceAccount.status(),
                sourceAccount.version(),
                sourceAccount.created_at(),
                sourceAccount.updated_at());
        Account updatedDestination=new Account(
                destinationAccount.account_id(),
                destinationAccount.account_number(),
                destinationAccount.account_holder_name(),
                destinationAccount.email(),
                destinationAccount.phone_number(),
                destinationAccount.balance().add(amount),
                destinationAccount.currency(),
                destinationAccount.status(),
                destinationAccount.version(),
                destinationAccount.created_at(),
                destinationAccount.updated_at());
        accountRepository.update(updatedSource);
        accountRepository.update(updatedDestination);

    }

    private PaymentResponse buildPaymentResponse(Payment payment, String paymentReference, String message) {
        return new PaymentResponse(
                payment.payment_id(),
                paymentReference,
                payment.source_account_id(),
                payment.destination_account_id(),
                payment.amount(),
                payment.currency(),
                payment.status(),
                payment.failure_reason(),
                payment.retry_count(),
                message
        );
    }

    @Override
    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(payment -> buildPaymentResponse(payment, payment.payment_reference(), "Success"))
                .toList();
    }

    @Override
    public PaymentResponse getPaymentById(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));
        return buildPaymentResponse(payment, payment.payment_reference(), "Success");
    }

    public PaymentResponse getPaymentByReference(String paymentReference) {
        Payment payment = paymentRepository.findByPaymentReference(paymentReference)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));
        return buildPaymentResponse(payment, payment.payment_reference(), "Success");
    }

    public List<PaymentResponse> getPaymentsByAccountId(Long account_id) {
        accountRepository.findById(account_id)
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));
        return paymentRepository.findByAccountId(account_id).stream()
                .map(payment -> buildPaymentResponse(payment, payment.payment_reference(), "Success"))
                .toList();
    }

    public void cancelPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));
        if (!payment.status().equals("CREATED") && !payment.status().equals("VALIDATED")) {
            throw new InvalidPaymentException("Only payments with status 'CREATED' or 'VALIDATED' can be cancelled.");
        }
        paymentRepository.cancelPayment(paymentId);
        recordHistory(paymentId, "FAILED", "Payment cancelled");
    }

    private void recordHistory(Long paymentId, String status, String message) {
        paymentHistoryRepository.save(
                new PaymentHistory(
                        null,
                        paymentId,
                        status,
                        message,
                        null
                )
        );
    }

}
