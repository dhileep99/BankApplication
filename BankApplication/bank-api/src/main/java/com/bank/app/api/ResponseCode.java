package com.bank.app.api;

public enum ResponseCode {

    SUCCESS(1, "Success"),
    DUPLICATE_SUCCESS(2, "Duplicate success"),
    NOT_FOUND(100, "Payment not found"),
    BAD_SOURCE_ACCOUNT(101, "Source account does not exist"),
    BAD_TARGET_ACCOUNT(102, "Target account does not exist"),
    LOCKED_SOURCE_ACCOUNT(103, "Source account is locked"),
    LOCKED_TARGET_ACCOUNT(104, "Target account is locked"),
    NO_MONEY(105, "Not enough money on source account");

    private final int code;
    private final String message;

    ResponseCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }

    @Override
    public String toString() {
        return "ResponseCode" +
                "code=" + code +
                ", message='" + message + '\'' +
                '}';
    }
}
