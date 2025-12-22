package com.paymentswitch.payment_switch.router;

import com.paymentswitch.payment_switch.model.ProcessingCode;
import com.paymentswitch.payment_switch.model.TransactionRequest;
import com.paymentswitch.payment_switch.model.TransactionResponse;
import com.paymentswitch.payment_switch.service.BankSimlatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionRouter {

    private final BankSimlatorService bankSimlatorService;

    public TransactionResponse route(TransactionRequest request){
        ProcessingCode pc= ProcessingCode.parse(request.processingCode(),request.transactionAmount());
        log.info("Routing transaction - type {}, STAN: {}, Amount: {}",pc.description(),request.stan(),request.transactionAmount());

        return switch (pc){
            case ProcessingCode.BalanceInquiry(var code) ->{
                log.info("Processing Balance Inquiry with code: {}", code);
                yield bankSimlatorService.processBalanceInquiry(request);
            }
            case ProcessingCode.Withdrawal(var code,var amount) ->{
                log.info("Processing withdrawal - code: {}, Amount: {}", code,amount);
                yield  bankSimlatorService.processWithdrawal(request);
            }
            case ProcessingCode.Purchase(var code, var amount) ->{
                log.info("Processing Purchase - code: {}, Amont {}", code ,amount);
                yield bankSimlatorService.processPurchase(request);
            }
            case ProcessingCode.Transfer(var code, var from, var to) -> {
                log.info("Processing transfer - code: {}", code );
                yield bankSimlatorService.processTransfer(request);
            }
            case ProcessingCode.MiniStatement(var code) ->{
                log.info("procesing for mini statement - code: {}", code);
                yield  bankSimlatorService.processMiniStatement(request);
            }
            case ProcessingCode.Unknown(var code) ->{
                log.info("Unknown Processing Code: {}", code);
                yield bankSimlatorService.processTransaction(request,"UNKNOWN");
            }

        };
    }

    public TransactionResponse routeWithValidation(TransactionRequest request){
        ProcessingCode pc = ProcessingCode.parse(request.processingCode(),request.transactionAmount());
        return switch(pc){
            case ProcessingCode.Withdrawal(var code,var amount) when amount.compareTo(BigDecimal.valueOf(50000))>0 ->{
            log.warn("wthdrawal amount exceeds limit: {}", amount);
            yield TransactionResponse.error(convertToResponseMTI(request.messageType()),request.stan(),"61");
            }
            case ProcessingCode.Withdrawal w-> bankSimlatorService.processWithdrawal(request);

            case ProcessingCode.Purchase(var code,var amount) when amount.compareTo(BigDecimal.ZERO)<=0 ->{
                log.warn("Invalid purchase amount: {}", amount);
                yield TransactionResponse.error(convertToResponseMTI(request.messageType()),request.stan(),"13");
            }
            case ProcessingCode.Purchase p -> bankSimlatorService.processPurchase(request);
            case ProcessingCode.BalanceInquiry bi -> bankSimlatorService.processBalanceInquiry(request);
            case ProcessingCode.Transfer t -> bankSimlatorService.processTransfer(request);
            case ProcessingCode.MiniStatement ms-> bankSimlatorService.processMiniStatement(request);
            case ProcessingCode.Unknown u -> {
                log.error("UNKNOWN transaction type: {}", u.code());
                yield bankSimlatorService.processTransaction(request,"UNKNOWN");
            }
        };
    }

    private String convertToResponseMTI(String requestMTI){
        return "0"+ (Integer.parseInt(requestMTI.substring(1))+10);
    }

    //Additional routing logic with guards
}
