package com.paymentswitch.payment_switch.transformer;

import com.paymentswitch.payment_switch.model.TransactionRequest;
import com.paymentswitch.payment_switch.model.TransactionResponse;
import lombok.extern.slf4j.Slf4j;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class IsoMessageTransformer {
    private static final DateTimeFormatter ISO_DATE_FORMAT=DateTimeFormatter.ofPattern("MMddHHmmss");

    @Autowired
    private GenericPackager packager;

    public TransactionRequest toTransactionRequest(ISOMsg msg) throws ISOException{
        log.debug("Transforming ISO message to TransactionRequest");
        return TransactionRequest.builder()
                .messageType(msg.getMTI()).primaryAccountNumber(msg.getString(2))
                .processingCode(msg.getString(3)).transactionAmount(parseAmount(msg.getString(4)))
                .transmissionDateTime(parseDateTime(msg.getString(7)))
                .stan(msg.getString(11))
                .acquiringInstitutionCode(msg.getString(32))
                .cardAcceptorTerminalId(msg.getString(41))
                .cardAcceptorNameLocation(msg.getString(43))
                .additionalData(msg.hasField(48) ? msg.getString(48) :null).build();
    }

    public ISOMsg toIsoMessage(TransactionResponse response) throws ISOException{
        log.debug("Trasforming transactionResponse to ISO message");
        ISOMsg msg = new ISOMsg();
        msg.setPackager(packager);
        msg.setMTI(response.messageType());
        msg.set(2,response.primaryAccountNumber());
        msg.set(3,response.processingCode());
        msg.set(4,formatAmount(response.transactionAmount()));
        msg.set(7,formateDateTime(response.transmissionDateTime()));
        msg.set(11,response.stan());
        msg.set(39,response.responseCode());

        if(response.authorizationCode() !=null){
            msg.set(38,response.authorizationCode());
        }
        if(response.acquiringInstitutionCode()!=null){
            msg.set(32,response.acquiringInstitutionCode());
        }
        if(response.additionalResponseData()!=null){
            msg.set(48,response.additionalResponseData());
        }
        return msg;
    }



    private BigDecimal parseAmount(String amount){
        if(amount==null || amount.isEmpty()){
            return BigDecimal.ZERO;
        }
        return  new BigDecimal(amount).divide(BigDecimal.valueOf(100));
    }

    private String formatAmount(BigDecimal amount){
        if(amount==null){
            return "000000000000";
        }
        long minorUnits = amount.multiply(BigDecimal.valueOf(100)).longValue();
        return String.format("%012d",minorUnits);
    }
    private LocalDateTime parseDateTime(String dateTime) {
        if (dateTime == null || dateTime.isEmpty()) {
            return LocalDateTime.now();
        }

        try {
            // ISO 8583 field 7: MMDDhhmmss (no year)
            // Example: "1227212919" -> December 27, 21:29:19
            int month = Integer.parseInt(dateTime.substring(0, 2));
            int day = Integer.parseInt(dateTime.substring(2, 4));
            int hour = Integer.parseInt(dateTime.substring(4, 6));
            int minute = Integer.parseInt(dateTime.substring(6, 8));
            int second = Integer.parseInt(dateTime.substring(8, 10));

            // Use current year since ISO 8583 doesn't include it
            int year = LocalDate.now().getYear();

            return LocalDateTime.of(year, month, day, hour, minute, second);
        } catch (Exception e) {
            log.error("Error parsing date/time: {}", dateTime, e);
            return LocalDateTime.now();
        }
    }
    private String formateDateTime(LocalDateTime datetime){
        if(datetime ==null){
            datetime=LocalDateTime.now();
        }
        return datetime.format(ISO_DATE_FORMAT);
    }
}
