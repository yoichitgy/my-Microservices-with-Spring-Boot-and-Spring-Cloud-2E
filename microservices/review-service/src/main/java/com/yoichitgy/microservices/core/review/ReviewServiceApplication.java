package com.yoichitgy.microservices.core.review;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.yoichitgy")
public class ReviewServiceApplication {
	private static final Logger LOG = LoggerFactory.getLogger(ReviewServiceApplication.class);

	public static void main(String[] args) {
		var ctx = SpringApplication.run(ReviewServiceApplication.class, args);

		var mysqlUri = ctx.getEnvironment().getProperty("spring.datasource.url");
		LOG.info("Connected to MySQL: " + mysqlUri);
	}

}
