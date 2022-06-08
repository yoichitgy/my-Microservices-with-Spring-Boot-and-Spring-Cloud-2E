package com.yoichitgy.microservices.core.product.persistence;

import com.yoichitgy.microservices.core.product.ContainerTestBase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;

import reactor.test.StepVerifier;

@DataMongoTest(
    excludeAutoConfiguration = EmbeddedMongoAutoConfiguration.class
)
class PersistenceTests extends ContainerTestBase {
    @Autowired
    private ProductRepository repository;

    private ProductEntity savedEntity;

    @BeforeEach
    void setupDb() {
        StepVerifier.create(repository.deleteAll()).verifyComplete();

        var entity = new ProductEntity(1, "n", 1);
        StepVerifier.create(repository.save(entity))
            .consumeNextWith(response -> savedEntity = response)
            .verifyComplete();
    }

    @Test
    void create() {
        var newEntity = new ProductEntity(2, "n", 2);
        StepVerifier.create(repository.save(newEntity))
            .expectNextCount(1)
            .verifyComplete();

        StepVerifier.create(repository.findByProductId(newEntity.getProductId()))
            .expectNext(newEntity)
            .verifyComplete();
        StepVerifier.create(repository.count())
            .expectNext(2L)
            .verifyComplete();
    }

    @Test
    void update() {
        savedEntity.setName("n2");
        StepVerifier.create(repository.save(savedEntity))
            .expectNextMatches(updated -> updated.getName().equals("n2"))
            .verifyComplete();
    
        StepVerifier.create(repository.findById(savedEntity.getId()))
            .expectNextMatches(found -> found.getVersion() == 1 && found.getName().equals("n2"))
            .verifyComplete();
        }

    @Test
    void delete() {
        StepVerifier.create(repository.delete(savedEntity)).verifyComplete();
        StepVerifier.create(repository.existsById(savedEntity.getId())).expectNext(false).verifyComplete();
    }

    @Test
    void getByProductId() {
        StepVerifier.create(repository.findByProductId(savedEntity.getProductId()))
            .expectNext(savedEntity)
            .verifyComplete();
    }

    @Test
    void duplicateError() {
        var entity = new ProductEntity(savedEntity.getProductId(), "n", 1);
        StepVerifier.create(repository.save(entity))
            .expectError(DuplicateKeyException.class)
            .verify();
    }

    @Test
    void optimisticLockError() {
        var entity1 = repository.findById(savedEntity.getId()).block();
        var entity2 = repository.findById(savedEntity.getId()).block();
    
        entity1.setName("n1");
        repository.save(entity1).block();
    
        StepVerifier.create(repository.save(entity2))
            .expectError(OptimisticLockingFailureException.class)
            .verify();
    
        StepVerifier.create(repository.findById(savedEntity.getId()))
            .expectNextMatches(found -> found.getVersion() == 1 && found.getName().equals("n1"))
            .verifyComplete();    
    }
}
