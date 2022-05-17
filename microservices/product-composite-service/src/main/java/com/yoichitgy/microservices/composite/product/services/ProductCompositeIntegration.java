package com.yoichitgy.microservices.composite.product.services;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yoichitgy.api.core.product.Product;
import com.yoichitgy.api.core.product.ProductService;
import com.yoichitgy.api.core.recommendation.Recommendation;
import com.yoichitgy.api.core.recommendation.RecommendationService;
import com.yoichitgy.api.core.review.Review;
import com.yoichitgy.api.core.review.ReviewService;
import com.yoichitgy.api.exceptions.InvalidInputException;
import com.yoichitgy.api.exceptions.NotFoundException;
import com.yoichitgy.util.http.HttpErrorInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class ProductCompositeIntegration implements ProductService, RecommendationService, ReviewService {
    private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeIntegration.class);
    
    private final WebClient webClient;
    private final ObjectMapper mapper;

    private final String productServiceUrl;
    private final String recommendationServiceUrl;
    private final String reviewServiceUrl;

    @Autowired
    public ProductCompositeIntegration(
        WebClient.Builder webClient,
        ObjectMapper mapper,
        @Value("${app.product-service.host}") String productServiceHost,
        @Value("${app.product-service.port}") int productServicePort,
        @Value("${app.recommendation-service.host}") String recommendationServiceHost,
        @Value("${app.recommendation-service.port}") int recommendationServicePort,
        @Value("${app.review-service.host}") String reviewServiceHost,
        @Value("${app.review-service.port}") int reviewServicePort
    ) {
        this.webClient = webClient.build();
        this.mapper = mapper;

        var format = "http://%s:%d%s";
        productServiceUrl = String.format(format, productServiceHost, productServicePort, "/product");
        recommendationServiceUrl = String.format(format, recommendationServiceHost, recommendationServicePort, "/recommendation");
        reviewServiceUrl = String.format(format, reviewServiceHost, reviewServicePort, "/review");
    }

    @Override
    public Mono<Product> createProduct(Product body) {
        return Mono.empty();

        // try {
        //     var url = productServiceUrl;
        //     LOG.debug("Will post a new product to URL: {}", url);

        //     var product = restTemplate.postForObject(url, body, Product.class);
        //     LOG.debug("Created a product with id: {}", product.getProductId());

        //     return product;
        // } catch (HttpClientErrorException ex) {
        //     throw handleHttpClientException(ex);
        // }
    }

    @Override
    public Mono<Product> getProduct(int productId) {
        var url = productServiceUrl + "/" + productId;
        LOG.debug("Will call getProduct API on URL: {}", url);

        return webClient.get()
            .uri(url)
            .retrieve()
            .bodyToMono(Product.class)
            .log(LOG.getName(), Level.FINE)
            .onErrorMap(WebClientResponseException.class, ex -> handleException(ex));
    }

    @Override
    public Mono<Void> deleteProduct(int productId) {
        return Mono.empty().then();

        // try {
        //     var url = productServiceUrl + "/" + productId;
        //     LOG.debug("Will call the deleteProduct API on URL: {}", url);

        //     restTemplate.delete(url);
        // } catch (HttpClientErrorException ex) {
        //     throw handleHttpClientException(ex);
        // }
    }

    @Override
    public Mono<Recommendation> createRecommendation(Recommendation body) {
        return Mono.empty();

        // try {
        //     var url = recommendationServiceUrl;
        //     LOG.debug("Will post a new recommendation to URL: {}", url);
    
        //     var recommendation = restTemplate.postForObject(url, body, Recommendation.class);
        //     LOG.debug("Created a recommendation with id: {}", recommendation.getProductId());
    
        //     return recommendation;
        // } catch (HttpClientErrorException ex) {
        //     throw handleHttpClientException(ex);
        // }
    }

    @Override
    public Flux<Recommendation> getRecommendations(int productId) {
        var url = recommendationServiceUrl + "?productId=" + productId;
        LOG.debug("Will call getRecommendations API on URL: {}", url);

        return webClient.get()
            .uri(url).
            retrieve()
            .bodyToFlux(Recommendation.class)
            .log(LOG.getName(), Level.FINE)
            .onErrorResume(error -> {
                LOG.warn("Got an error while requesting recommendations, return zero recommendations: {}", error.getMessage());
                return Flux.empty();
            });
    }

    @Override
    public Mono<Void> deleteRecommendations(int productId) {
        return Mono.empty().then();

        // try {
        //     var url = recommendationServiceUrl + "?productId=" + productId;
        //     LOG.debug("Will call the deleteRecommendations API on URL: {}", url);
      
        //     restTemplate.delete(url);
        // } catch (HttpClientErrorException ex) {
        //     throw handleHttpClientException(ex);
        // }
    }

    @Override
    public Mono<Review> createReview(Review body) {
        return Mono.empty();

        // try {
        //     var url = reviewServiceUrl;
        //     LOG.debug("Will post a new review to URL: {}", url);
      
        //     var review = restTemplate.postForObject(url, body, Review.class);
        //     LOG.debug("Created a review with id: {}", review.getProductId());
      
        //     return review;
        // } catch (HttpClientErrorException ex) {
        //     throw handleHttpClientException(ex);
        // }
    }

    @Override
    public Flux<Review> getReviews(int productId) {
        var url = reviewServiceUrl + "?productId=" + productId;
        LOG.debug("Will call getReviews API on URL: {}", url);

        return webClient.get()
            .uri(url)
            .retrieve()
            .bodyToFlux(Review.class)
            .log(LOG.getName(), Level.FINE)
            .onErrorResume(error -> { 
                LOG.warn("Got an error while requesting reviews, return zero reviews: {}", error.getMessage());
                return Flux.empty();
            });
    }

    @Override
    public Mono<Void> deleteReviews(int productId) {
        return Mono.empty().then();

        // try {
        //     String url = reviewServiceUrl + "?productId=" + productId;
        //     LOG.debug("Will call the deleteReviews API on URL: {}", url);
      
        //     restTemplate.delete(url);
        // } catch (HttpClientErrorException ex) {
        //     throw handleHttpClientException(ex);
        // }
    }

    private Throwable handleException(Throwable ex) {
        if (!(ex instanceof WebClientResponseException)) {
            LOG.warn("Got a unexpected error: {}, will rethrow it", ex.toString());
            return ex;
        }
        var wcre = (WebClientResponseException)ex;

        switch (wcre.getStatusCode()) {
            case NOT_FOUND:
                return new NotFoundException(getErrorMessage(wcre));        
            case UNPROCESSABLE_ENTITY :
                return new InvalidInputException(getErrorMessage(wcre));        
            default:
                LOG.warn("Got an unexpected HTTP error: {}, will rethrow it", wcre.getStatusCode());
                LOG.warn("Error body: {}", wcre.getResponseBodyAsString());
                return ex;
        }
    }
    
    private String getErrorMessage(WebClientResponseException ex) {
        try {
            return mapper.readValue(ex.getResponseBodyAsString(), HttpErrorInfo.class).getMessage();
        } catch (IOException ioex) {
            return ex.getMessage();
        }
    }
}
