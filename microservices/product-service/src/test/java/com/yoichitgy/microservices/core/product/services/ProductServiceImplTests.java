package com.yoichitgy.microservices.core.product.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Consumer;

import com.yoichitgy.api.core.product.Product;
import com.yoichitgy.api.event.Event;
import com.yoichitgy.api.event.Event.Type;
import com.yoichitgy.api.exceptions.InvalidInputException;
import com.yoichitgy.microservices.core.product.ContainerTestBase;
import com.yoichitgy.microservices.core.product.persistence.ProductRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.sleuth.mongodb.enabled=false",
        "spring.cloud.config.enabled=false"
    }
)
class ProductServiceImplTests extends ContainerTestBase {
    @Autowired
    private WebTestClient client;
    @Autowired
    private ProductRepository repository;
    @Autowired
    @Qualifier("messageProcessor")
    private Consumer<Event<Integer, Product>> messageProcessor;

    @BeforeEach
    void setupDb() {
        repository.deleteAll().block();
    }

    @Test
    void getProductById() {
        int productId = 1;
        
        sendCreateProductEvent(productId);

        assertNotNull(repository.findByProductId(productId).block());
        assertEquals(1, (long)repository.count().block());
    
        getAndVerifyProduct(productId, HttpStatus.OK)
            .jsonPath("$.productId").isEqualTo(productId);
    }

    @Test
    void duplicateError() {
        int productId = 1;

        sendCreateProductEvent(productId);
        assertNotNull(repository.findByProductId(productId).block());

        var thrown = assertThrows(
            InvalidInputException.class,
            () -> sendCreateProductEvent(productId),
            "Expected a InvalidInputException here!"
        );
        assertEquals("Duplicate key, productId: " + productId, thrown.getMessage());
    }

    @Test
    void deleteProduct() {
        int productId = 1;

        sendCreateProductEvent(productId);
        assertNotNull(repository.findByProductId(productId).block());
    
        sendDeleteProductEvent(productId);
        assertNull(repository.findByProductId(productId).block());
    
        sendDeleteProductEvent(productId);    
    }
   
    @Test
    void getProductInvalidParameterString() {
        getAndVerifyProduct("/no-integer", HttpStatus.BAD_REQUEST)
            .jsonPath("$.path").isEqualTo("/product/no-integer")
            .jsonPath("$.message").isEqualTo("Type mismatch.");
    }
  
    @Test
    void getProductNotFound() {
        int productIdNotFound = 13;
        getAndVerifyProduct(productIdNotFound, HttpStatus.NOT_FOUND)
            .jsonPath("$.path").isEqualTo("/product/" + productIdNotFound)
            .jsonPath("$.message").isEqualTo("No product found for productId: " + productIdNotFound);
    }
  
    @Test
    void getProductInvalidParameterNegativeValue() {  
        int productIdInvalid = -1;
        getAndVerifyProduct(productIdInvalid, HttpStatus.UNPROCESSABLE_ENTITY)
            .jsonPath("$.path").isEqualTo("/product/" + productIdInvalid)
            .jsonPath("$.message").isEqualTo("Invalid productId: " + productIdInvalid);
    }

    private WebTestClient.BodyContentSpec getAndVerifyProduct(int productId, HttpStatus expectedStatus) {
        return getAndVerifyProduct("/" + productId, expectedStatus);
    }

    private WebTestClient.BodyContentSpec getAndVerifyProduct(String productIdPath, HttpStatus expectedStatus) {
        return client.get()
            .uri("/product" + productIdPath)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isEqualTo(expectedStatus)
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody();
    }

    private void sendCreateProductEvent(int productId) {
        var product = new Product(productId, "Name " + productId, productId, "SA");
        var event = new Event<Integer, Product>(Type.CREATE, productId, product);
        messageProcessor.accept(event);
    }

    private void sendDeleteProductEvent(int productId) {
        var event = new Event<Integer, Product>(Type.DELETE, productId, null);
        messageProcessor.accept(event);
    }
}
