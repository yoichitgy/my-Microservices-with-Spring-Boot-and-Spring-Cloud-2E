package com.yoichitgy.microservices.core.product.services;

import java.util.function.Consumer;

import com.yoichitgy.api.core.product.Product;
import com.yoichitgy.api.core.product.ProductService;
import com.yoichitgy.api.event.Event;
import com.yoichitgy.api.exceptions.EventProcessingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessageProcessorConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(MessageProcessorConfiguration.class);

    private final ProductService productService;

    @Autowired
    public MessageProcessorConfiguration(ProductService productService) {
        this.productService = productService;
    }

    @Bean
    public Consumer<Event<Integer, Product>> messageProcessor() {
        return event -> {
            LOG.info("Process message created at {}...", event.getEventCreatedAt());

            switch (event.getEventType()) {
                case CREATE:
                    var product = event.getData();
                    LOG.info("Create product with ID: {}", product.getProductId());
                    productService.createProduct(product).block();
                    break;
                case DELETE:
                    int productId = event.getKey();
                    LOG.info("Delete recommendations with ProductID: {}", productId);
                    productService.deleteProduct(productId).block();
                    break;
                default:
                    String errorMessage = "Incorrect event type: " + event.getEventType() + ", expected a CREATE or DELETE event";
                    LOG.warn(errorMessage);
                    throw new EventProcessingException(errorMessage);
            }
            LOG.info("Message processing done!");
        };
    }
}
