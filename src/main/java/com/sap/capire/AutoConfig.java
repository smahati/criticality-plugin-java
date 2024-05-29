package com.sap.capire;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class AutoConfig {

    @Bean
    @ConditionalOnMissingBean
    public CriticalityHandler someService() {
        return new CriticalityHandler();
    }

}