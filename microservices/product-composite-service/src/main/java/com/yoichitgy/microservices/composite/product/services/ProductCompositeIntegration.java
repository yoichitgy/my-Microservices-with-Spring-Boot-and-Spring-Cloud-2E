package com.yoichitgy.microservices.composite.product.services;

import java.io.IOException;
import java.util.logging.Level;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yoichitgy.api.core.product.Product;
import com.yoichitgy.api.core.product.ProductService;
import com.yoichitgy.api.core.recommendation.Recommendation;
import com.yoichitgy.api.core.recommendation.RecommendationService;
import com.yoichitgy.api.core.review.Review;
import com.yoichitgy.api.core.review.ReviewService;
import com.yoichitgy.api.event.Event;
import com.yoichitgy.api.event.Event.Type;
import com.yoichitgy.api.exceptions.InvalidInputException;
import com.yoichitgy.api.exceptions.NotFoundException;
import com.yoichitgy.util.http.HttpErrorInfo;
import com.yoichitgy.util.http.ServiceUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@Component
public class ProductCompositeIntegration implements ProductService, RecommendationService, ReviewService {
    private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeIntegration.class);

    private static final String PRODUCT_SERVICE_URL = "http://product";
    private static final String RECOMMENDATION_SERVICE_URL = "http://recommendation";
    private static final String REVIEW_SERVICE_URL = "http://review";
  
    private final WebClient webClient;
    private final ObjectMapper mapper;
    private final StreamBridge streamBridge;
    private final Scheduler publishEventScheduler;

    private final ServiceUtil serviceUtil;

    @Autowired
    public ProductCompositeIntegration(
        WebClient.Builder webClientBuilder,
        ObjectMapper mapper,
        StreamBridge streamBridge,
        @Qualifier("publishEventScheduler") Scheduler publishEventScheduler,
        ServiceUtil serviceUtil
    ) {
        this.webClient = webClientBuilder.build();
        this.mapper = mapper;
        this.streamBridge = streamBridge;
        this.publishEventScheduler = publishEventScheduler;
        this.serviceUtil = serviceUtil;
    }

    @Override
    public Mono<Product> createProduct(Product body) {
        return Mono.fromCallable(() -> {
            var event = new Event<Integer, Product>(Type.CREATE, body.getProductId(), body);
            sendMessage("products-out-0", event);
            return body;
        }).subscribeOn(publishEventScheduler);
    }

    @Override
    @Retry(name = "product")
    @TimeLimiter(name = "product")
    @CircuitBreaker(name = "product", fallbackMethod = "getProductFallbackValue")
    public Mono<Product> getProduct(int productId, int delay, int faultPercent) {
        var url = UriComponentsBuilder.fromUriString(
            PRODUCT_SERVICE_URL + "/product/{productId}?delay={delay}&faultPercent={faultPercent}"
        ).build(productId, delay, faultPercent);
        LOG.debug("Will call getProduct API on URL: {}", url);

        return webClient.get()
            .uri(url)
            .retrieve()
            .bodyToMono(Product.class)
            .log(LOG.getName(), Level.FINE)
            .onErrorMap(WebClientResponseException.class, ex -> handleException(ex));
    }

    private Mono<Product> getProductFallbackValue(int productId, int delay, int faultPercent, CallNotPermittedException ex) {
        LOG.warn("Creating a fail-fast fallback product for productId = {}, delay = {}, faultPercent = {} and exception = {} ",
            productId, delay, faultPercent, ex.toString());

        if (productId == 13) {
            String errMsg = "Product Id: " + productId + " not found in fallback cache!";
            LOG.warn(errMsg);
            throw new NotFoundException(errMsg);      
        }

        var fallback = new Product(productId, "Fallback product" + productId, productId, serviceUtil.getServiceAddress());
        return Mono.just(fallback);
    }

    @Override
    public Mono<Void> deleteProduct(int productId) {
        return Mono.fromRunnable(() -> {
            var event = new Event<Integer, Product>(Type.DELETE, productId, null);
            sendMessage("products-out-0", event);
        }).subscribeOn(publishEventScheduler).then();
    }

    @Override
    public Mono<Recommendation> createRecommendation(Recommendation body) {
        return Mono.fromCallable(() -> {
            var event = new Event<Integer, Recommendation>(Type.CREATE, body.getProductId(), body);
            sendMessage("recommendations-out-0", event);
            return body;
        }).subscribeOn(publishEventScheduler);
    }

    @Override
    public Flux<Recommendation> getRecommendations(int productId) {
        var url = UriComponentsBuilder.fromUriString(
            RECOMMENDATION_SERVICE_URL + "/recommendation?productId={productId}"
        ).build(productId);
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
        return Mono.fromRunnable(() -> {
            var event = new Event<Integer, Recommendation>(Type.DELETE, productId, null);
            sendMessage("recommendations-out-0", event);
        }).subscribeOn(publishEventScheduler).then();
    }

    @Override
    public Mono<Review> createReview(Review body) {
        return Mono.fromCallable(() -> {
            var event = new Event<Integer, Review>(Type.CREATE, body.getProductId(), body);
            sendMessage("reviews-out-0", event);
            return body;
        }).subscribeOn(publishEventScheduler);
    }

    @Override
    public Flux<Review> getReviews(int productId) {
        var url = UriComponentsBuilder.fromUriString(
            REVIEW_SERVICE_URL + "/review?productId={productId}"
        ).build(productId);
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
        return Mono.fromRunnable(() -> {
            var event = new Event<Integer, Review>(Type.DELETE, productId, null);
            sendMessage("reviews-out-0", event);
        }).subscribeOn(publishEventScheduler).then();
    }

    private void sendMessage(String bindingName, Event event) {
        LOG.debug("Sending a {} message to {}", event.getEventType(), bindingName);
        var message = MessageBuilder.withPayload(event)
            .setHeader("partitionKey", event.getKey())
            .build();
        streamBridge.send(bindingName, message);
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
