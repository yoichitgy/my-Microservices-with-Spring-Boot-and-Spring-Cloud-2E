package com.yoichitgy.microservices.core.review.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;

import com.yoichitgy.api.core.review.Review;

import org.junit.Test;
import org.mapstruct.factory.Mappers;

class ReviewMapperTests {
    private ReviewMapper mapper = Mappers.getMapper(ReviewMapper.class);

    @Test
    void mapperTests() {
        var api = new Review(1, 2, "a", "s", "C", "adr");
        var entity = mapper.apiToEntity(api);
        assertEquals(api.getProductId(), entity.getProductId());
        assertEquals(api.getReviewId(), entity.getReviewId());
        assertEquals(api.getAuthor(), entity.getAuthor());
        assertEquals(api.getSubject(), entity.getSubject());
        assertEquals(api.getContent(), entity.getContent());

        Review apiBack = mapper.entityToApi(entity);
        assertEquals(api.getProductId(), apiBack.getProductId());
        assertEquals(api.getReviewId(), apiBack.getReviewId());
        assertEquals(api.getAuthor(), apiBack.getAuthor());
        assertEquals(api.getSubject(), apiBack.getSubject());
        assertEquals(api.getContent(), apiBack.getContent());
        assertNull(apiBack.getServiceAddress());
    }
  
    @Test
    void mapperListTests() {
        var api = new Review(1, 2, "a", "s", "C", "adr");
        var apiList = List.of(api);

        var entities = mapper.apiListToEntityList(apiList);
        assertEquals(apiList.size(), entities.size());

        var entity = entities.get(0);
        assertEquals(api.getProductId(), entity.getProductId());
        assertEquals(api.getReviewId(), entity.getReviewId());
        assertEquals(api.getAuthor(), entity.getAuthor());
        assertEquals(api.getSubject(), entity.getSubject());
        assertEquals(api.getContent(), entity.getContent());

        List<Review> apiListBack = mapper.entityListToApiList(entities);
        assertEquals(apiList.size(), apiListBack.size());

        Review apiBack = apiListBack.get(0);
        assertEquals(api.getProductId(), apiBack.getProductId());
        assertEquals(api.getReviewId(), apiBack.getReviewId());
        assertEquals(api.getAuthor(), apiBack.getAuthor());
        assertEquals(api.getSubject(), apiBack.getSubject());
        assertEquals(api.getContent(), apiBack.getContent());
        assertNull(apiBack.getServiceAddress());
    }    
}
