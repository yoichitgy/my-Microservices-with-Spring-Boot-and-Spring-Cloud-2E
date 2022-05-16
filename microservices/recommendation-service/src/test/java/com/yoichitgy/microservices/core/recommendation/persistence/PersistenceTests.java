package com.yoichitgy.microservices.core.recommendation.persistence;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import com.yoichitgy.microservices.core.recommendation.ContainerTestBase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;

@DataMongoTest(excludeAutoConfiguration = EmbeddedMongoAutoConfiguration.class)
class PersistenceTests extends ContainerTestBase {
    @Autowired
    private RecommendationRepository repository;

    private RecommendationEntity savedEntity;

    @BeforeEach
    void setupDb() {
        repository.deleteAll();

        RecommendationEntity entity = new RecommendationEntity(1, 2, "a", 3, "c");
        savedEntity = repository.save(entity);
    }


    @Test
    void create() {
        var newEntity = new RecommendationEntity(1, 3, "a", 3, "c");
        repository.save(newEntity);

        var foundEntity = repository.findById(newEntity.getId()).get();
        assertEquals(newEntity, foundEntity);
        assertEquals(2, repository.count());
    }

    @Test
    void update() {
        savedEntity.setAuthor("a2");
        repository.save(savedEntity);

        var foundEntity = repository.findById(savedEntity.getId()).get();
        assertEquals(1, (int)foundEntity.getVersion());
        assertEquals("a2", foundEntity.getAuthor());
    }

    @Test
    void delete() {
        repository.delete(savedEntity);
        assertFalse(repository.existsById(savedEntity.getId()));
    }

    @Test
    void getByProductId() {
       var entityList = repository.findByProductId(savedEntity.getProductId());

        assertEquals(1, entityList.size());
        assertEquals(savedEntity, entityList.get(0));
    }

    @Test
    void duplicateError() {
        assertThrows(DuplicateKeyException.class, () -> {
            RecommendationEntity entity = new RecommendationEntity(1, 2, "a", 3, "c");
            repository.save(entity);
        });
    }

    @Test
    void optimisticLockError() {
        var entity1 = repository.findById(savedEntity.getId()).get();
        var entity2 = repository.findById(savedEntity.getId()).get();

        entity1.setAuthor("a1");
        repository.save(entity1);

        assertThrows(OptimisticLockingFailureException.class, () -> {
            entity2.setAuthor("a2");
            repository.save(entity2);
        });

        var updatedEntity = repository.findById(savedEntity.getId()).get();
        assertEquals(1, (int)updatedEntity.getVersion());
        assertEquals("a1", updatedEntity.getAuthor());
    }
}
