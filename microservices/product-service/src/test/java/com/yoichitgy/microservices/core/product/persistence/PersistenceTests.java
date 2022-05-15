package com.yoichitgy.microservices.core.product.persistence;

import static java.util.stream.IntStream.rangeClosed;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.data.domain.Sort.Direction.ASC;

import com.yoichitgy.microservices.core.product.ContainerTestBase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@DataMongoTest(excludeAutoConfiguration = EmbeddedMongoAutoConfiguration.class)
class PersistenceTests extends ContainerTestBase {
    @Autowired
    private ProductRepository repository;

    private ProductEntity savedEntity;

    @BeforeEach
    void setupDb() {
        repository.deleteAll();

        var entity = new ProductEntity(1, "n", 1);
        savedEntity = repository.save(entity);
    }

    @Test
    void create() {
        var newEntity = new ProductEntity(2, "n", 2);
        repository.save(newEntity);

        var foundEntity = repository.findById(newEntity.getId()).get();
        assertEquals(newEntity, foundEntity);
        assertEquals(2, repository.count());
    }

    @Test
    void update() {
        savedEntity.setName("n2");
        repository.save(savedEntity);

        var foundEntity = repository.findById(savedEntity.getId()).get();
        assertEquals(1, foundEntity.getVersion());
        assertEquals("n2", foundEntity.getName());
    }

    @Test
    void delete() {
        repository.delete(savedEntity);
        assertFalse(repository.existsById(savedEntity.getId()));
    }

    @Test
    void getByProductId() {
        var entity = repository.findByProductId(savedEntity.getProductId());

        assertTrue(entity.isPresent());
        assertEquals(savedEntity, entity.get());
    }

    @Test
    void duplicateError() {
        assertThrows(DuplicateKeyException.class, () -> {
            var entity = new ProductEntity(savedEntity.getProductId(), "n", 1);
            repository.save(entity);
            System.out.println("repository.count = " + repository.count());
        });
    }

    @Test
    void optimisticLockError() {
        var entity1 = repository.findById(savedEntity.getId()).get();
        var entity2 = repository.findById(savedEntity.getId()).get();

        entity1.setName("n1");
        repository.save(entity1);

        assertThrows(OptimisticLockingFailureException.class, () -> {
            entity2.setName("n2");
            repository.save(entity2);
        }); 

        ProductEntity updatedEntity = repository.findById(savedEntity.getId()).get();
        assertEquals(1, updatedEntity.getVersion());
        assertEquals("n1", updatedEntity.getName());
    }

    @Test
    void paging() {
        repository.deleteAll();

        var newProducts = rangeClosed(1001, 1010)
            .mapToObj(i -> new ProductEntity(i, "name " + i, i))
            .toList();
        repository.saveAll(newProducts);

        Pageable nextPage = PageRequest.of(0, 4, ASC, "productId");
        nextPage = testNextPage(nextPage, "[1001, 1002, 1003, 1004]", true);
        nextPage = testNextPage(nextPage, "[1005, 1006, 1007, 1008]", true);
        nextPage = testNextPage(nextPage, "[1009, 1010]", false);
    }

    private Pageable testNextPage(Pageable nextPage, String expectedProductIds, boolean expectsNextPage) {
        var productPage = repository.findAll(nextPage);
        assertEquals(
            expectedProductIds,
            productPage.getContent().stream().map(p -> p.getProductId()).toList().toString()
        );
        assertEquals(expectsNextPage, productPage.hasNext());
        return productPage.nextPageable();
    }
}
