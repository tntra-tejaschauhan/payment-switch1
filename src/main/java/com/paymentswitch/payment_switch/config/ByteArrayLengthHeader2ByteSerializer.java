package com.paymentswitch.payment_switch.config;
//package com.paymentswitch.payment_switch.config;

import org.springframework.integration.ip.tcp.serializer.AbstractByteArraySerializer;
import org.springframework.integration.ip.tcp.serializer.SoftEndOfStreamException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ByteArrayLengthHeader2ByteSerializer extends AbstractByteArraySerializer {

    @Override
    public void serialize(byte[] bytes, OutputStream out) throws IOException {
        byte[] lengthHeader = new byte[2];
        lengthHeader[0] = (byte) ((bytes.length >> 8) & 0xFF);
        lengthHeader[1] = (byte) (bytes.length & 0xFF);
        out.write(lengthHeader);
        out.write(bytes);
        out.flush();
    }

    @Override
    public byte[] deserialize(InputStream in) throws IOException {
        byte[] lengthHeader = new byte[2];
        int bytesRead = in.read(lengthHeader);

        if (bytesRead != 2) {
            throw new SoftEndOfStreamException("Failed to read 2-byte length header");
        }

        int messageLength = ((lengthHeader[0] & 0xFF) << 8) | (lengthHeader[1] & 0xFF);

        if (messageLength <= 0 || messageLength > 2048) {
            throw new IOException("Invalid message length: " + messageLength);
        }

        byte[] message = new byte[messageLength];
        int totalRead = 0;
        while (totalRead < messageLength) {
            int read = in.read(message, totalRead, messageLength - totalRead);
            if (read == -1) {
                throw new SoftEndOfStreamException("Connection closed while reading message");
            }
            totalRead += read;
        }

        return message;
    }
}
