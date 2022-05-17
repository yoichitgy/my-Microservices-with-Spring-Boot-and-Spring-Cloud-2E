package com.yoichitgy.microservices.core.recommendation.services;

import java.util.function.Consumer;

import com.yoichitgy.api.core.recommendation.Recommendation;
import com.yoichitgy.api.core.recommendation.RecommendationService;
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

    private final RecommendationService recommendationService;

    @Autowired
    public MessageProcessorConfiguration(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @Bean
    public Consumer<Event<Integer, Recommendation>> messageProcessor() {
        return event -> {
            LOG.info("Process message created at {}...", event.getEventCreatedAt());

            switch (event.getEventType()) {
                case CREATE:
                    var recommendation = event.getData();
                    LOG.info(
                        "Create recommendation with ID: {}/{}",
                        recommendation.getProductId(),
                        recommendation.getRecommendationId()
                    );
                    recommendationService.createRecommendation(recommendation).block();
                    break;
                case DELETE:
                    var productId = event.getKey();
                    LOG.info("Delete recommendations with ProductID: {}", productId);
                    recommendationService.deleteRecommendations(productId).block();
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
