package com.yoichitgy.api.composite.product;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import reactor.core.publisher.Mono;

@SecurityRequirement(name = "security_auth")
@Tag(name = "ProductComposite", description = "REST API for composite product information.")
public interface ProductCompositeService {
    /**
     * Sample usage, see below.
     *
     * curl -X POST $HOST:$PORT/product-composite \
     *   -H "Content-Type: application/json" --data \
     *   '{"productId":123,"name":"product 123","weight":123}'
     *
     * @param body A JSON representation of the new composite product
     */
    @Operation(
        summary = "${api.product-composite.create-composite-product.description}",
        description = "${api.product-composite.create-composite-product.notes}"
    )
    @ApiResponse(responseCode = "400", description = "${api.responseCodes.badRequest.description}")
    @ApiResponse(responseCode = "422", description = "${api.responseCodes.unprocessableEntity.description}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(value = "/product-composite", consumes = MediaType.APPLICATION_JSON_VALUE)
    Mono<Void> createProduct(@RequestBody ProductAggregate body);

    /**
     * Sample usage: "curl $HOST:$PORT/product-composite/1".
     *
     * @param productId Id of the product
     * @return the composite product info, if found, else null
     */
    @Operation(
        summary = "${api.product-composite.get-composite-product.description}",
        description = "${api.product-composite.get-composite-product.notes}"
    )
    @ApiResponse(responseCode = "200", description = "${api.responseCodes.ok.description}")
    @ApiResponse(responseCode = "400", description = "${api.responseCodes.badRequest.description}")
    @ApiResponse(responseCode = "404", description = "${api.responseCodes.notFound.description}")
    @ApiResponse(responseCode = "422", description = "${api.responseCodes.unprocessableEntity.description}")    
    @GetMapping(value = "/product-composite/{productId}", produces = MediaType.APPLICATION_JSON_VALUE)
    Mono<ProductAggregate> getProduct(
        @PathVariable int productId,
        @RequestParam(value = "delay", required = false, defaultValue = "0") int delay,
        @RequestParam(value = "faultPercent", required = false, defaultValue = "0") int faultPercent
    );

    /**
     * Sample usage: "curl -X DELETE $HOST:$PORT/product-composite/1".
     *
     * @param productId Id of the product
     */
    @Operation(
        summary = "${api.product-composite.delete-composite-product.description}",
        description = "${api.product-composite.delete-composite-product.notes}"
    )
    @ApiResponse(responseCode = "400", description = "${api.responseCodes.badRequest.description}")
    @ApiResponse(responseCode = "422", description = "${api.responseCodes.unprocessableEntity.description}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @DeleteMapping(value = "/product-composite/{productId}")
    Mono<Void> deleteProduct(@PathVariable int productId);
}
