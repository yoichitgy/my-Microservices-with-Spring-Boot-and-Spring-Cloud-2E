package com.yoichitgy.microservices.composite.product;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    classes = {TestSecurityConfiguration.class},
    properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=",
        "spring.main.allow-bean-definition-overriding=true",
        "eureka.client.enabled=false"
    }
)
class ProductCompositeServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
