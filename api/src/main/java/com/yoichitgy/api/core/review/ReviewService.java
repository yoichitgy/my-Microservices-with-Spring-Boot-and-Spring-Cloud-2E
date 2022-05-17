package com.yoichitgy.api.core.review;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReviewService {
    Mono<Review> createReview(@RequestBody Review body);

    /**
     * Sample usage: "curl $HOST:$PORT/review?productId=1".
     *
     * @param productId Id of the product
     * @return the reviews of the product
     */    
    @GetMapping(value = "/review", produces = MediaType.APPLICATION_JSON_VALUE)
    Flux<Review> getReviews(@RequestParam(value = "productId", required = true) int productId);

    Mono<Void> deleteReviews(@RequestParam(value = "productId", required = true)  int productId);
}
