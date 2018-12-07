package com.bank.app.payment;

import com.bank.app.api.ResponseCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class DummyAccountStorage {
    private static final Logger logger = LoggerFactory.getLogger(DummyAccountStorage.class);

    private final Map<UUID, TransactionReference> transactions = Collections.synchronizedMap(new HashMap<>());
    private final Map<Long, Account> accounts;

    public DummyAccountStorage(Map<Long, Account> accounts) {
        this.accounts = Objects.requireNonNull(accounts, "accounts");
    }

    public ResponseCode processPayment(PaymentServiceOperation payment) {
        UUID transactionId = payment.transactionId();
        long sourceAccountId = payment.getSourceAccountId();
        long sourceAmount = payment.getSourceAmount();
        Currency sourceCurrency = payment.getSourceCurrency();
        long targetAccountId = payment.getTargetAccountId();
        Currency targetCurrency = payment.getTargetCurrency();
        long targetAmount = payment.getTargetAmount();

        checkAccounts(sourceAccountId, targetAccountId);
        checkAmount(sourceAmount, sourceCurrency, targetAmount, targetCurrency);
        Thread currentThread = Thread.currentThread();
        do {
            TransactionReference ref = transactions.computeIfAbsent(transactionId, k -> new TransactionReference(currentThread));
            boolean evictRef = false;
            try {
                synchronized (ref) {
                    while (ref.thread != null && ref.thread != currentThread) {
                        assert ref.state == TransactionState.INITIAL;
                        assert ref.transaction == null;
                        try {
                            ref.wait(1000L);
                        } catch (InterruptedException e) {
                            throw new RuntimeException("interrupted");
                        }
                    }

                    if (ref.thread == null) {
                        if (ref.state == TransactionState.ROLLED_BACK) {
                            evictRef = true;
                            continue;
                        }
                        assert ref.state == TransactionState.COMMITED;
                        ResponseCode responseCode = ref.transaction.responseCode;
                        if (responseCode == ResponseCode.SUCCESS) {
                            return ResponseCode.DUPLICATE_SUCCESS;
                        } else {
                            return responseCode;
                        }

                    }

                    assert ref.thread == currentThread;

                    Account sourceAccount = accounts.get(sourceAccountId);
                    if (sourceAccount == null || sourceAccount.currency != sourceCurrency) {
                        return ResponseCode.BAD_SOURCE_ACCOUNT;
                    }
                    Account targetAccount = accounts.get(targetAccountId);
                    if (targetAccount == null || targetAccount.currency != targetCurrency) {
                        return ResponseCode.BAD_TARGET_ACCOUNT;
                    }
                    Account firstLockAccount;
                    Account secondLockAccount;
                    if (sourceAccountId < targetAccountId) {
                        firstLockAccount = sourceAccount;
                        secondLockAccount = targetAccount;
                    } else {
                        assert sourceAccountId > targetAccountId;
                        firstLockAccount = targetAccount;
                        secondLockAccount = sourceAccount;
                    }

                    synchronized (firstLockAccount) {
                        synchronized (secondLockAccount) {
                            if (!sourceAccount.ensureAvailableToWithdraw(sourceAmount)) {
                                logger.warn("Not Enough Balance on Account {} : {}, txn {}",
                                        sourceAccountId, sourceAccount.getBalance(), transactionId);
                                ref.rollback();
                                return ResponseCode.NO_MONEY;
                            }

                            Transaction transaction = new Transaction(transactionId, ResponseCode.SUCCESS,
                                    sourceAccountId, sourceAmount, sourceCurrency,
                                    targetAccountId, targetAmount, targetCurrency,
                                    payment.getComment());
                            sourceAccount.enroll(-sourceAmount);
                            targetAccount.enroll(targetAmount);

                            ref.commit(transaction);
                        }
                    }

                    assert ref.state == TransactionState.COMMITED;
                    assert ref.transaction != null;
                    ref.notifyAll();
                    return ResponseCode.SUCCESS;
                }
            } finally {
                if (evictRef) {
                    transactions.remove(transactionId, ref);
                }
            }
        } while (true);
    }

    private static void checkAccounts(long sourceAccountId, long targetAccountId) {
        if (sourceAccountId <= 0L) {
            throw new IllegalArgumentException("Illegal Source Account ID : " + sourceAccountId);
        }
        if (targetAccountId <= 0L) {
            throw new IllegalArgumentException("Illegal Target Account ID : " + targetAccountId);
        }
        if (sourceAccountId == targetAccountId) {
            throw new IllegalArgumentException("Transactions On The Same Accounts Are Forbidden : " + sourceAccountId);
        }
    }

    private static void checkAmount(long sourceAmount, Currency sourceCurrency, long targetAmount, Currency targetCurrency) {
        if (sourceCurrency == null || targetCurrency == null) {
            throw new IllegalArgumentException("Currency Not Set.");
        }
        if (sourceCurrency != targetCurrency) {
            throw new IllegalArgumentException("Cross Currency Payments Are Forbidden : " + sourceCurrency + " " + targetCurrency);
        }
        if (sourceAmount <= 0 || targetAmount <= 0) {
            throw new IllegalArgumentException("Illegal Amounts : " + sourceAmount + " " + targetAmount);
        }
        if (sourceAmount != targetAmount) {
            throw new IllegalArgumentException("Source And Target Amount Differences : " + sourceAmount + " " + targetAmount);
        }
    }

    public ResponseCode getPaymentStatus(UUID transactionId) {
        TransactionReference ref = transactions.get(transactionId);
        if (ref == null) {
            return ResponseCode.NOT_FOUND;
        }
        synchronized (ref) {
            if (ref.state == TransactionState.COMMITED) {
                return ref.transaction.responseCode;
            }
        }
        return ResponseCode.NOT_FOUND;
    }

    private static class TransactionReference {
        private Thread thread;
        private TransactionState state;
        private Transaction transaction;

        TransactionReference(Thread thread) {
            this.thread = thread;
            this.state = TransactionState.INITIAL;
        }

        private void commit(Transaction transaction) {
            assert Thread.holdsLock(this);
            assert this.thread == Thread.currentThread();
            assert this.state == TransactionState.INITIAL;
            assert this.transaction == null;
            this.thread = null;
            this.state = TransactionState.COMMITED;
            this.transaction = transaction;
        }

        private void rollback() {
            assert Thread.holdsLock(this);
            assert this.thread != null;
            assert this.state == TransactionState.INITIAL;
            assert this.transaction == null;
            this.thread = null;
            this.state = TransactionState.ROLLED_BACK;
        }

    }

    private enum TransactionState {
        INITIAL,
        COMMITED,
        ROLLED_BACK
    }

    private static class Transaction {
		
        private final UUID transactionId;
        private final ResponseCode responseCode;
        private final long sourceAccountId;
        private final long sourceAmount;
        private final Currency sourceCurrency;
        private final long targetAccountId;
        private final long targetAmount;
        private final Currency targetCurrency;
        private final String comment;

        private Transaction(UUID transactionId, ResponseCode responseCode,
                            long sourceAccountId, long sourceAmount, Currency sourceCurrency,
                            long targetAccountId, long targetAmount, Currency targetCurrency,
                            String comment) {
            this.transactionId = transactionId;
            this.responseCode = responseCode;
            this.sourceAccountId = sourceAccountId;
            this.sourceAmount = sourceAmount;
            this.sourceCurrency = sourceCurrency;
            this.targetAccountId = targetAccountId;
            this.targetAmount = targetAmount;
            this.targetCurrency = targetCurrency;
            this.comment = comment;
        }
    }

    public static class Account {
        private final long accountId;
        private final Currency currency;
        private final long lowerLimit;

        private long balance;

        public Account(long accountId, Currency currency, long lowerLimit, long balance) {
            if (balance < lowerLimit) {
                throw new IllegalArgumentException("Balance is Less Than Lower Limit : " + balance + " " + lowerLimit);
            }
            this.accountId = accountId;
            this.currency = currency;
            this.lowerLimit = lowerLimit;
            this.balance = balance;
        }

        public long accountId() {
            return accountId;
        }

        private boolean ensureAvailableToWithdraw(long amount) {
            assert Thread.holdsLock(this);
            return balance - amount >= lowerLimit;
        }

        private void enroll(long amount) {
            assert Thread.holdsLock(this);
            long newBalance = this.balance + amount;
            assert newBalance >= lowerLimit;
            this.balance = newBalance;
        }

        public synchronized long getBalance() {
            return balance;
        }

        @Override
        public String toString() {
            return "Account{" +
                    "accountId=" + accountId +
                    ", lowerLimit=" + lowerLimit +
                    ", balance=" + balance +
                    '}';
        }
    }
}
