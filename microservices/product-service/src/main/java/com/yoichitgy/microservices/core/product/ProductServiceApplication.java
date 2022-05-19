package com.yoichitgy.microservices.core.product;

import com.yoichitgy.microservices.core.product.persistence.ProductEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;

@SpringBootApplication
@ComponentScan("com.yoichitgy")
public class ProductServiceApplication {
	private static final Logger LOG = LoggerFactory.getLogger(ProductServiceApplication.class);

	public static void main(String[] args) {
		var ctx = SpringApplication.run(ProductServiceApplication.class, args);

		var mongoHost = ctx.getEnvironment().getProperty("spring.data.mongodb.host");
		var mongoPort = ctx.getEnvironment().getProperty("spring.data.mongodb.port");
		LOG.info(String.format("Connected to MongoDB: %s:%s", mongoHost, mongoPort));
	}

	@Autowired
	ReactiveMongoOperations mongoTemplate;

	@EventListener(ContextRefreshedEvent.class)
	public void initIndicesAfterStartup() {
		var mappingContext = mongoTemplate.getConverter().getMappingContext();
		var resolver = new MongoPersistentEntityIndexResolver(mappingContext);
		var indexOps = mongoTemplate.indexOps(ProductEntity.class);
		resolver.resolveIndexFor(ProductEntity.class).forEach(e -> indexOps.ensureIndex(e).block());
	}
}
