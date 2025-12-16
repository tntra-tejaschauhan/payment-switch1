package com.paymentswitch.payment_switch.model;

public enum ResponseCode {
    APPROVED("00", "Approved"),
    INVALID_TRANSACTION("12", "Invalid transaction"),
    INVALID_AMOUNT("13", "Invalid amount"),
    INVALID_CARD("14", "Invalid card number"),
    NO_SUCH_ISSUER("15", "No such issuer"),
    INSUFFICIENT_FUNDS("51", "Insufficient funds"),
    EXPIRED_CARD("54", "Expired card"),
    INCORRECT_PIN("55", "Incorrect PIN"),
    TRANSACTION_NOT_PERMITTED("57", "Transaction not permitted"),
    SUSPECTED_FRAUD("59", "Suspected fraud"),
    EXCEEDS_WITHDRAWAL_LIMIT("61", "Exceeds withdrawal limit"),
    RESTRICTED_CARD("62", "Restricted card"),
    SECURITY_VIOLATION("63", "Security violation"),
    EXCEEDS_FREQUENCY_LIMIT("65", "Exceeds frequency limit"),
    FORMAT_ERROR("30", "Format error"),
    SYSTEM_MALFUNCTION("96", "System malfunction");

    private final String code;
    private final String description;

    ResponseCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
