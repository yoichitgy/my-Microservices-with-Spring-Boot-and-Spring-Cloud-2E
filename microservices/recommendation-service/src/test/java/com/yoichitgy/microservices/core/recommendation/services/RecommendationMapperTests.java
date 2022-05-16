package com.yoichitgy.microservices.core.recommendation.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import com.yoichitgy.api.core.recommendation.Recommendation;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class RecommendationMapperTests {
    private RecommendationMapper mapper = Mappers.getMapper(RecommendationMapper.class);

    @Test
    void mapperTest() {
        var api = new Recommendation(1, 2, "a", 4, "C", "adr");
        var entity = mapper.apiToEntity(api);    
        assertEquals(api.getProductId(), entity.getProductId());
        assertEquals(api.getRecommendationId(), entity.getRecommendationId());
        assertEquals(api.getAuthor(), entity.getAuthor());
        assertEquals(api.getRate(), entity.getRating());
        assertEquals(api.getContent(), entity.getContent());
    
        Recommendation apiBack = mapper.entityToApi(entity);    
        assertEquals(api.getProductId(), apiBack.getProductId());
        assertEquals(api.getRecommendationId(), apiBack.getRecommendationId());
        assertEquals(api.getAuthor(), apiBack.getAuthor());
        assertEquals(api.getRate(), apiBack.getRate());
        assertEquals(api.getContent(), apiBack.getContent());
        assertNull(apiBack.getServiceAddress());
    }

    @Test
    void mapperListTest() {
        var api = new Recommendation(1, 2, "a", 4, "C", "adr");
        var apiList = List.of(api);
    
        var entities = mapper.apiListToEntityList(apiList);
        assertEquals(apiList.size(), entities.size());
    
        var entity = entities.get(0);
        assertEquals(api.getProductId(), entity.getProductId());
        assertEquals(api.getRecommendationId(), entity.getRecommendationId());
        assertEquals(api.getAuthor(), entity.getAuthor());
        assertEquals(api.getRate(), entity.getRating());
        assertEquals(api.getContent(), entity.getContent());
    
        var apiListBack = mapper.entityListToApiList(entities);
        assertEquals(apiList.size(), apiListBack.size());
    
        Recommendation api2 = apiListBack.get(0);
        assertEquals(api.getProductId(), api2.getProductId());
        assertEquals(api.getRecommendationId(), api2.getRecommendationId());
        assertEquals(api.getAuthor(), api2.getAuthor());
        assertEquals(api.getRate(), api2.getRate());
        assertEquals(api.getContent(), api2.getContent());
        assertNull(api2.getServiceAddress());
    }
}
