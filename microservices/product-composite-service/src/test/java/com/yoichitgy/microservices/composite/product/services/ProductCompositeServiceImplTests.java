package com.yoichitgy.microservices.composite.product.services;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;

import com.yoichitgy.api.core.product.Product;
import com.yoichitgy.api.core.recommendation.Recommendation;
import com.yoichitgy.api.core.review.Review;
import com.yoichitgy.api.exceptions.InvalidInputException;
import com.yoichitgy.api.exceptions.NotFoundException;
import com.yoichitgy.microservices.composite.product.TestSecurityConfiguration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    classes = {TestSecurityConfiguration.class},
    properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=",
        "spring.main.allow-bean-definition-overriding=true",
        "spring.cloud.config.enabled=false"
    }
)
class ProductCompositeServiceImplTests {
    private static final int PRODUCT_ID_OK = 1;
    private static final int PRODUCT_ID_NOT_FOUND = 2;
    private static final int PRODUCT_ID_INVALID = 3;
  
    @Autowired
    private WebTestClient client;
    @MockBean
    private ProductCompositeIntegration compositeIntegration;

    @BeforeEach
    void setUp() {
        when(compositeIntegration.getProduct(eq(PRODUCT_ID_OK), anyInt(), anyInt()))
            .thenReturn(Mono.just(new Product(PRODUCT_ID_OK, "name", 1, "mock-address")));
        when(compositeIntegration.getRecommendations(PRODUCT_ID_OK))
            .thenReturn(Flux.just(new Recommendation(PRODUCT_ID_OK, 1, "author", 1, "content", "mock address")));
        when(compositeIntegration.getReviews(PRODUCT_ID_OK))
            .thenReturn(Flux.just(new Review(PRODUCT_ID_OK, 1, "author", "subject", "content", "mock address")));
      
        when(compositeIntegration.getProduct(eq(PRODUCT_ID_NOT_FOUND), anyInt(), anyInt()))
            .thenThrow(new NotFoundException("NOT FOUND: " + PRODUCT_ID_NOT_FOUND));
      
        when(compositeIntegration.getProduct(eq(PRODUCT_ID_INVALID), anyInt(), anyInt()))
            .thenThrow(new InvalidInputException("INVALID: " + PRODUCT_ID_INVALID));
    }
 
    @Test
    void getProductById() {
        getAndVerifyProduct(PRODUCT_ID_OK, HttpStatus.OK)
            .jsonPath("$.productId").isEqualTo(PRODUCT_ID_OK)
            .jsonPath("$.recommendations.length()").isEqualTo(1)
            .jsonPath("$.reviews.length()").isEqualTo(1);
    }

    @Test
    void getProductNotFound() {
        getAndVerifyProduct(PRODUCT_ID_NOT_FOUND, HttpStatus.NOT_FOUND)
            .jsonPath("$.path").isEqualTo("/product-composite/" + PRODUCT_ID_NOT_FOUND)
            .jsonPath("$.message").isEqualTo("NOT FOUND: " + PRODUCT_ID_NOT_FOUND);
    }
  
    @Test
    void getProductInvalidInput() {
        getAndVerifyProduct(PRODUCT_ID_INVALID, HttpStatus.UNPROCESSABLE_ENTITY)
            .jsonPath("$.path").isEqualTo("/product-composite/" + PRODUCT_ID_INVALID)
            .jsonPath("$.message").isEqualTo("INVALID: " + PRODUCT_ID_INVALID);
    }

    private WebTestClient.BodyContentSpec getAndVerifyProduct(int productId, HttpStatus expectedStatus) {
        return client.get()
            .uri("/product-composite/" + productId)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isEqualTo(expectedStatus)
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody();
    }
}
