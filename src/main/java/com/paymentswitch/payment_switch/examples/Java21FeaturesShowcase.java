package com.paymentswitch.payment_switch.examples;

//package com.paymentswitch.examples;

import com.paymentswitch.payment_switch.model.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Showcase of Java 21 features used in the Payment Switch
 * This class demonstrates the modern Java capabilities
 */
public class Java21FeaturesShowcase {

    // ============================================
    // 1. RECORD PATTERN MATCHING
    // ============================================

    public void demonstrateRecordPatternMatching() {
        TransactionRequest request = new TransactionRequest(
                "0200",
                "4111111111111111",
                "310000",
                BigDecimal.valueOf(100),
                LocalDateTime.now(),
                "123456",
                "000001",
                "ATM001",
                "Test Location",
                null
        );

        // Pattern matching with records
        String info = switch (request) {
            case TransactionRequest r when r.transactionAmount().compareTo(BigDecimal.ZERO) == 0 ->
                    "Zero amount transaction for PAN: " + r.maskedPAN();
            case TransactionRequest r when r.transactionAmount().compareTo(BigDecimal.valueOf(1000)) > 0 ->
                    "High value transaction: " + r.transactionAmount();
            case TransactionRequest r ->
                    "Standard transaction: " + r.stan();
        };

        System.out.println(info);
    }

    // ============================================
    // 2. SEALED INTERFACES WITH PATTERN MATCHING
    // ============================================

    public void demonstrateSealedInterfacePatternMatching() {
        ProcessingCode pc = ProcessingCode.parse("310000", BigDecimal.ZERO);

        // Exhaustive pattern matching with sealed interfaces
        String description = switch (pc) {
            case ProcessingCode.BalanceInquiry(var code) ->
                    "Balance check with code: " + code;
            case ProcessingCode.Withdrawal(var code, var amount) ->
                    String.format("Withdrawal of %.2f with code: %s", amount, code);
            case ProcessingCode.Purchase(var code, var amount) ->
                    String.format("Purchase of %.2f with code: %s", amount, code);
            case ProcessingCode.Transfer(var code, var from, var to) ->
                    "Transfer operation: " + code;
            case ProcessingCode.MiniStatement(var code) ->
                    "Statement request: " + code;
            case ProcessingCode.Unknown(var code) ->
                    "Unknown transaction: " + code;
        };

        System.out.println(description);
    }

    // ============================================
    // 3. GUARDED PATTERNS (WHEN CLAUSE)
    // ============================================

    public String categorizeTransaction(TransactionRequest request) {
        ProcessingCode pc = ProcessingCode.parse(
                request.processingCode(),
                request.transactionAmount()
        );

        // Pattern matching with guards
        return switch (pc) {
            case ProcessingCode.Withdrawal(var code, var amount)
                    when amount.compareTo(BigDecimal.valueOf(10000)) > 0 ->
                    "Large withdrawal";

            case ProcessingCode.Withdrawal(var code, var amount)
                    when amount.compareTo(BigDecimal.valueOf(1000)) > 0 ->
                    "Medium withdrawal";

            case ProcessingCode.Withdrawal w ->
                    "Small withdrawal";

            case ProcessingCode.Purchase(var code, var amount)
                    when amount.compareTo(BigDecimal.valueOf(5000)) > 0 ->
                    "High value purchase";

            case ProcessingCode.Purchase p ->
                    "Regular purchase";

            default -> "Other transaction";
        };
    }

    // ============================================
    // 4. NESTED PATTERN MATCHING
    // ============================================

    public void demonstrateNestedPatternMatching() {
        ValidationResult result = ValidationResult.success();

        String message = switch (result) {
            case ValidationResult.Success success ->
                    "Validation passed";

            case ValidationResult.Failure(var code, var msg, var field)
                    when field != null ->
                    String.format("Field '%s' failed: %s (code: %s)",
                            field, msg, code.getCode());

//            case ValidationResult.Failure(var code, var msg,null) ->
//                    String.format("Validation failed: %s (code: %s)",
//                            msg, code.getCode());
            default -> throw new IllegalStateException("Unexpected value: " + result);
        };

        System.out.println(message);
    }

    // ============================================
    // 5. RECORD DECONSTRUCTION IN ENHANCED FOR
    // ============================================

