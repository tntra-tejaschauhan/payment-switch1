package com.paymentswitch.payment_switch.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionRequest(
        String messageType,
        String primaryAccountNumber,
        String processingCode,
        BigDecimal transactionAmount,
        LocalDateTime transmissionDateTime,
        String stan,
        String acquiringInstitutionCode,
        String cardAcceptorTerminalId,
        String cardAcceptorNameLocation,
        String additionalData
) {
    // Compact constructor for validation
    public TransactionRequest {
        if (messageType == null || messageType.isBlank()) {
            throw new IllegalArgumentException("Message type cannot be null or blank");
        }
        if (stan == null || stan.isBlank()) {
            throw new IllegalArgumentException("STAN cannot be null or blank");
        }
    }

    // Convenience factory method
    // of means to create an instance of TransactionRequest
    public static TransactionRequest of(
            String messageType,
            String pan,
            String processingCode,
            BigDecimal amount,
            LocalDateTime dateTime,
            String stan) {
        return new TransactionRequest(
                messageType, pan, processingCode, amount, dateTime, stan,
                null, null, null, null
        );
    }

    // Helper method to get masked PAN
    public String maskedPAN() {
        if (primaryAccountNumber == null || primaryAccountNumber.length() < 10) {
            return "****";
        }
        return primaryAccountNumber.substring(0, 6) + "******"
                + primaryAccountNumber.substring(primaryAccountNumber.length() - 4);
    }
}
