package com.yoichitgy.microservices.core.review.services;

import java.util.List;

import com.yoichitgy.api.core.review.Review;
import com.yoichitgy.api.core.review.ReviewService;
import com.yoichitgy.api.exceptions.InvalidInputException;
import com.yoichitgy.microservices.core.review.persistence.ReviewRepository;
import com.yoichitgy.util.http.ServiceUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReviewServiceImpl implements ReviewService {
    private static final Logger LOG = LoggerFactory.getLogger(ReviewServiceImpl.class);

    private final ReviewRepository repository;
    private final ReviewMapper mapper;
    private final ServiceUtil serviceUtil;

    @Autowired
    public ReviewServiceImpl(ReviewRepository repository, ReviewMapper mapper, ServiceUtil serviceUtil) {
        this.repository = repository;
        this.mapper = mapper;
        this.serviceUtil = serviceUtil;
    }

    @Override
    public Review createReview(Review body) {
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
    public List<Review> getReviews(int productId) {
        if (productId < 1) {
            throw new InvalidInputException("Invalid productId: " + productId);
        }
        
        var entities = repository.findByProductId(productId);
        var response = mapper.entityListToApiList(entities);
        var address = serviceUtil.getServiceAddress();
        response.forEach(e -> e.setServiceAddress(address));

        LOG.debug("getReviews: response size: {}", response.size());
        return response;
    }

    @Override
    public void deleteReviews(int productId) {
        LOG.debug("deleteReviews: tries to delete reviews for the product with productId: {}", productId);
        repository.deleteAll(repository.findByProductId(productId));
    }
}
