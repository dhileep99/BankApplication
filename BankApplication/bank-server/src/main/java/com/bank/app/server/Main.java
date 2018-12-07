package com.bank.app.server;

import com.bank.app.controller.PaymentController;
import com.bank.app.controller.PaymentStatusController;
import com.bank.app.netty.DispatchHttpHandlerBuilder;
import com.bank.app.netty.HttpHandler;
import com.bank.app.netty.HttpServerBuilder;
import com.bank.app.netty.NettyServer;
import com.bank.app.payment.Currency;
import com.bank.app.payment.DummyAccountStorage;
import com.bank.app.payment.DummyPaymentService;
import com.bank.app.payment.PaymentService;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        Map<Long, DummyAccountStorage.Account> accounts = createDefaultAccounts();

        DummyAccountStorage accountStorage = new DummyAccountStorage(accounts);

        PaymentService paymentService = new DummyPaymentService(accountStorage);

        HttpHandler dispatcher = new DispatchHttpHandlerBuilder()
                .bind("/rest/json/payment", new PaymentController(paymentService))
                .bind("/rest/json/status", new PaymentStatusController(paymentService))
                .build();

        NettyServer server = new HttpServerBuilder("sinap")
                .handler(dispatcher)
                .group(2, 16)
                .localAddress("0.0.0.0", 8080)
                .build();

        server.startSync().channel().closeFuture().syncUninterruptibly();
    }

    static Map<Long, DummyAccountStorage.Account> createDefaultAccounts() {
        Map<Long, DummyAccountStorage.Account> accounts = Collections.synchronizedMap(new HashMap<>());
        for (int i = 0; i < 100; i++) {
            long accountId = i + 1;
            DummyAccountStorage.Account account = new DummyAccountStorage.Account(accountId, Currency.RUB, 0, 10000);
            accounts.put(accountId, account);
        }
        return accounts;
    }
}
