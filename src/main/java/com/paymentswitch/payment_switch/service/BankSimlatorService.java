package com.paymentswitch.payment_switch.service;
//package com.paymentswitch.service;

import com.paymentswitch.payment_switch.model.ResponseCode;
import com.paymentswitch.payment_switch.model.TransactionRequest;
import com.paymentswitch.payment_switch.model.TransactionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Random;

@Slf4j
@Service
public class BankSimlatorService {

    private final Random random = new Random();

    // Sealed interface for bank operation results
    public sealed interface BankOperationResult {
        record Approved(
                String authCode,
                String additionalData
        ) implements BankOperationResult {}

        record Declined(
                ResponseCode reason,
                String message
        ) implements BankOperationResult {}

        record Error(
                String errorCode,
                String errorMessage
        ) implements BankOperationResult {}
    }

    public TransactionResponse processBalanceInquiry(TransactionRequest request) {
        log.info("Bank: Processing Balance Inquiry for PAN: {}", request.maskedPAN());

        var result = new BankOperationResult.Approved(
                generateAuthCode(),
                "AVAIL:25000.00|LEDGER:25500.00"
        );

        return buildResponseFromResult(request, result);
    }

    public TransactionResponse processWithdrawal(TransactionRequest request) {
        log.info("Bank: Processing Withdrawal for amount: {}", request.transactionAmount());

        BankOperationResult result = validateWithdrawal(request.transactionAmount());

        return buildResponseFromResult(request, result);
    }

    public TransactionResponse processPurchase(TransactionRequest request) {
        log.info("Bank: Processing Purchase for amount: {}", request.transactionAmount());

        // Random approval for demo (90% success rate)
        BankOperationResult result = random.nextInt(10) < 9
                ? new BankOperationResult.Approved(generateAuthCode(), null)
                : new BankOperationResult.Declined(
                ResponseCode.INSUFFICIENT_FUNDS,
                "Insufficient funds available"
        );

        return buildResponseFromResult(request, result);
    }

    public TransactionResponse processTransfer(TransactionRequest request) {
        log.info("Bank: Processing Transfer for amount: {}", request.transactionAmount());

        var result = new BankOperationResult.Approved(
                generateAuthCode(),
                "TRANSFER:SUCCESS"
        );

        return buildResponseFromResult(request, result);
    }

    public TransactionResponse processMiniStatement(TransactionRequest request) {
        log.info("Bank: Processing Mini Statement");

        var result = new BankOperationResult.Approved(
                generateAuthCode(),
                "STMT:5 transactions available"
        );

        return buildResponseFromResult(request, result);
    }

    public TransactionResponse processTransaction(TransactionRequest request, String type) {
        log.warn("Bank: Processing unknown transaction type: {}", type);

        var result = new BankOperationResult.Declined(
                ResponseCode.INVALID_TRANSACTION,
                "Unknown transaction type"
        );

        return buildResponseFromResult(request, result);
    }

    // Validation logic with sealed result
    private BankOperationResult validateWithdrawal(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.valueOf(50000)) > 0) {
            return new BankOperationResult.Declined(
                    ResponseCode.EXCEEDS_WITHDRAWAL_LIMIT,
                    "Amount exceeds daily withdrawal limit"
            );
        }

        if (amount.compareTo(BigDecimal.valueOf(25000)) > 0) {
            return new BankOperationResult.Declined(
                    ResponseCode.INSUFFICIENT_FUNDS,
                    "Insufficient funds in account"
            );
        }

        return new BankOperationResult.Approved(generateAuthCode(), null);
    }

    // Java 21 pattern matching to build response from result
    private TransactionResponse buildResponseFromResult(
            TransactionRequest request,
            BankOperationResult result) {

        String responseMTI = convertToResponseMTI(request.messageType());

        return switch (result) {
            case BankOperationResult.Approved(var authCode, var additionalData) ->
                    new TransactionResponse(
                            responseMTI,
                            request.primaryAccountNumber(),
                            request.processingCode(),
                            request.transactionAmount(),
                            request.transmissionDateTime(),
                            request.stan(),
                            ResponseCode.APPROVED.getCode(),
                            authCode,
                            request.acquiringInstitutionCode(),
                            additionalData
                    );

            case BankOperationResult.Declined(var reason, var message) -> {
                log.warn("Transaction declined: {} - {}", reason.getCode(), message);
                yield new TransactionResponse(
                        responseMTI,
                        request.primaryAccountNumber(),
                        request.processingCode(),
                        request.transactionAmount(),
                        request.transmissionDateTime(),
                        request.stan(),
                        reason.getCode(),
                        null,
                        request.acquiringInstitutionCode(),
                        message
                );
            }

            case BankOperationResult.Error(var errorCode, var errorMessage) -> {
                log.error("Transaction error: {} - {}", errorCode, errorMessage);
                yield new TransactionResponse(
                        responseMTI,
                        request.primaryAccountNumber(),
                        request.processingCode(),
                        request.transactionAmount(),
                        request.transmissionDateTime(),
                        request.stan(),
                        ResponseCode.SYSTEM_MALFUNCTION.getCode(),
                        null,
                        request.acquiringInstitutionCode(),
                        errorMessage
                );
            }
        };
    }

    private String convertToResponseMTI(String requestMTI) {
        return "0" + (Integer.parseInt(requestMTI.substring(1)) + 10);
    }

    private String generateAuthCode() {
        return String.format("%06d", random.nextInt(1000000));
    }
}