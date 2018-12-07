package com.bank.app.api.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.UUID;

@JsonSerialize
@JsonPropertyOrder({"transaction_id", "source", "target"})
public class PaymentRequest {

    @JsonProperty(value = "transaction_id", required = true)
    private String transactionId;

    @JsonProperty(required = true)
    private Account source;

    @JsonProperty(required = true)
    private Account target;

    @JsonProperty(required = false)
    private String comment;

    public PaymentRequest setTransactionId(String transactionId) {
        this.transactionId = UUID.fromString(transactionId).toString();
        return this;
    }

    public PaymentRequest setSource(Account source) {
        this.source = source;
        return this;
    }

    public PaymentRequest setTarget(Account target) {
        this.target = target;
        return this;
    }

    public PaymentRequest setComment(String comment) {
        this.comment = comment;
        return this;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public Account getSource() {
        return source;
    }

    public Account getTarget() {
        return target;
    }

    public String getComment() {
        return comment;
    }

    @Override
    public String toString() {
        return "PaymentRequest{" +
                "transactionId=" + transactionId +
                ", source=" + source +
                ", target=" + target +
                ", comment='" + comment + '\'' +
                '}';
    }

    @JsonSerialize
    public static class Account {

	@JsonProperty(required = true)
        private long id;

        @JsonProperty(required = true)
        private String currency;

        @JsonProperty(required = true)
        private String amount;

        public Account setId(long id) {
            this.id = id;
            return this;
        }

        public Account setCurrency(String currency) {
            this.currency = currency;
            return this;
        }

        public Account setAmount(String amount) {
            this.amount = amount;
            return this;
        }

        public long getId() {
            return id;
        }

        public String getCurrency() {
            return currency;
        }

        public String getAmount() {
            return amount;
        }

        @Override
        public String toString() {
            return "Account{" +
                    "id=" + id +
                    ", currency='" + currency + '\'' +
                    ", amount='" + amount + '\'' +
                    '}';
        }
    }
}
