package com.yoichitgy.microservices.core.recommendation;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"eureka.client.enabled=false"})
class RecommendationServiceApplicationTests extends ContainerTestBase {

	@Test
	void contextLoads() {
	}

}
