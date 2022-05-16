package com.yoichitgy.microservices.core.product.services;

import com.yoichitgy.api.core.product.Product;
import com.yoichitgy.api.core.product.ProductService;
import com.yoichitgy.api.exceptions.InvalidInputException;
import com.yoichitgy.api.exceptions.NotFoundException;
import com.yoichitgy.microservices.core.product.persistence.ProductRepository;
import com.yoichitgy.util.http.ServiceUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductServiceImpl implements ProductService {
    private static final Logger LOG = LoggerFactory.getLogger(ProductServiceImpl.class);

    private final ProductRepository repository;
    private final ProductMapper mapper;
    private final ServiceUtil ServiceUtil;

    @Autowired
    public ProductServiceImpl(ProductRepository repository, ProductMapper mapper, ServiceUtil serviceUtil) {
        this.repository = repository;
        this.mapper = mapper;
        this.ServiceUtil = serviceUtil;
    }

    @Override
    public Product createProduct(Product body) {
        int productId = body.getProductId();
        if (productId < 1) {
            throw new InvalidInputException("Invalid productId: " + productId);
        }
            
        try {
            var entity = mapper.apiToEntity(body);
            var newEntity = repository.save(entity);

            LOG.debug("createProduct: entity created for productId: {}", productId);
            return mapper.entityToApi(newEntity);
        } catch (DuplicateKeyException ex) {
            throw new InvalidInputException("Duplicate key, productId: " + productId);
        }
    }

    @Override
    public Product getProduct(int productId) {
        if (productId < 1) {
            throw new InvalidInputException("Invalid productId: " + productId);
        }
        
        var entity = repository.findByProductId(productId)
            .orElseThrow(() -> new NotFoundException("No product found for productId: " + productId));
        var response = mapper.entityToApi(entity);
        response.setServiceAddress(ServiceUtil.getServiceAddress());

        LOG.debug("getProduct: found productId: {}", response.getProductId());
        return response;
    }

    @Override
    public void deleteProduct(int productId) {
        LOG.debug("deleteProduct: tries to delete an entity with productId: {}", productId);
        repository.findByProductId(productId).ifPresent(e -> repository.delete(e));
    }
}
