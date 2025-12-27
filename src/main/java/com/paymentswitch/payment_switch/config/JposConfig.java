package com.paymentswitch.payment_switch.config;

import org.jpos.iso.ISOException;
import org.jpos.iso.packager.GenericPackager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

@Configuration
public class JposConfig {

    @Bean
    public GenericPackager isoPackager() throws ISOException, IOException {
        GenericPackager packager = new GenericPackager();
        packager.readFile(new ClassPathResource("packager/iso87ascii.xml").getInputStream());
        return packager;
    }

}
