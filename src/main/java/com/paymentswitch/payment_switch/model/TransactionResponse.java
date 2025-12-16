package com.paymentswitch.payment_switch.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        String messageType,
        String primaryAccountNumber,
        String processingCode,
        BigDecimal transactionAmount,
        LocalDateTime transmissionDateTime,
        String stan,
        String responseCode,
        String authorizationCode,
        String acquiringInstitutionCode,
        String additionalResponseData
) {
    // Compact constructor for validation
    public TransactionResponse {
        if (messageType == null || messageType.isBlank()) {
            throw new IllegalArgumentException("Message type cannot be null or blank");
        }
        if (responseCode == null || responseCode.isBlank()) {
            throw new IllegalArgumentException("Response code cannot be null or blank");
        }
    }

    // Check if transaction was approved
    public boolean isApproved() {
        return "00".equals(responseCode);
    }

    // Check if transaction was declined
    public boolean isDeclined() {
        return !isApproved();
    }

    // Factory method for error responses
    public static TransactionResponse error(
            String messageType,
            String stan,
            String responseCode) {
        return new TransactionResponse(
                messageType, null, null, null, null, stan,
                responseCode, null, null, null
        );
    }
}