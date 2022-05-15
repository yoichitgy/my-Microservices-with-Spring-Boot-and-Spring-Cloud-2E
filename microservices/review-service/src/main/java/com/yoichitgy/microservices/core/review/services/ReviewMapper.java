package com.yoichitgy.microservices.core.review.services;

import java.util.List;

import com.yoichitgy.api.core.review.Review;
import com.yoichitgy.microservices.core.review.persistence.ReviewEntity;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewMapper {
    @Mapping(target = "serviceAddress", ignore = true)
    Review entityToApi(ReviewEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    ReviewEntity apiToEntity(Review api);

    List<Review> entityListToApiList(List<ReviewEntity> entities);
    List<ReviewEntity> apiListToEntityList(List<Review> api);
  }
