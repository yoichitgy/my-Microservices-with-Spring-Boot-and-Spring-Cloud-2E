package com.yoichitgy.api.core.product;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import reactor.core.publisher.Mono;

public interface ProductService {
    Mono<Product> createProduct(Product body);

    /**
     * Sample usage: "curl $HOST:$PORT/product/1".
     *
     * @param productId Id of the product
     * @return the product, if found, else empty.
     */
    @GetMapping(value = "/product/{productId}", produces = MediaType.APPLICATION_JSON_VALUE)
    Mono<Product> getProduct(@PathVariable int productId);

    Mono<Void> deleteProduct(int productId);
}
