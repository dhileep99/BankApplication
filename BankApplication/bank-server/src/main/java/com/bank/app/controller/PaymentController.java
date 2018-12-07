package com.bank.app.controller;

import com.revolut.sinap.api.ResponseCode;
import com.revolut.sinap.api.json.PaymentRequest;
import com.revolut.sinap.api.json.PaymentResponse;
import com.revolut.sinap.payment.Currencies;
import com.revolut.sinap.payment.Currency;
import com.revolut.sinap.payment.Payment;
import com.revolut.sinap.payment.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class PaymentController extends AbstractJsonController<PaymentRequest, PaymentResponse> {
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        super(PaymentRequest.class);
        this.paymentService = paymentService;
    }

    @Override
    protected PaymentResponse process(PaymentRequest req) {
        return doProcess(req);
    }

    private PaymentResponse doProcess(PaymentRequest req) {
        PaymentRequest.Account sourceAccount = req.getSource();
        PaymentRequest.Account targetAccount = req.getTarget();

        Currency sourceCurrency = Currency.valueOf(sourceAccount.getCurrency());
        Currency targetCurrency = Currency.valueOf(targetAccount.getCurrency());

        long sourceAmount = Currencies.parseAmount(sourceAccount.getAmount(), sourceCurrency);
        long targetAmount = Currencies.parseAmount(targetAccount.getAmount(), targetCurrency);

        Payment payment = new Payment(UUID.fromString(req.getTransactionId()))
                .setSourceAccountId(sourceAccount.getId())
                .setSourceAmount(sourceAmount)
                .setSourceCurrency(sourceCurrency)
                .setTargetAccountId(targetAccount.getId())
                .setTargetAmount(targetAmount)
                .setTargetCurrency(targetCurrency)
                .setComment(req.getComment());

        ResponseCode responseCode = paymentService.processPayment(payment);

        return new PaymentResponse()
                .setTransactionId(req.getTransactionId())
                .setResponseCode(responseCode);
    }
}
