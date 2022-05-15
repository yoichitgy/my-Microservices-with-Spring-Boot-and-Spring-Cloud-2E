package com.yoichitgy.microservices.core.recommendation.services;

import java.util.List;

import com.yoichitgy.api.core.recommendation.Recommendation;
import com.yoichitgy.microservices.core.recommendation.persistence.RecommendationEntity;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RecommendationMapper {
    @Mapping(target = "rate", source = "entity.rating")
    @Mapping(target = "serviceAddress", ignore = true)
    Recommendation entityToApi(RecommendationEntity entity);

    @Mapping(target = "rating", source = "api.rate")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    RecommendationEntity apiToEntity(Recommendation api);

    List<Recommendation> entityListToApiList(List<RecommendationEntity> entities);
    List<RecommendationEntity> apiListToEntityList(List<Recommendation> api);
}
