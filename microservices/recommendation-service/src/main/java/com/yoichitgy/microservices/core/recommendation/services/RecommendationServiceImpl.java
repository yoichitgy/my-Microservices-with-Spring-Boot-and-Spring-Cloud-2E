package com.yoichitgy.microservices.core.recommendation.services;

import java.util.List;

import com.yoichitgy.api.core.recommendation.Recommendation;
import com.yoichitgy.api.core.recommendation.RecommendationService;
import com.yoichitgy.api.exceptions.InvalidInputException;
import com.yoichitgy.util.http.ServiceUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RecommendationServiceImpl implements RecommendationService {
    private static final Logger LOG = LoggerFactory.getLogger(RecommendationServiceImpl.class);

    private final ServiceUtil serviceUtil;
    
    @Autowired
    public RecommendationServiceImpl(ServiceUtil serviceUtil) {
        this.serviceUtil = serviceUtil;
    }

    @Override
    public List<Recommendation> getRecommendations(int productId) {
        if (productId < 1) {
            throw new InvalidInputException("Invalid productId: " + productId);
        }
        if (productId == 113) {
            LOG.debug("No recommendations found for productId: {}", productId);
            return List.of();
        }
        
        var address = serviceUtil.getServiceAddress();
        var list = List.of(
            new Recommendation(productId, 1, "Author 1", 1, "Content 1", address),
            new Recommendation(productId, 2, "Author 2", 1, "Content 2", address),
            new Recommendation(productId, 3, "Author 3", 1, "Content 3", address)
        );

        LOG.debug("/recommendation response size: {}", list.size());
        return list;
    }
}
