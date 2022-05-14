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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductCompositeServiceImpl implements ProductCompositeService {
    private final ServiceUtil serviceUtil;
    private ProductCompositeIntegration integration;
    
    @Autowired
    public ProductCompositeServiceImpl(ServiceUtil serviceUtil, ProductCompositeIntegration integration) {
        this.serviceUtil = serviceUtil;
        this.integration = integration;
    }

    @Override
    public ProductAggregate getProduct(int productId) {
        var product = integration.getProduct(productId);
        if (product == null) {
            throw new NotFoundException("No product found for productId: " + productId);
        }

        var recommendations = integration.getRecommendations(productId);
        var reviews = integration.getReviews(productId);
        return createProductAggregate(product, recommendations, reviews, serviceUtil.getServiceAddress());
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
            .map(r -> new RecommendationSummary(r.getRecommendationId(), r.getAuthor(), r.getRate()))
            .toList();
        var reviewSummaries = reviews == null ? null : reviews.stream()
            .map(r -> new ReviewSummary(r.getReviewId(), r.getAuthor(), r.getSubject()))
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
