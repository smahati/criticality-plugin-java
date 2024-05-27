package com.sap.capire;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;

@AutoConfiguration
public class AutoConfig {

    @Bean
    @ConditionalOnMissingBean
    public HelloWorldSayer someService() {
        return new HelloWorldSayer();
    }

    public class HelloWorldSayer {
        @EventListener(ApplicationReadyEvent.class)
        public void sayHello() {
            System.out.println("Hello World");
        }
    }
}