package com.yoichitgy.microservices.core.product.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.yoichitgy.api.core.product.Product;

import org.junit.Test;
import org.mapstruct.factory.Mappers;

class ProductMapperTests {
    @Test
    void mapperTest() {
        var mapper = Mappers.getMapper(ProductMapper.class);

        var api = new Product(1, "n", 1, "sa");
        var entity = mapper.apiToEntity(api);
        assertEquals(api.getProductId(), entity.getProductId());
        assertEquals(api.getName(), entity.getName());
        assertEquals(api.getWeight(), entity.getWeight());
    
        Product apiBack = mapper.entityToApi(entity);
        assertEquals(api.getProductId(), apiBack.getProductId());
        assertEquals(api.getName(),      apiBack.getName());
        assertEquals(api.getWeight(),    apiBack.getWeight());
        assertNull(apiBack.getServiceAddress());
    }
}
