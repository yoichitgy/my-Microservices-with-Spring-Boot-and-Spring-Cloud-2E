package com.yoichitgy.microservices.core.product.services;

import static java.util.logging.Level.FINE;

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

import reactor.core.publisher.Mono;

@RestController
public class ProductServiceImpl implements ProductService {
    private static final Logger LOG = LoggerFactory.getLogger(ProductServiceImpl.class);

    private final ProductRepository repository;
    private final ProductMapper mapper;
    private final ServiceUtil serviceUtil;

    @Autowired
    public ProductServiceImpl(ProductRepository repository, ProductMapper mapper, ServiceUtil serviceUtil) {
        this.repository = repository;
        this.mapper = mapper;
        this.serviceUtil = serviceUtil;
    }

    @Override
    public Mono<Product> createProduct(Product body) {
        int productId = body.getProductId();
        if (productId < 1) {
            throw new InvalidInputException("Invalid productId: " + productId);
        }
            
        var entity = mapper.apiToEntity(body);
        return repository.save(entity)
            .log(LOG.getName(), FINE)
            .onErrorMap(
                DuplicateKeyException.class,
                ex -> new InvalidInputException("Duplicate key, productId: " + productId)
            )
            .map(e -> mapper.entityToApi(e));
    }

    @Override
    public Mono<Product> getProduct(int productId) {
        if (productId < 1) {
            throw new InvalidInputException("Invalid productId: " + productId);
        }
        LOG.debug("Will get product info for id={}", productId);

        return repository.findByProductId(productId)
            .switchIfEmpty(
                Mono.error(new NotFoundException("No product found for productId: " + productId))
            )
            .log(LOG.getName(), FINE)
            .map(e -> {
                var api = mapper.entityToApi(e);
                api.setServiceAddress(serviceUtil.getServiceAddress());
                return api;
            });
    }

    @Override
    public Mono<Void> deleteProduct(int productId) {
        if (productId < 1) {
            throw new InvalidInputException("Invalid productId: " + productId);
        }
        LOG.debug("deleteProduct: tries to delete an entity with productId: {}", productId);

        return repository.findByProductId(productId)
            .log(LOG.getName(), FINE)
            .flatMap(e -> repository.delete(e));
    }
}
