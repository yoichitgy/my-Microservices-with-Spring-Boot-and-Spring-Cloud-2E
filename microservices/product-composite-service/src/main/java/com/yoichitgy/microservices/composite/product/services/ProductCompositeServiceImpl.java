package com.yoichitgy.microservices.composite.product.services;

import java.util.List;

import com.yoichitgy.api.composite.product.ProductAggregate;
import com.yoichitgy.api.composite.product.ProductCompositeService;
import com.yoichitgy.api.composite.product.RecommendationSummary;
import com.yoichitgy.api.composite.product.ReviewSummary;
import com.yoichitgy.api.composite.product.ServiceAddresses;
import com.yoichitgy.api.core.product.Product;
import com.yoichitgy.api.core.recommendation.Recommendation;
import com.yoichitgy.api.core.review.Review;
import com.yoichitgy.api.exceptions.NotFoundException;
import com.yoichitgy.util.http.ServiceUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductCompositeServiceImpl implements ProductCompositeService {
    private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeServiceImpl.class);

    private final ServiceUtil serviceUtil;
    private ProductCompositeIntegration integration;
    
    @Autowired
    public ProductCompositeServiceImpl(ServiceUtil serviceUtil, ProductCompositeIntegration integration) {
        this.serviceUtil = serviceUtil;
        this.integration = integration;
    }

    @Override
    public void createProduct(ProductAggregate body) {
        try {
            int productId = body.getProductId();
            LOG.debug("createCompositeProduct: creates a new composite entity for productId: {}", productId);

            var product = new Product(productId, body.getName(), body.getWeight(), null);
            integration.createProduct(product);

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
                    integration.createRecommendation(recommendation);
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
                    integration.createReview(review);
                });
            }

            LOG.debug("createCompositeProduct: composite entities created for productId: {}", body.getProductId());
        } catch (RuntimeException ex) {
            LOG.warn("createCompositeProduct failed", ex);
            throw ex;
        }
    }

    @Override
    public ProductAggregate getProduct(int productId) {
        LOG.debug("getCompositeProduct: lookup a product aggregate for productId: {}", productId);

        var product = integration.getProduct(productId);
        if (product == null) {
            throw new NotFoundException("No product found for productId: " + productId);
        }

        var recommendations = integration.getRecommendations(productId);
        var reviews = integration.getReviews(productId);
        LOG.debug("getCompositeProduct: aggregate entity found for productId: {}", productId);

        return createProductAggregate(product, recommendations, reviews, serviceUtil.getServiceAddress());
    }

    @Override
    public void deleteProduct(int productId) {
        LOG.debug("deleteCompositeProduct: Deletes a product aggregate for productId: {}", productId);

        integration.deleteProduct(productId);
        integration.deleteRecommendations(productId);
        integration.deleteReviews(productId);

        LOG.debug("deleteCompositeProduct: aggregate entities deleted for productId: {}", productId);
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
