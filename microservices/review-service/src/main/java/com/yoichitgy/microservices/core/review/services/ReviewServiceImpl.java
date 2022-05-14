package com.yoichitgy.microservices.core.review.services;

import java.util.List;

import com.yoichitgy.api.core.review.Review;
import com.yoichitgy.api.core.review.ReviewService;
import com.yoichitgy.api.exceptions.InvalidInputException;
import com.yoichitgy.util.http.ServiceUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReviewServiceImpl implements ReviewService {
    private static final Logger LOG = LoggerFactory.getLogger(ReviewServiceImpl.class);

    private final ServiceUtil serviceUtil;

    @Autowired
    public ReviewServiceImpl(ServiceUtil serviceUtil) {
        this.serviceUtil = serviceUtil;
    }

    @Override
    public List<Review> getReviews(int productId) {
        if (productId < 1) {
            throw new InvalidInputException("Invalid productId: " + productId);
        }
        if (productId == 213) {
            LOG.debug("No reviews found for productId: {}", productId);
            return List.of();
        }

        var address = serviceUtil.getServiceAddress();
        var list = List.of(
            new Review(productId, 1, "Author 1", "Subject 1", "Content 1", address),
            new Review(productId, 2, "Author 2", "Subject 2", "Content 2", address),
            new Review(productId, 3, "Author 3", "Subject 3", "Content 3", address)
        );

        LOG.debug("/review response size: {}", list.size());
        return list;
    }
}
