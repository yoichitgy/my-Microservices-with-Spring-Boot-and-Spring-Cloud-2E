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
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
public class ProductCompositeServiceImpl implements ProductCompositeService {
    private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeServiceImpl.class);

    private final SecurityContext nullSecCtx = new SecurityContextImpl();

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

            monoList.add(getLogAuthorizationInMono());

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
    public Mono<ProductAggregate> getProduct(HttpHeaders requestHeaders, int productId, int delay, int faultPercent) {
        LOG.info("Will get composite product info for product.id={}", productId);

        var headers = getHeaders(requestHeaders, "X-group");
        return Mono.zip(
                getLogAuthorizationInMono(),
                integration.getProduct(headers, productId, delay, faultPercent),
                integration.getRecommendations(headers, productId).collectList(),
                integration.getReviews(headers, productId).collectList()
            ).map(values -> {
                var product = values.getT2();
                var recommendations = values.getT3();
                var reviews = values.getT4();
                var address = serviceUtil.getServiceAddress();
                return createProductAggregate(product, recommendations, reviews, address);
            }).doOnError(ex -> LOG.warn("getCompositeProduct failed: {}", ex.toString()))
            .log(LOG.getName(), Level.FINE);
    }

    private HttpHeaders getHeaders(HttpHeaders requestHeaders, String... headers) {
        LOG.trace("Will look for {} headers: {}", headers.length, headers);
        var h = new HttpHeaders();
        for (var header: headers) {
            var value = requestHeaders.get(header);
            if (value != null) {
                h.addAll(header, value);
            }
        }
        LOG.trace("Will transfer {}, headers: {}", h.size(), h);
        return h;
    }

    @Override
    public Mono<Void> deleteProduct(int productId) {
        LOG.debug("deleteCompositeProduct: Deletes a product aggregate for productId: {}", productId);

        try {
            return Mono.zip(
                    getLogAuthorizationInMono(),
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

    private Mono<SecurityContext> getLogAuthorizationInMono() {
        return getSecurityContextMono().doOnNext(sc -> logAuthorizationInfo(sc));
    }

    private Mono<SecurityContext> getSecurityContextMono() {
        return ReactiveSecurityContextHolder.getContext().defaultIfEmpty(nullSecCtx);
    }
    
    private void logAuthorizationInfo(SecurityContext sc) {
        if (sc == null
            || sc.getAuthentication() == null
            || !(sc.getAuthentication() instanceof JwtAuthenticationToken)
        ) {
            LOG.warn("No JWT based Authentication supplied, running tests are we?");            
            return;
        }

        Jwt jwtToken = ((JwtAuthenticationToken)sc.getAuthentication()).getToken();
        logAuthorizationInfo(jwtToken);
    }
    
    private void logAuthorizationInfo(Jwt jwt) {
        if (jwt == null) {
            LOG.warn("No JWT supplied, running tests are we?");
            return;
        }

        if (LOG.isDebugEnabled()) {
            var issuer = jwt.getIssuer();
            var audience = jwt.getAudience();
            var claims = jwt.getClaims();
            var subject = claims.get("sub");
            var scopes = claims.get("scope");
            var expires = claims.get("exp");

            LOG.debug("Authorization info: Subject: {}, scopes: {}, expires {}: issuer: {}, audience: {}",
                subject, scopes, expires, issuer, audience);
        }
    }
}
