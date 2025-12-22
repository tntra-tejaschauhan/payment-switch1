package com.paymentswitch.payment_switch.service;

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
public TransactionResponse processBalanceInquiry(TransactionRequest request){
    log.info("Bank: Processing Balance Inquiry for PAN: {}", maskPAN(request.primaryAccountNumber()));
    return buildResponse(request, ResponseCode.APPROVED, generateAuthCode())
            .additionalResponseData("AVAIL:25000.00|LEDGER:2550.00").build();
}

public TransactionResponse processWithdrawal(TransactionRequest request){
    log.info("Bank: Processing withdrawal for amount: {}",request.transactionAmount());
    if(request.transactionAmount().compareTo(BigDecimal.valueOf(50000))>0){
        return buildResponse(request,ResponseCode.EXCEEDS_WITHDRAWAL_LIMIT,null).build();
    }
    if(request.transactionAmount().compareTo(BigDecimal.valueOf(25000))>0){
        return buildResponse(request,ResponseCode.INSUFFICIENT_FUNDS,null).build();
    }
    return buildResponse(request,ResponseCode.APPROVED,generateAuthCode()).build();
}

public TransactionResponse processPurchase(TransactionRequest request){
    log.info("Bank: Processing Purchase for amount: {}", request.transactionAmount());
    ResponseCode code = random.nextInt(10)<9? ResponseCode.APPROVED : ResponseCode.INSUFFICIENT_FUNDS;

    String authCode= code == ResponseCode.APPROVED ? generateAuthCode():null;
    return buildResponse(request,code,authCode).build();

}

public TransactionResponse processTransfer(TransactionRequest request){
    log.info("Bank: processing transfer for amount: {}", request.transactionAmount());
    return buildResponse(request,ResponseCode.APPROVED,generateAuthCode()).build();
}

public TransactionResponse processMiniStatement(TransactionRequest request){
    log.info("Bank : Processing mini statement");
    return buildResponse(request, ResponseCode.APPROVED, generateAuthCode()).additionalResponseData("STMT:5 transactions available")
            .build();
}

public TransactionResponse processTransaction(TransactionRequest request, String type){
    log.warn("Bank: Processing unknown transaction type: {}", type);
    return buildResponse(request,ResponseCode.INVALID_TRANSACTION,null).build();
}

private TransactionResponse.TransactionResponseBuilder buildResponse(TransactionRequest request, ResponseCode code, String authcode){
    String responseMTI = "0"+(Integer.parseInt(request.messageType().substring(1))+10);
    return TransactionResponse.builder()
            .messageType(responseMTI).primaryAccountNumber(request.primaryAccountNumber())
            .processingCode(request.processingCode()).transactionAmount(request.transactionAmount())
            .transmissionDateTime(request.transmissionDateTime()).stan(request.stan())
            .responseCode(code.getCode())
            .authorizationCode(authcode).acquiringInstitutionCode(request.acquiringInstitutionCode());

}

private String generateAuthCode(){
    return String.format("%06d", random.nextInt(1000000));
}
private String maskPAN(String pan){
    if(pan==null || pan.length()<10) return "****";
    return pan.substring(0,6)+"******"+pan.substring(pan.length()-4);
}

}
