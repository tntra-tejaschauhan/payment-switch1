package com.paymentswitch.payment_switch.client;

import org.bouncycastle.util.Bytes;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.iso.packager.GenericPackager;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TreeMap;

public class AtmSimulatorClient {
    private static final String HOST= "localhost";
    private static final int PORT= 8583;
    private static final DateTimeFormatter ISO_DATE_FORMAT=DateTimeFormatter.ofPattern("MMddHHmmss");
    private GenericPackager packager;
    public AtmSimulatorClient() throws Exception {
        try (InputStream is = new ClassPathResource("packager/iso87ascii.xml").getInputStream()) {
            packager = new GenericPackager(is);
        }
    }

    public static void main(String[] args){
        try{
            AtmSimulatorClient client =new AtmSimulatorClient();
            System.out.println("=== ATM Simulator - Payment Switch Test Client ===\n");
            System.out.println("Test 1: Balance Inquiry");
            client.sendBalanceInquiry();
            Thread.sleep(1000);

            System.out.println("\nTest 2: Withdrawal");
            client.sendWithDrawal();
            Thread.sleep(1000);

            System.out.println("\n Test 3: Purchase");
            client.sendPurchase();
            Thread.sleep(1000);

            System.out.println("\n=== All tests completed ===");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendBalanceInquiry() throws Exception{
        ISOMsg msg= createBaseMessage("0200");
        msg.set(3,"310000");
        msg.set(4,"000000000000");
        sendAndReceive(msg);
    }

    public void sendWithDrawal() throws Exception{
        ISOMsg msg= createBaseMessage("0200");
        msg.set(3,"010000");
        msg.set(4,"000000010000");
        sendAndReceive(msg);
    }
    public void sendPurchase() throws Exception{
        ISOMsg msg = createBaseMessage("0200");
        msg.set(3,"000000");
        msg.set(4,"000000025000");
        sendAndReceive(msg);
    }

    private ISOMsg createBaseMessage(String mti)throws ISOException {
        ISOMsg msg = new ISOMsg();
        msg.setPackager(packager);
        msg.setMTI(mti);

        msg.set(2,"4111111111111111");
        msg.set(7, LocalDateTime.now().format(ISO_DATE_FORMAT));
        msg.set(11, String.format("%06d",System.currentTimeMillis() % 1000000));
        msg.set(32,"123456");
        msg.set(41, "ATM00001");                                         // ← ADD THIS LINE!
        msg.set(43,"Test atm location 123 main st");

        return msg;
    }

    private void sendAndReceive(ISOMsg request) throws Exception{
        try(Socket socket = new Socket(HOST,PORT)){
            OutputStream out= socket.getOutputStream();
            InputStream in = socket.getInputStream();
            socket.setSoTimeout(5000); // 5 second timeout

            ByteArrayOutputStream baos= new ByteArrayOutputStream();
            System.out.println(request.getMTI());
            request.pack(baos);
            byte[] requestBytes = baos.toByteArray();
            System.out.println("the lenght of request is "+requestBytes.length);
            byte[] lengthHeader = new byte[2];
            lengthHeader[0]=(byte) ((requestBytes.length>>8) & 0xFF);
            lengthHeader[1]=(byte) (requestBytes.length & 0xFF);

            byte[] packet = new byte[lengthHeader.length + requestBytes.length];
            System.arraycopy(lengthHeader, 0, packet, 0, lengthHeader.length);
            System.arraycopy(requestBytes, 0, packet, lengthHeader.length, requestBytes.length);
            out.write(packet);


            out.flush();

            System.out.println("Request sent:");
            System.out.println("  MTI: " + request.getMTI());
            System.out.println("  PAN: " + maskPAN(request.getString(2)));
            System.out.println("  Processing Code: " + request.getString(3));
            System.out.println("  Amount: " + request.getString(4));
            System.out.println("Time:" +request.getString(7));
            System.out.println("  STAN: " + request.getString(11));
            System.out.println("  Terminal ID: " + request.getString(41)); // Show terminal ID
            System.out.println("  Request bytes: " + ISOUtil.hexString(requestBytes));
            System.out.println("  Request length: " + requestBytes.length + " bytes");

            // Read response length header
            byte[] respLengthHeader = new byte[2];
            int bytesRead = in.read(respLengthHeader);

            if (bytesRead != 2) {
                throw new Exception("Failed to read response length header. Bytes read: " + bytesRead);
            }

            int responseLength = ((respLengthHeader[0] & 0xFF) << 8) | (respLengthHeader[1] & 0xFF);
            System.out.println("\nResponse length from header: " + responseLength + " bytes");

            if (responseLength == 0) {
                throw new Exception("Server sent empty response (length = 0). Check server logs!");
            }

            if (responseLength > 2048) {
                throw new Exception("Invalid response length: " + responseLength + ". Possible corruption.");
            }

            // Read response payload
            byte[] responseBytes = new byte[responseLength];
            int totalRead = 0;
            while (totalRead < responseLength) {
                int read = in.read(responseBytes, totalRead, responseLength - totalRead);
                if (read == -1) {
                    throw new Exception(STR."Connection closed while reading response. Expected \{responseLength} bytes, got \{totalRead}");
                }
                totalRead += read;
            }

            System.out.println("Response bytes read: " + totalRead);
            System.out.println("Response hex: " + ISOUtil.hexString(responseBytes));

            // Unpack response
            ISOMsg response = new ISOMsg();
            response.setPackager(packager);
            response.unpack(responseBytes);

            System.out.println("\n Response received:");
            System.out.println("MTI :"+ response.getMTI());
            System.out.println("Time:" +request.getString(7));
            System.out.println("STAN :"+ response.getString(11));
            System.out.println("Response code : "+ response.getString(39)+ "(" + getResponseDescription(response.getString(39))+")");
            if(response.hasField(38)){
                System.out.println("Auth code:" + response.getString(38));
            }
            if(response.hasField(48)){
                System.out.println("Additional Data: "+response.getString(48));
            }
            System.out.println("Response bytes: "+ ISOUtil.hexString(responseBytes));

        } catch (Exception e) {
            System.err.println("Error communicating with switch: "+e.getMessage());
            throw  e;
        }
    }

    private String maskPAN(String pan){
        if(pan==null || pan.length() <10) return "****";
        return pan.substring(0,6)+"*******"+pan.substring(pan.length()-4);
    }
    private String getResponseDescription(String code){
        return switch (code){
            case "00" -> "Approved";
            case "12" -> "Invalid Transaction";
            case "13" -> "Invalid amount";
            case "14" -> "Invalid card number";
            case "30" -> "Format error";
            case "51" -> "Insufficient funds";
            case "54" -> "Expired card";
            case "55" -> "Incorrect PIN";
            case "57" -> "Transaction not permitted";
            case "61" -> "Exceeds withdrawal limit";
            case "96" -> "System malfunction";
            default ->  "UNKNOWN";
        };
    }
}
