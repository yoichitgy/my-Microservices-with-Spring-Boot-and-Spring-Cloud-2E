package com.yoichitgy.microservices.core.review.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yoichitgy.microservices.core.review.ContainerTestBase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@AutoConfigureTestDatabase(replace = Replace.NONE)
class PersistenceTests extends ContainerTestBase {
    @Autowired
    private ReviewRepository repository;

    private ReviewEntity savedEntity;

    @BeforeEach
    void setupDb() {
        repository.deleteAll();

        var entity = new ReviewEntity(1, 2, "a", "s", "c");
        savedEntity = repository.save(entity);
    }

    @Test
    void create() {
        var newEntity = new ReviewEntity(1, 3, "a3", "s3", "c3");
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
        assertEquals(1, foundEntity.getVersion());
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
        assertThrows(DataIntegrityViolationException.class, () -> {
            var entity = new ReviewEntity(1, 2, "a", "s", "c");
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
        assertEquals(1, updatedEntity.getVersion());
        assertEquals("a1", updatedEntity.getAuthor());
    }
}
