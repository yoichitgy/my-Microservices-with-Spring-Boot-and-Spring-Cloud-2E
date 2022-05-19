package com.yoichitgy.microservices.composite.product.services;

import java.util.LinkedHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.CompositeReactiveHealthContributor;
import org.springframework.boot.actuate.health.ReactiveHealthContributor;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HealthCheckConfiguration {
    private final ProductCompositeIntegration integration;

    @Autowired
    public HealthCheckConfiguration(ProductCompositeIntegration integration) {
        this.integration = integration;
    }

    @Bean
    ReactiveHealthContributor coreServices() {
  
      var registry = new LinkedHashMap<String, ReactiveHealthIndicator>();
  
      registry.put("product", () -> integration.getProductHealth());
      registry.put("recommendation", () -> integration.getRecommendationHealth());
      registry.put("review", () -> integration.getReviewHealth());
  
      return CompositeReactiveHealthContributor.fromMap(registry);
    }  
}
