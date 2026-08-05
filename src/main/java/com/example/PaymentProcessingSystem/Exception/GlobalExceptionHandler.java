package com.example.PaymentProcessingSystem.Exception;

import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ApiError> handleAccountNotFound(AccountNotFoundException ex){
        ApiError error = new ApiError(LocalDateTime.now(),HttpStatus.NOT_FOUND.value(),"Account Not Found" ,ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ApiError> handlePaymentNotFound(PaymentNotFoundException ex){
        ApiError error = new ApiError(LocalDateTime.now(),HttpStatus.NOT_FOUND.value(),"Payment Not Found" ,ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }
    @ExceptionHandler(DuplicatePaymentEXception.class)
    public ResponseEntity<ApiError> handleDuplicatePayment(DuplicatePaymentEXception ex){
        ApiError error = new ApiError(LocalDateTime.now(),HttpStatus.CONFLICT.value(),"Duplicate Payment" ,ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }
    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ApiError> handleInsufficientBalance(InsufficientBalanceException ex){
        ApiError error = new ApiError(LocalDateTime.now(),HttpStatus.BAD_REQUEST.value(),"Insufficient Balance" ,ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
    @ExceptionHandler(InvalidPaymentException.class)
    public ResponseEntity<ApiError> handleInvalidPayment(InvalidPaymentException ex){
        ApiError error = new ApiError(LocalDateTime.now(),HttpStatus.BAD_REQUEST.value(),"Invalid Payment" ,ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
    @ExceptionHandler(GatewayException.class)
    public ResponseEntity<ApiError> handleGateway(GatewayException ex){
        ApiError error = new ApiError(LocalDateTime.now(),HttpStatus.SERVICE_UNAVAILABLE.value(),"Gateway Error" ,ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.SERVICE_UNAVAILABLE);
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception ex){
        ApiError error = new ApiError(LocalDateTime.now(),HttpStatus.INTERNAL_SERVER_ERROR.value(),"Internal Server Error" ,ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
