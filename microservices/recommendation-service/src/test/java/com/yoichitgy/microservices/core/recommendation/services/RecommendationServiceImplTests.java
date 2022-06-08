package com.yoichitgy.microservices.core.recommendation.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static reactor.core.publisher.Mono.just;

import java.util.function.Consumer;

import com.yoichitgy.api.core.recommendation.Recommendation;
import com.yoichitgy.api.event.Event;
import com.yoichitgy.api.event.Event.Type;
import com.yoichitgy.api.exceptions.InvalidInputException;
import com.yoichitgy.microservices.core.recommendation.ContainerTestBase;
import com.yoichitgy.microservices.core.recommendation.persistence.RecommendationRepository;

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
class RecommendationServiceImplTests extends ContainerTestBase {
    @Autowired
    private WebTestClient client;
    @Autowired
    private RecommendationRepository repository;
    @Autowired
    @Qualifier("messageProcessor")
    private Consumer<Event<Integer, Recommendation>> messageProcessor;

    @BeforeEach
    void setupDb() {
        repository.deleteAll().block();
    }

    @Test
    void getRecommendationsByProductId() {
        int productId = 1;

        sendCreateRecommendationEvent(productId, 1);
        sendCreateRecommendationEvent(productId, 2);
        sendCreateRecommendationEvent(productId, 3);
        assertEquals(3, repository.findByProductId(productId).count().block());

        getAndVerifyRecommendationsByProductId(productId, HttpStatus.OK)
            .jsonPath("$.length()").isEqualTo(3)
            .jsonPath("$[2].productId").isEqualTo(productId)
            .jsonPath("$[2].recommendationId").isEqualTo(3);
    }

    @Test
    void duplicateError() {
        int productId = 1;
        int recommendationId = 1;
    
        sendCreateRecommendationEvent(productId, recommendationId);
        assertEquals(1, repository.count().block());
    
        var thrown = assertThrows(
            InvalidInputException.class,
            () -> sendCreateRecommendationEvent(productId, recommendationId),
            "Expected a InvalidInputException here!"
        );
        assertEquals("Duplicate key, productId: 1, recommendationId: 1", thrown.getMessage());
      
        assertEquals(1, repository.count().block());
    }
  
    @Test
    void deleteRecommendations() {
        int productId = 1;
        int recommendationId = 1;
        
        sendCreateRecommendationEvent(productId, recommendationId);
        assertEquals(1, repository.findByProductId(productId).count().block());
    
        sendDeleteRecommendationEvent(productId);
        assertEquals(0, repository.findByProductId(productId).count().block());
    
        sendDeleteRecommendationEvent(productId);
    }
  
    @Test
    void getRecommendationsMissingParameter() {
        getAndVerifyRecommendationsByProductId("", HttpStatus.BAD_REQUEST)
            .jsonPath("$.path").isEqualTo("/recommendation")
            .jsonPath("$.message").isEqualTo("Required int parameter 'productId' is not present");
    }
  
    @Test
    void getRecommendationsInvalidParameter() {
        getAndVerifyRecommendationsByProductId("?productId=no-integer", HttpStatus.BAD_REQUEST)
            .jsonPath("$.path").isEqualTo("/recommendation")
            .jsonPath("$.message").isEqualTo("Type mismatch.");
    }
  
    @Test
    void getRecommendationsNotFound() {
        int productIdNotFound = 113;
        getAndVerifyRecommendationsByProductId("?productId=" + productIdNotFound, HttpStatus.OK)
            .jsonPath("$.length()").isEqualTo(0);
    }
  
    @Test
    void getRecommendationsInvalidParameterNegativeValue() {
        int productIdInvalid = -1;
        getAndVerifyRecommendationsByProductId("?productId=" + productIdInvalid, HttpStatus.UNPROCESSABLE_ENTITY)
            .jsonPath("$.path").isEqualTo("/recommendation")
            .jsonPath("$.message").isEqualTo("Invalid productId: " + productIdInvalid);
    }

    private WebTestClient.BodyContentSpec getAndVerifyRecommendationsByProductId(int productId, HttpStatus expectedStatus) {
        return getAndVerifyRecommendationsByProductId("?productId=" + productId, expectedStatus);
    }

    private WebTestClient.BodyContentSpec getAndVerifyRecommendationsByProductId(String productIdQuery, HttpStatus expectedStatus) {
        return client.get()
            .uri("/recommendation" + productIdQuery)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isEqualTo(expectedStatus)
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody();
    }

    private void sendCreateRecommendationEvent(int productId, int recommendationId) {
        var recommendation = new Recommendation(
            productId,
            recommendationId,
            "Author " + recommendationId,
            recommendationId,
            "Content " + recommendationId,
            "SA"
        );
        var event = new Event<Integer, Recommendation>(Type.CREATE, productId, recommendation);
        messageProcessor.accept(event);
    }

    private void sendDeleteRecommendationEvent(int productId) {
        var event = new Event<Integer, Recommendation>(Type.DELETE, productId, null);
        messageProcessor.accept(event);
    }
}
