package com.paymentswitch.payment_switch.config;

import org.jpos.iso.packager.GenericPackager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JposConfig {

    @Bean
    public GenericPackager isoPackager() throws Exception {
        return new GenericPackager("src/main/resources/packager/iso87ascii.xml");
    }

}
