package com.yoichitgy.microservices.core.review.services;

import com.yoichitgy.microservices.core.review.ContainerTestBase;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ReviewServiceImplTests extends ContainerTestBase {
    @Autowired
    private WebTestClient client;

    @Test
    void getReviewsByProductId() {
        int productId = 1;

        client.get()
        .uri("/review?productId=" + productId)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk()
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
            .jsonPath("$.length()").isEqualTo(3)
            .jsonPath("$[0].productId").isEqualTo(productId);
    }
  
    @Test
    void getReviewsMissingParameter() {
        client.get()
            .uri("/review")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
                .jsonPath("$.path").isEqualTo("/review")
                .jsonPath("$.message").isEqualTo("Required int parameter 'productId' is not present");
    }
  
    @Test
    void getReviewsInvalidParameter() {
        client.get()
            .uri("/review?productId=no-integer")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
                .jsonPath("$.path").isEqualTo("/review")
                .jsonPath("$.message").isEqualTo("Type mismatch.");
    }
  
    @Test
    void getReviewsNotFound() {  
        int productIdNotFound = 213;
    
        client.get()
            .uri("/review?productId=" + productIdNotFound)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
                .jsonPath("$.length()").isEqualTo(0);
    }
  
    @Test
    void getReviewsInvalidParameterNegativeValue() {  
        int productIdInvalid = -1;
    
        client.get()
            .uri("/review?productId=" + productIdInvalid)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
                .jsonPath("$.path").isEqualTo("/review")
                .jsonPath("$.message").isEqualTo("Invalid productId: " + productIdInvalid);
    }    
}
