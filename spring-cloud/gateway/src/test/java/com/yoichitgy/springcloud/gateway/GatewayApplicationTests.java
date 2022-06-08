package com.yoichitgy.springcloud.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

@SpringBootTest(
	webEnvironment = WebEnvironment.RANDOM_PORT,
	properties = {
		"spring.security.oauth2.resourceserver.jwt.jwk-set-uri=some-url"
	}
)
class GatewayApplicationTests {

	@Test
	void contextLoads() {
	}

}
