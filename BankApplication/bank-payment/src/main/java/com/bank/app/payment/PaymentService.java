package com.bank.app.payment;

import com.bank.app.api.ResponseCode;

import java.util.UUID;

public interface PaymentService {
	
    ResponseCode processPayment(PaymentServiceOperation payment);

    ResponseCode processPaymentStatus(UUID transactionId);
	
}
