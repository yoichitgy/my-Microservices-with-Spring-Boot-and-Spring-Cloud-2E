package com.yoichitgy.microservices.composite.product.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static reactor.core.publisher.Mono.just;

import java.util.List;

import com.yoichitgy.api.composite.product.ProductAggregate;
import com.yoichitgy.api.composite.product.RecommendationSummary;
import com.yoichitgy.api.composite.product.ReviewSummary;
import com.yoichitgy.api.core.product.Product;
import com.yoichitgy.api.core.recommendation.Recommendation;
import com.yoichitgy.api.core.review.Review;
import com.yoichitgy.api.exceptions.InvalidInputException;
import com.yoichitgy.api.exceptions.NotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
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
        when(compositeIntegration.getProduct(PRODUCT_ID_OK))
            .thenReturn(new Product(PRODUCT_ID_OK, "name", 1, "mock-address"));
        when(compositeIntegration.getRecommendations(PRODUCT_ID_OK))
            .thenReturn(List.of(new Recommendation(PRODUCT_ID_OK, 1, "author", 1, "content", "mock address")));
        when(compositeIntegration.getReviews(PRODUCT_ID_OK))
            .thenReturn(List.of(new Review(PRODUCT_ID_OK, 1, "author", "subject", "content", "mock address")));
      
        when(compositeIntegration.getProduct(PRODUCT_ID_NOT_FOUND))
            .thenThrow(new NotFoundException("NOT FOUND: " + PRODUCT_ID_NOT_FOUND));
      
        when(compositeIntegration.getProduct(PRODUCT_ID_INVALID))
            .thenThrow(new InvalidInputException("INVALID: " + PRODUCT_ID_INVALID));

    }

    @Test
    void createCompositeProduct1() {
        var compositeProduct = new ProductAggregate(1, "name", 1, List.of(), List.of(), null);    
        postAndVerifyProduct(compositeProduct, HttpStatus.OK);
    }
  
    @Test
    void createCompositeProduct2() {
        var compositeProduct = new ProductAggregate(
            1,
            "name",
            1,
            List.of(new RecommendationSummary(1, "a", 1, "c")),
            List.of(new ReviewSummary(1, "a", "s", "c")),
            null)
        ;
        postAndVerifyProduct(compositeProduct, HttpStatus.OK);
    }
  
    @Test
    void deleteCompositeProduct() {
        var compositeProduct = new ProductAggregate(
            1,
            "name",
            1,
            List.of(new RecommendationSummary(1, "a", 1, "c")),
            List.of(new ReviewSummary(1, "a", "s", "c")),
            null
        );
        postAndVerifyProduct(compositeProduct, HttpStatus.OK);

        deleteAndVerifyProduct(compositeProduct.getProductId(), HttpStatus.OK);
        deleteAndVerifyProduct(compositeProduct.getProductId(), HttpStatus.OK);
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
    
    private void postAndVerifyProduct(ProductAggregate compositeProduct, HttpStatus expectedStatus) {
        client.post()
            .uri("/product-composite")
            .body(just(compositeProduct), ProductAggregate.class)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isEqualTo(expectedStatus);
    }

    private void deleteAndVerifyProduct(int productId, HttpStatus expectedStatus) {
        client.delete()
            .uri("/product-composite/" + productId)
            .exchange()
            .expectStatus().isEqualTo(expectedStatus);
    }    
}
