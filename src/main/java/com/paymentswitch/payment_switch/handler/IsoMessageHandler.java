package com.paymentswitch.payment_switch.handler;
//package com.paymentswitch.handler;

import com.paymentswitch.payment_switch.model.TransactionRequest;
import com.paymentswitch.payment_switch.model.TransactionResponse;
import com.paymentswitch.payment_switch.model.ValidationResult;
import com.paymentswitch.payment_switch.router.TransactionRouter;
import com.paymentswitch.payment_switch.transformer.IsoMessageTransformer;
import com.paymentswitch.payment_switch.validator.IsoMessageValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.iso.packager.GenericPackager;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class IsoMessageHandler {

    private final GenericPackager packager;
    private final IsoMessageValidator validator;
    private final IsoMessageTransformer transformer;
    private final TransactionRouter router;

    // Sealed interface for processing results
    sealed interface ProcessingResult {
        record Success(TransactionResponse response) implements ProcessingResult {}
        record ValidationError(ValidationResult.Failure failure, ISOMsg originalMsg) implements ProcessingResult {}
        record SystemError(String error) implements ProcessingResult {}
    }

    @ServiceActivator(inputChannel = "inboundChannel", outputChannel = "outboundChannel")
    public Message<byte[]> handleMessage(Message<byte[]> message) {
        long startTime = System.currentTimeMillis();

        byte[] payload = message.getPayload();
        log.info("Received message: {} bytes", payload.length);
        log.debug("Raw message: {}", ISOUtil.hexString(payload));

        // Process the message and get result
        ProcessingResult result = processIsoMessage(payload);

        // Pattern match on result to generate response
        byte[] responseBytes = switch (result) {
            case ProcessingResult.Success(var response) -> {
                log.info("Transaction processed - STAN: {}, Response Code: {}",
                        response.stan(), response.responseCode());
                System.out.println("transmission time from handler"+response.transmissionDateTime());
                yield packResponse(response);
            }
            case ProcessingResult.ValidationError(var failure, var originalMsg) -> {
                log.warn("Validation failed: {} (field: {})",
                        failure.message(), failure.field());
                yield packErrorResponse(originalMsg, failure);
            }
            case ProcessingResult.SystemError(var error) -> {
                log.error("System error: {}", error);
                yield packSystemErrorResponse();
            }
        };

        log.info("Response sent - {} bytes, Processing time: {} ms",
                responseBytes.length,
                System.currentTimeMillis() - startTime);
        log.debug("Response ISO: {}", ISOUtil.hexString(responseBytes));

        MessageHeaders headers = message.getHeaders();
        return MessageBuilder.withPayload(responseBytes)
                .copyHeaders(headers)
                .build();
    }

    private ProcessingResult processIsoMessage(byte[] payload) {
        try {
            // Parse incoming ISO message
            ISOMsg isoRequest = new ISOMsg();
            isoRequest.setPackager(packager);
            isoRequest.unpack(payload);

            log.info("Parsed ISO Message - MTI: {}, STAN: {}",
                    isoRequest.getMTI(),
                    isoRequest.hasField(11) ? isoRequest.getString(11) : "N/A");

            // Validate message using pattern matching
            ValidationResult validationResult = validator.validate(isoRequest);
//            System.out.println(validationResult);
            return switch (validationResult) {
                case ValidationResult.Success success -> {
                    // Transform to domain model
                    TransactionRequest request = transformer.toTransactionRequest(isoRequest);
                    System.out.println("transmissoon time is "+request.transmissionDateTime());
                    // Route and process
                    TransactionResponse response = router.route(request);
                    System.out.println("transmission time"+response.transmissionDateTime());
                    yield new ProcessingResult.Success(response);
                }
                case ValidationResult.Failure failure ->
                        new ProcessingResult.ValidationError(failure, isoRequest);
            };

        } catch (ISOException e) {
            log.error("ISO parsing error", e);
            return new ProcessingResult.SystemError("ISO parsing error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error processing message", e);
            return new ProcessingResult.SystemError("Unexpected error: " + e.getMessage());
        }
    }

    private byte[] packResponse(TransactionResponse response) {
        try {
            ISOMsg isoResponse = transformer.toIsoMessage(response);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            isoResponse.pack(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error packing response", e);
            return packSystemErrorResponse();
        }
    }

    private byte[] packErrorResponse(ISOMsg request, ValidationResult.Failure failure) {
        try {
            String mti = request.getMTI();
            String responseMTI = "0" + (Integer.parseInt(mti.substring(1)) + 10);

            TransactionResponse response = new TransactionResponse(
                    responseMTI,
                    request.hasField(2) ? request.getString(2) : "",
                    request.hasField(3) ? request.getString(3) : "",
                    null,
                    null,
                    request.hasField(11) ? request.getString(11) : "000000",
                    failure.responseCode().getCode(),
                    null,
                    null,
                    failure.message()
            );

            return packResponse(response);
        } catch (Exception e) {
            log.error("Error creating error response", e);
            return packSystemErrorResponse();
        }
    }

    private byte[] packSystemErrorResponse() {
        try {
            ISOMsg msg = new ISOMsg();
            msg.setPackager(packager);
            msg.setMTI("0210");
            msg.set(39, "96"); // System malfunction

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            msg.pack(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to create system error response", e);
            return new byte[0];
        }
    }
}