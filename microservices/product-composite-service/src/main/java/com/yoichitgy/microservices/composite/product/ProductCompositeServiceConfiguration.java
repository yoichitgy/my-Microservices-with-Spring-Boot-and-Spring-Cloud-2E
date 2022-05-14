package com.yoichitgy.microservices.composite.product;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ProductCompositeServiceConfiguration {
    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