    public void demonstrateRecordDeconstruction() {
        // Creating a list of results
        var results = List.of(
                new ValidationResult.Failure(
                        ResponseCode.INVALID_CARD,
                        "Invalid PAN",
                        "PAN"
                ),
                new ValidationResult.Failure(
                        ResponseCode.INVALID_AMOUNT,
                        "Invalid amount",
                        "Amount"
                )
        );

        // Pattern matching in loops
        for (ValidationResult result : results) {
            switch (result) {
                case ValidationResult.Failure(var code, var msg, var field) ->
                        System.out.printf("Error in %s: %s%n", field, msg);
                case ValidationResult.Success s ->
                        System.out.println("Success");
            }
        }
    }

    // ============================================
    // 6. TYPE PATTERNS WITH INSTANCEOF
    // ============================================

    public void demonstrateTypePatterns(Object obj) {
        // Modern instanceof with pattern matching
        if (obj instanceof TransactionRequest request
                && request.transactionAmount().compareTo(BigDecimal.ZERO) > 0) {
            System.out.println("Valid transaction: " + request.stan());
        }

        // With negation
        if (!(obj instanceof TransactionResponse response
                && response.isApproved())) {
            System.out.println("Transaction not approved or not a response");
        }
    }

    // ============================================
    // 7. SWITCH EXPRESSIONS WITH YIELD
    // ============================================

    public TransactionResponse handleTransactionWithYield(TransactionRequest request) {
        return switch (ProcessingCode.parse(request.processingCode(), request.transactionAmount())) {
            case ProcessingCode.BalanceInquiry bi -> {
                // Complex logic with yield
                System.out.println("Processing balance inquiry");
                yield createSuccessResponse(request, "00");
            }
            case ProcessingCode.Withdrawal w -> {
                System.out.println("Processing withdrawal");
                yield createSuccessResponse(request, "00");
            }
            default -> {
                System.out.println("Unknown transaction type");
                yield createErrorResponse(request, "12");
            }
        };
    }

    // ============================================
    // 8. COMPACT RECORD CONSTRUCTORS
    // ============================================

    public void demonstrateCompactConstructors() {
        // Records can validate in compact constructors
        try {
            var request = new TransactionRequest(
                    null,  // Will throw IllegalArgumentException
                    "4111111111111111",
                    "310000",
                    BigDecimal.valueOf(100),
                    LocalDateTime.now(),
                    "123456",
                    null, null, null, null
            );
        } catch (IllegalArgumentException e) {
            System.out.println("Validation in compact constructor: " + e.getMessage());
        }
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    private TransactionResponse createSuccessResponse(TransactionRequest request, String code) {
        return new TransactionResponse(
                "0210",
                request.primaryAccountNumber(),
                request.processingCode(),
                request.transactionAmount(),
                request.transmissionDateTime(),
                request.stan(),
                code,
                "123456",
                request.acquiringInstitutionCode(),
                null
        );
    }

    private TransactionResponse createErrorResponse(TransactionRequest request, String code) {
        return TransactionResponse.error("0210", request.stan(), code);
    }

    // ============================================
    // MAIN METHOD FOR DEMONSTRATION
    // ============================================

    public static void main(String[] args) {
        System.out.println("=== Java 21 Features Showcase ===\n");

        Java21FeaturesShowcase showcase = new Java21FeaturesShowcase();

        System.out.println("1. Record Pattern Matching:");
        showcase.demonstrateRecordPatternMatching();

        System.out.println("\n2. Sealed Interface Pattern Matching:");
        showcase.demonstrateSealedInterfacePatternMatching();

        System.out.println("\n3. Guarded Patterns:");
        TransactionRequest testRequest = new TransactionRequest(
                "0200", "4111111111111111", "010000",
                BigDecimal.valueOf(15000), LocalDateTime.now(),
                "123456", null, null, null, null
        );
        System.out.println(showcase.categorizeTransaction(testRequest));

        System.out.println("\n4. Nested Pattern Matching:");
        showcase.demonstrateNestedPatternMatching();

        System.out.println("\n5. Record Deconstruction:");
        showcase.demonstrateRecordDeconstruction();

        System.out.println("\n6. Type Patterns:");
        showcase.demonstrateTypePatterns(testRequest);

        System.out.println("\n7. Compact Constructors:");
        showcase.demonstrateCompactConstructors();

        System.out.println("\n=== End of Showcase ===");
    }
}