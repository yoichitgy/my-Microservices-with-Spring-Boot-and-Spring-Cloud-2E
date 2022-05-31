package com.yoichitgy.microservices.core.recommendation;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

@SpringBootTest(
	webEnvironment = WebEnvironment.RANDOM_PORT,
	properties = {
		"eureka.client.enabled=false",
		"spring.sleuth.mongodb.enabled=false",
		"spring.cloud.config.enabled=false"
	}
)
class RecommendationServiceApplicationTests extends ContainerTestBase {

	@Test
	void contextLoads() {
	}

}
