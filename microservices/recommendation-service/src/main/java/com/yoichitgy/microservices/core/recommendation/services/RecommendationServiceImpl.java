package com.yoichitgy.microservices.core.recommendation.services;

import static java.util.logging.Level.FINE;

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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
    public Mono<Recommendation> createRecommendation(Recommendation body) {
        int productId = body.getProductId();
        if (productId < 1) {
            throw new InvalidInputException("Invalid productId: " + productId);
        }

        var entity = mapper.apiToEntity(body);
        return repository.save(entity)
            .log(LOG.getName(), FINE)
            .onErrorMap(
                DuplicateKeyException.class,
                ex -> {
                    var msg = String.format("Duplicate key, productId: %d, recommendationId: %d", body.getProductId(), body.getRecommendationId());
                    return new InvalidInputException(msg);
                })
            .map(e -> mapper.entityToApi(e));
    }

    @Override
    public Flux<Recommendation> getRecommendations(int productId) {
        if (productId < 1) {
            throw new InvalidInputException("Invalid productId: " + productId);
        }
        LOG.info("Will get recommendations for product with id={}", productId);

        return repository.findByProductId(productId)
            .log(LOG.getName(), FINE)
            .map(e -> { 
                var api = mapper.entityToApi(e);
                api.setServiceAddress(serviceUtil.getServiceAddress());
                return api;
            });
    }

    @Override
    public Mono<Void> deleteRecommendations(int productId) {
        if (productId < 1) {
            throw new InvalidInputException("Invalid productId: " + productId);
        }
           
        LOG.debug("deleteRecommendations: tries to delete recommendations for the product with productId: {}", productId);
        return repository.deleteAll(repository.findByProductId(productId));
    }
}
