package com.paymentswitch.payment_switch.model;

/// Sealed interface for validation results using record pattern
public sealed interface ValidationResult {

    record Success() implements ValidationResult {
        public static final Success INSTANCE = new Success();
    }

    record Failure(
            ResponseCode responseCode,
            String message,
            String field
    ) implements ValidationResult {
        public Failure {
            if (responseCode == null) {
                throw new IllegalArgumentException("Response code cannot be null");
            }
        }
    }

    // Helper methods
    default boolean isValid() {
        return this instanceof Success;
    }

    default boolean isFailure() {
        return this instanceof Failure;
    }

    static ValidationResult success() {
        return Success.INSTANCE;
    }

    static ValidationResult fail(ResponseCode code, String message, String field) {
        return new Failure(code, message, field);
    }

    static ValidationResult fail(ResponseCode code, String message) {
        return new Failure(code, message, null);
    }
}