package com.yoichitgy.microservices.core.review.services;

import java.util.function.Consumer;

import com.yoichitgy.api.core.review.Review;
import com.yoichitgy.api.core.review.ReviewService;
import com.yoichitgy.api.event.Event;
import com.yoichitgy.api.exceptions.EventProcessingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessageProcessorConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(MessageProcessorConfiguration.class);

    private final ReviewService reviewService;
  
    @Autowired
    public MessageProcessorConfiguration(ReviewService reviewService) {
        this.reviewService = reviewService;
    }
  
    @Bean
    public Consumer<Event<Integer, Review>> messageProcessor() {
        return event -> {
            LOG.info("Process message created at {}...", event.getEventCreatedAt());
    
            switch (event.getEventType()) {    
                case CREATE:
                    var review = event.getData();
                    LOG.info("Create review with ID: {}/{}", review.getProductId(), review.getReviewId());
                    reviewService.createReview(review).block();
                    break;
                case DELETE:
                    int productId = event.getKey();
                    LOG.info("Delete reviews with ProductID: {}", productId);
                    reviewService.deleteReviews(productId).block();
                    break;
                default:
                    var errorMessage = "Incorrect event type: " + event.getEventType() + ", expected a CREATE or DELETE event";
                    LOG.warn(errorMessage);
                    throw new EventProcessingException(errorMessage);
            }
            LOG.info("Message processing done!");
        };
    }
}
