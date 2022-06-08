package com.yoichitgy.microservices.core.review.services;

import static java.util.logging.Level.FINE;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RestController;

import com.yoichitgy.api.core.review.Review;
import com.yoichitgy.api.core.review.ReviewService;
import com.yoichitgy.api.exceptions.InvalidInputException;
import com.yoichitgy.microservices.core.review.persistence.ReviewRepository;
import com.yoichitgy.util.http.ServiceUtil;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@RestController
public class ReviewServiceImpl implements ReviewService {
    private static final Logger LOG = LoggerFactory.getLogger(ReviewServiceImpl.class);

    private final Scheduler jdbcScheduler;
    private final ReviewRepository repository;
    private final ReviewMapper mapper;
    private final ServiceUtil serviceUtil;

    @Autowired
    public ReviewServiceImpl(
        @Qualifier("jdbcScheduler") Scheduler jdbcScheduler,
        ReviewRepository repository,
        ReviewMapper mapper,
        ServiceUtil serviceUtil
    ) {
        this.jdbcScheduler = jdbcScheduler;
        this.repository = repository;
        this.mapper = mapper;
        this.serviceUtil = serviceUtil;
    }

    @Override
    public Mono<Review> createReview(Review body) {
        int productId = body.getProductId();
        if (productId < 1) {
            throw new InvalidInputException("Invalid productId: " + productId);
        }
        return Mono.fromCallable(() -> _createReview(body))
            .subscribeOn(jdbcScheduler);
    }

    private Review _createReview(Review body) {
        try {
            var entity = mapper.apiToEntity(body);
            var newEntity = repository.save(entity);

            LOG.debug("createReview: created a review entity: {}/{}", body.getProductId(), body.getReviewId());
            return mapper.entityToApi(newEntity);
        } catch (DataIntegrityViolationException ex) {
            var msg = String.format(
                "Duplicate key, productId: %d, reviewId: %d",
                body.getProductId(),
                body.getReviewId())
            ;
            throw new InvalidInputException(msg);
        }
    }

    @Override
    public Flux<Review> getReviews(HttpHeaders headers, int productId) {
        if (productId < 1) {
            throw new InvalidInputException("Invalid productId: " + productId);
        }
        LOG.info("Will get review for product with id={}", productId);
       
        return Mono.fromCallable(() -> _getReviews(productId))
            .flatMapMany(Flux::fromIterable)
            .log(LOG.getName(), FINE)
            .subscribeOn(jdbcScheduler);
    }

    private List<Review> _getReviews(int productId) {
        var entities = repository.findByProductId(productId);
        var response = mapper.entityListToApiList(entities);
        var address = serviceUtil.getServiceAddress();
        response.forEach(e -> e.setServiceAddress(address));

        LOG.debug("Response size: {}", response.size());
        return response;
    }

    @Override
    public Mono<Void>deleteReviews(int productId) {
        if (productId < 1) {
            throw new InvalidInputException("Invalid productId: " + productId);
        }
        return Mono.fromRunnable(() -> _deleteReviews(productId))
            .subscribeOn(jdbcScheduler)
            .then();
    }

    private void _deleteReviews(int productId) {
        LOG.debug("deleteReviews: tries to delete reviews for the product with productId: {}", productId);
        repository.deleteAll(repository.findByProductId(productId));
    }
}
