package com.yoichitgy.microservices.composite.product.services;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.yoichitgy.api.composite.product.ProductAggregate;
import com.yoichitgy.api.composite.product.ProductCompositeService;
import com.yoichitgy.api.composite.product.RecommendationSummary;
import com.yoichitgy.api.composite.product.ReviewSummary;
import com.yoichitgy.api.composite.product.ServiceAddresses;
import com.yoichitgy.api.core.product.Product;
import com.yoichitgy.api.core.recommendation.Recommendation;
import com.yoichitgy.api.core.review.Review;
import com.yoichitgy.util.http.ServiceUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
public class ProductCompositeServiceImpl implements ProductCompositeService {
    private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeServiceImpl.class);

    private final ServiceUtil serviceUtil;
    private final ProductCompositeIntegration integration;
    
    @Autowired
    public ProductCompositeServiceImpl(ServiceUtil serviceUtil, ProductCompositeIntegration integration) {
        this.serviceUtil = serviceUtil;
        this.integration = integration;
    }

    @Override
    public Mono<Void> createProduct(ProductAggregate body) {
        try {
            List<Mono> monoList = new ArrayList<>();

            int productId = body.getProductId();
            LOG.debug("createCompositeProduct: creates a new composite entity for productId: {}", productId);

            var product = new Product(productId, body.getName(), body.getWeight(), null);
            monoList.add(integration.createProduct(product));

            var recommendations = body.getRecommendations();
            if (recommendations != null) {
                recommendations.forEach(r -> {
                    var recommendation = new Recommendation(
                        productId,
                        r.getRecommendationId(),
                        r.getAuthor(),
                        r.getRate(),
                        r.getContent(),
                        null
                    );
                    monoList.add(integration.createRecommendation(recommendation));
                });
            }

            var reviews = body.getReviews();
            if (reviews != null) {
                reviews.forEach(r -> {
                    var review = new Review(
                        productId,
                        r.getReviewId(),
                        r.getAuthor(),
                        r.getSubject(),
                        r.getContent(),
                        null
                    );
                    monoList.add(integration.createReview(review));
                });
            }

            LOG.debug("createCompositeProduct: composite entities created for productId: {}", productId);

            return Mono.zip(r -> "", monoList.toArray(new Mono[0]))
                .doOnError(ex -> LOG.warn("createCompositeProduct failed: {}", ex.toString()))
                .then();
        } catch (RuntimeException ex) {
            LOG.warn("createCompositeProduct failed", ex);
            throw ex;
        }
    }

    @Override
    public Mono<ProductAggregate> getProduct(int productId) {
        LOG.info("Will get composite product info for product.id={}", productId);

        return Mono.zip(
                integration.getProduct(productId),
                integration.getRecommendations(productId).collectList(),
                integration.getReviews(productId).collectList()
            ).map(values -> {
                var product = values.getT1();
                var recommendations = values.getT2();
                var reviews = values.getT3();
                var address = serviceUtil.getServiceAddress();
                return createProductAggregate(product, recommendations, reviews, address);
            }).doOnError(ex -> LOG.warn("getCompositeProduct failed: {}", ex.toString()))
            .log(LOG.getName(), Level.FINE);
    }

    @Override
    public Mono<Void> deleteProduct(int productId) {
        LOG.debug("deleteCompositeProduct: Deletes a product aggregate for productId: {}", productId);

        try {
            return Mono.zip(
                    integration.deleteProduct(productId),
                    integration.deleteRecommendations(productId),
                    integration.deleteReviews(productId)
                ).doOnError(ex -> LOG.warn("delete failed: {}", ex.toString()))
                .log(LOG.getName(), Level.FINE)
                .then();
        } catch (RuntimeException ex) {
            LOG.warn("deleteCompositeProduct failed: {}", ex.toString());
            throw ex;
        }
    }

    private ProductAggregate createProductAggregate(
        Product product,
        List<Recommendation> recommendations,
        List<Review> reviews,
        String serviceAddress
    ) {
        var productId = product.getProductId();
        var name = product.getName();
        var weight = product.getWeight();

        var recommendationSummaries = recommendations == null ? null : recommendations.stream()
            .map(r -> new RecommendationSummary(r.getRecommendationId(), r.getAuthor(), r.getRate(), r.getContent()))
            .toList();
        var reviewSummaries = reviews == null ? null : reviews.stream()
            .map(r -> new ReviewSummary(r.getReviewId(), r.getAuthor(), r.getSubject(), r.getContent()))
            .toList();
        var serviceAddresses = new ServiceAddresses(
            serviceAddress,
            product.getServiceAddress(),
            (recommendations == null || recommendations.isEmpty()) ? "" : recommendations.get(0).getServiceAddress(),
            (reviews == null || reviews.isEmpty()) ? "" : reviews.get(0).getServiceAddress()
        );

        return new ProductAggregate(productId, name, weight, recommendationSummaries, reviewSummaries, serviceAddresses);
    }
}
