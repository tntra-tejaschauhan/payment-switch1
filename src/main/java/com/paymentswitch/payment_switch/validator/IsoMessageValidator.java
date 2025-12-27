//package com.paymentswitch.validator;
package com.paymentswitch.payment_switch.validator;
//package com.paymentswitch.validator;

import com.paymentswitch.payment_switch.model.ResponseCode;
//import com.paymentswitch.model.ValidationResult;
import com.paymentswitch.payment_switch.model.ValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class IsoMessageValidator {

    public ValidationResult validate(ISOMsg msg) {
        try {
            // Use pattern matching for validation
            var mtiValidation = validateMTI(msg);
            if (mtiValidation instanceof ValidationResult.Failure failure) {
                log.error("MTI validation failed: {}", failure.message());
                return failure;
            }

            var panValidation = validatePAN(msg);
            if (panValidation instanceof ValidationResult.Failure failure) {
                log.error("PAN validation failed: {}", failure.message());
                return failure;
            }

            var processingCodeValidation = validateProcessingCode(msg);
            if (processingCodeValidation instanceof ValidationResult.Failure failure) {
                log.error("Processing Code validation failed: {}", failure.message());
                return failure;
            }

            var amountValidation = validateAmount(msg);
            if (amountValidation instanceof ValidationResult.Failure failure) {
                log.error("Amount validation failed: {}", failure.message());
                return failure;
            }

            var stanValidation = validateSTAN(msg);
            if (stanValidation instanceof ValidationResult.Failure failure) {
                log.error("STAN validation failed: {}", failure.message());
                return failure;
            }

            var dateTimeValidation = validateDateTime(msg);
            if (dateTimeValidation instanceof ValidationResult.Failure failure) {
                log.error("DateTime validation failed: {}", failure.message());
                return failure;
            }

            var terminalValidation = validateTerminalId(msg);
            if (terminalValidation instanceof ValidationResult.Failure failure) {
                log.error("Terminal ID validation failed: {}", failure.message());
                return failure;
            }

            log.info("Validation passed for STAN: {}", msg.getString(11));
            return ValidationResult.success();

        } catch (Exception e) {
            log.error("Validation error", e);
            return ValidationResult.fail(
                    ResponseCode.FORMAT_ERROR,
                    "Validation error: " + e.getMessage()
            );
        }
    }

    private ValidationResult validateMTI(ISOMsg msg) throws ISOException {
        String mti = msg.getMTI();
        if (mti == null || !isValidMTI(mti)) {
            return ValidationResult.fail(
                    ResponseCode.FORMAT_ERROR,
                    "Invalid MTI: " + mti,
                    "MTI"
            );
        }
        return ValidationResult.success();
    }

    private ValidationResult validatePAN(ISOMsg msg) {
        if (!msg.hasField(2)) {
            return ValidationResult.fail(
                    ResponseCode.INVALID_CARD,
                    "Missing Primary Account Number",
                    "PAN"
            );
        }

        try {
            String pan = msg.getString(2);
            if (!isValidPAN(pan)) {
                return ValidationResult.fail(
                        ResponseCode.INVALID_CARD,
                        "Invalid PAN format: " + maskPAN(pan),
                        "PAN"
                );
            }
        } catch (Exception e) {
            return ValidationResult.fail(
                    ResponseCode.INVALID_CARD,
                    "Error reading PAN",
                    "PAN"
            );
        }

        return ValidationResult.success();
    }

    private ValidationResult validateProcessingCode(ISOMsg msg) {
        if (!msg.hasField(3)) {
            return ValidationResult.fail(
                    ResponseCode.FORMAT_ERROR,
                    "Missing Processing Code",
                    "ProcessingCode"
            );
        }
        return ValidationResult.success();
    }

    private ValidationResult validateAmount(ISOMsg msg) {
        if (!msg.hasField(4)) {
            return ValidationResult.fail(
                    ResponseCode.INVALID_AMOUNT,
                    "Missing Transaction Amount",
                    "Amount"
            );
        }
        return ValidationResult.success();
    }

    private ValidationResult validateSTAN(ISOMsg msg) {
        if (!msg.hasField(11)) {
            return ValidationResult.fail(
                    ResponseCode.FORMAT_ERROR,
                    "Missing STAN",
                    "STAN"
            );
        }
        return ValidationResult.success();
    }

    private ValidationResult validateDateTime(ISOMsg msg) {
        if (!msg.hasField(7)) {
            return ValidationResult.fail(
                    ResponseCode.FORMAT_ERROR,
                    "Missing Transmission DateTime",
                    "DateTime"
            );
        }
        return ValidationResult.success();
    }

    private ValidationResult validateTerminalId(ISOMsg msg) {
        if (!msg.hasField(41)) {
            return ValidationResult.fail(
                    ResponseCode.FORMAT_ERROR,
                    "Missing Terminal ID",
                    "TerminalID"
            );
        }
        return ValidationResult.success();
    }

    private boolean isValidMTI(String mti) {
        return mti != null && mti.matches("\\d{4}") &&
                (mti.startsWith("01") || mti.startsWith("02"));
    }

    private boolean isValidPAN(String pan) {
        return pan != null && pan.matches("\\d{13,19}");
    }

    private String maskPAN(String pan) {
        if (pan == null || pan.length() < 10) return "****";
        return pan.substring(0, 6) + "******" + pan.substring(pan.length() - 4);
    }
}