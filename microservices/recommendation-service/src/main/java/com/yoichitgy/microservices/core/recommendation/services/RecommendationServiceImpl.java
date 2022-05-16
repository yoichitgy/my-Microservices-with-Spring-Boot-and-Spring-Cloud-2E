package com.yoichitgy.microservices.core.recommendation.services;

import java.util.List;

import com.yoichitgy.api.core.recommendation.Recommendation;
import com.yoichitgy.api.core.recommendation.RecommendationService;
import com.yoichitgy.api.exceptions.InvalidInputException;
import com.yoichitgy.microservices.core.recommendation.persistence.RecommendationRepository;
import com.yoichitgy.util.http.ServiceUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RecommendationServiceImpl implements RecommendationService {
    private static final Logger LOG = LoggerFactory.getLogger(RecommendationServiceImpl.class);

    private final RecommendationRepository repository;
    private final RecommendationMapper mapper;
    private final ServiceUtil serviceUtil;
    
    @Autowired
    public RecommendationServiceImpl(
        RecommendationRepository repository,
        RecommendationMapper mapper,
        ServiceUtil serviceUtil
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.serviceUtil = serviceUtil;
    }

    @Override
    public Recommendation createRecommendation(Recommendation body) {
        try {
            var entity = mapper.apiToEntity(body);
            var newEntity = repository.save(entity);
        
            LOG.debug("createRecommendation: created a recommendation entity: {}/{}", body.getProductId(), body.getRecommendationId());
            return mapper.entityToApi(newEntity);
        } catch (DuplicateKeyException ex) {
            var msg = String.format("Duplicate key, productId: %d, recommendationId: %d", body.getProductId(), body.getRecommendationId());
            throw new InvalidInputException(msg);
        }
    }

    @Override
    public List<Recommendation> getRecommendations(int productId) {
        if (productId < 1) {
            throw new InvalidInputException("Invalid productId: " + productId);
        }

        var entities = repository.findByProductId(productId);
        var response = mapper.entityListToApiList(entities);
        var address = serviceUtil.getServiceAddress();
        response.forEach(e -> e.setServiceAddress(address));
        
        LOG.debug("getRecommendations: response size: {}", response.size());
        return response;
    }

    @Override
    public void deleteRecommendations(int productId) {
        LOG.debug("deleteRecommendations: tries to delete recommendations for the product with productId: {}", productId);
        repository.deleteAll(repository.findByProductId(productId));
    }
}
