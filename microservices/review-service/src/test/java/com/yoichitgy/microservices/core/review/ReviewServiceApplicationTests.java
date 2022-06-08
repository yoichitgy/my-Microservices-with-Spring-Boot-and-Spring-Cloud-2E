package com.yoichitgy.microservices.core.review;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

@SpringBootTest(
	webEnvironment = WebEnvironment.RANDOM_PORT
)
class ReviewServiceApplicationTests extends ContainerTestBase {

	@Test
	void contextLoads() {
	}

}
