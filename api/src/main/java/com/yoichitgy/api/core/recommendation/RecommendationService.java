package com.yoichitgy.api.core.recommendation;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RecommendationService {
    Mono<Recommendation> createRecommendation(Recommendation body);

    /**
     * Sample usage: "curl $HOST:$PORT/recommendation?productId=1".
     *
     * @param productId Id of the product
     * @return the recommendations of the product
     */
    @GetMapping(value = "/recommendation", produces = MediaType.APPLICATION_JSON_VALUE)
    Flux<Recommendation> getRecommendations(
        @RequestHeader HttpHeaders headers,
        @RequestParam(value = "productId", required = true) int productId
    );

    Mono<Void> deleteRecommendations(int productId);
}
