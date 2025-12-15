package com.paymentswitch.payment_switch.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNetServerConnectionFactory;
import org.springframework.integration.ip.tcp.inbound.TcpReceivingChannelAdapter;
import org.springframework.integration.ip.tcp.outbound.TcpSendingMessageHandler;
import org.springframework.integration.ip.tcp.serializer.ByteArrayCrLfSerializer;
import org.springframework.integration.ip.tcp.serializer.ByteArrayLengthHeaderSerializer;
import org.springframework.messaging.MessageChannel;

@Configuration
public class TcpServerConfig {


    @Value("${switch.tcp.port}")
    private int port;

    @Bean
    public AbstractServerConnectionFactory serverConnectionFactory() {
        TcpNetServerConnectionFactory factory = new TcpNetServerConnectionFactory(port);
        factory.setSerializer(lengthHeaderSerializer());
        factory.setDeserializer(lengthHeaderSerializer());
        factory.setSingleUse(false);
        return factory;
    }

    @Bean
    public ByteArrayLengthHeaderSerializer lengthHeaderSerializer() {
        ByteArrayLengthHeaderSerializer serializer = new ByteArrayLengthHeaderSerializer();
        serializer.setMaxMessageSize(2048);
        return serializer;
    }

    @Bean
    public TcpReceivingChannelAdapter tcpInboundAdapter() {
        TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
        adapter.setConnectionFactory(serverConnectionFactory());
        adapter.setOutputChannel(inboundChannel());
        return adapter;
    }

    @Bean
    public MessageChannel inboundChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel outboundChannel() {
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "outboundChannel")
    public TcpSendingMessageHandler tcpOutboundAdapter() {
        TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
        handler.setConnectionFactory(serverConnectionFactory());
        return handler;
    }

}
