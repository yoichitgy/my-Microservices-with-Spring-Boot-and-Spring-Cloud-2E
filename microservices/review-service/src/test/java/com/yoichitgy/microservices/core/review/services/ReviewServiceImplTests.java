package com.yoichitgy.microservices.core.review.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.function.Consumer;

import com.yoichitgy.api.core.review.Review;
import com.yoichitgy.api.event.Event;
import com.yoichitgy.api.event.Event.Type;
import com.yoichitgy.api.exceptions.InvalidInputException;
import com.yoichitgy.microservices.core.review.ContainerTestBase;
import com.yoichitgy.microservices.core.review.persistence.ReviewRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = {"eureka.client.enabled=false"})
class ReviewServiceImplTests extends ContainerTestBase {
    @Autowired
    private WebTestClient client;
    @Autowired
    private ReviewRepository repository;
    @Autowired
    @Qualifier("messageProcessor")
    private Consumer<Event<Integer, Review>> messageProcessor;
  
    @BeforeEach
    void setupDb() {
        repository.deleteAll();
    }

    @Test
    void getReviewsByProductId() {
        int productId = 1;
        sendCreateReviewEvent(productId, 1);  
        sendCreateReviewEvent(productId, 2);  
        sendCreateReviewEvent(productId, 3);  
        assertEquals(3, repository.findByProductId(productId).size());
    
        getAndVerifyReviewsByProductId(productId, HttpStatus.OK)
            .jsonPath("$.length()").isEqualTo(3)
            .jsonPath("$[2].productId").isEqualTo(productId)
            .jsonPath("$[2].reviewId").isEqualTo(3);
    }

    @Test
    void duplicateError() {
        int productId = 1;
        int reviewId = 1;

        sendCreateReviewEvent(productId, reviewId);
        assertEquals(1, repository.count());

        var thrown = assertThrows(
            InvalidInputException.class,
            () -> sendCreateReviewEvent(productId, reviewId),
            "Expected a InvalidInputException here!"
        );
        assertEquals("Duplicate key, productId: 1, reviewId: 1", thrown.getMessage());
          
        assertEquals(1, repository.count());
    }
  
    @Test
    void deleteReviews() {
        int productId = 1;
        int reviewId = 1;    
        
        sendCreateReviewEvent(productId, reviewId);
        assertEquals(1, repository.findByProductId(productId).size());
    
        sendDeleteReviewEvent(productId);
        assertEquals(0, repository.findByProductId(productId).size());
    
        sendDeleteReviewEvent(productId);
    }

    @Test
    void getReviewsMissingParameter() {
        getAndVerifyReviewsByProductId("", HttpStatus.BAD_REQUEST)
            .jsonPath("$.path").isEqualTo("/review")
            .jsonPath("$.message").isEqualTo("Required int parameter 'productId' is not present");
    }
  
    @Test
    void getReviewsInvalidParameter() {
        getAndVerifyReviewsByProductId("?productId=no-integer", HttpStatus.BAD_REQUEST)
            .jsonPath("$.path").isEqualTo("/review")
            .jsonPath("$.message").isEqualTo("Type mismatch.");
    }
  
    @Test
    void getReviewsNotFound() {  
        int productIdNotFound = 213;
        getAndVerifyReviewsByProductId("?productId=" + productIdNotFound, HttpStatus.OK)
            .jsonPath("$.length()").isEqualTo(0);
    }
  
    @Test
    void getReviewsInvalidParameterNegativeValue() {  
        int productIdInvalid = -1;
        getAndVerifyReviewsByProductId("?productId=" + productIdInvalid, HttpStatus.UNPROCESSABLE_ENTITY)
            .jsonPath("$.path").isEqualTo("/review")
            .jsonPath("$.message").isEqualTo("Invalid productId: " + productIdInvalid);      
    }    

    private WebTestClient.BodyContentSpec getAndVerifyReviewsByProductId(int productId, HttpStatus expectedStatus) {
        return getAndVerifyReviewsByProductId("?productId=" + productId, expectedStatus);
    }

    private WebTestClient.BodyContentSpec getAndVerifyReviewsByProductId(String productIdQuery, HttpStatus expectedStatus) {
        return client.get()
            .uri("/review" + productIdQuery)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isEqualTo(expectedStatus)
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody();
    }

    private void sendCreateReviewEvent(int productId, int reviewId) {
        var review = new Review(
            productId,
            reviewId,
            "Author " + reviewId,
            "Subject " + reviewId,
            "Content " + reviewId,
            "SA"
        );
        var event = new Event<Integer, Review>(Type.CREATE, productId, review);
        messageProcessor.accept(event);
      }
    
    private void sendDeleteReviewEvent(int productId) {
        var event = new Event<Integer, Review>(Type.DELETE, productId, null);
        messageProcessor.accept(event);
    }
}
