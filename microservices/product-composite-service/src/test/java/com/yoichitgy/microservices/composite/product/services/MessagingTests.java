package com.yoichitgy.microservices.composite.product.services;

import static com.yoichitgy.microservices.composite.product.services.IsSameEvent.sameEventExceptCreatedAt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import com.yoichitgy.api.composite.product.ProductAggregate;
import com.yoichitgy.api.composite.product.RecommendationSummary;
import com.yoichitgy.api.composite.product.ReviewSummary;
import com.yoichitgy.api.core.product.Product;
import com.yoichitgy.api.core.recommendation.Recommendation;
import com.yoichitgy.api.core.review.Review;
import com.yoichitgy.api.event.Event;
import com.yoichitgy.api.event.Event.Type;
import com.yoichitgy.microservices.composite.product.TestSecurityConfiguration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.messaging.Message;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Mono;

@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    classes = {TestSecurityConfiguration.class},
    properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=",
        "spring.main.allow-bean-definition-overriding=true",
        "eureka.client.enabled=false"
    }
)
@Import({TestChannelBinderConfiguration.class})
class MessagingTests {
    private static final Logger LOG = LoggerFactory.getLogger(MessagingTests.class);

    @Autowired
    private WebTestClient client;
    @Autowired
    private OutputDestination target;
  
    @BeforeEach
    void setUp() {
        purgeMessages("products");
        purgeMessages("recommendations");
        purgeMessages("reviews");
    }
   
    @Test
    void createCompositeProduct1() {
        var composite = new ProductAggregate(1, "name", 1, null, null, null);
        postAndVerifyProduct(composite, HttpStatus.ACCEPTED);
    
        final var productMessages = getMessages("products");
        final var recommendationMessages = getMessages("recommendations");
        final var reviewMessages = getMessages("reviews");
    
        assertEquals(1, productMessages.size());
        assertEquals(0, recommendationMessages.size());
        assertEquals(0, reviewMessages.size());    

        var expectedEvent = new Event<Integer, Product>(
            Type.CREATE,
            composite.getProductId(),
            new Product(composite.getProductId(), composite.getName(), composite.getWeight(), null));
        assertThat(productMessages.get(0), is(sameEventExceptCreatedAt(expectedEvent)));
    }

    @Test
    void createCompositeProduct2() {
        var recommendations = List.of(new RecommendationSummary(1, "a", 1, "c"));
        var reviews = List.of(new ReviewSummary(1, "a", "s", "c"));
        var composite = new ProductAggregate(1, "name", 1, recommendations, reviews, null);
        postAndVerifyProduct(composite, HttpStatus.ACCEPTED);

        final var productMessages = getMessages("products");
        final var recommendationMessages = getMessages("recommendations");
        final var reviewMessages = getMessages("reviews");

        assertEquals(1, productMessages.size());
        assertEquals(1, recommendationMessages.size());
        assertEquals(1, reviewMessages.size());

        var expectedProductEvent = new Event<Integer, Product>(
            Type.CREATE,
            composite.getProductId(),
            new Product(composite.getProductId(), composite.getName(), composite.getWeight(), null)
        );
        assertThat(productMessages.get(0), is(sameEventExceptCreatedAt(expectedProductEvent)));

        var rec = composite.getRecommendations().get(0);
        var expectedRecommendationEvent = new Event<Integer, Recommendation>(
            Type.CREATE,
            composite.getProductId(),
            new Recommendation(composite.getProductId(), rec.getRecommendationId(), rec.getAuthor(), rec.getRate(), rec.getContent(), null)
        );
        assertThat(recommendationMessages.get(0), is(sameEventExceptCreatedAt(expectedRecommendationEvent)));

        var rev = composite.getReviews().get(0);
        var expectedReviewEvent = new Event<Integer, Review>(
            Type.CREATE,
            composite.getProductId(),
            new Review(composite.getProductId(), rev.getReviewId(), rev.getAuthor(), rev.getSubject(), rev.getContent(), null)
        );
        assertThat(reviewMessages.get(0), is(sameEventExceptCreatedAt(expectedReviewEvent)));
    }

    @Test
    void deleteCompositeProduct() {
        deleteAndVerifyProduct(1, HttpStatus.ACCEPTED);

        final var productMessages = getMessages("products");
        final var recommendationMessages = getMessages("recommendations");
        final var reviewMessages = getMessages("reviews");

        assertEquals(1, productMessages.size());
        assertEquals(1, recommendationMessages.size());
        assertEquals(1, reviewMessages.size());

        assertThat(
            productMessages.get(0),
            is(sameEventExceptCreatedAt(new Event<Integer, Product>(Type.DELETE, 1, null)))
        );
        assertThat(
            recommendationMessages.get(0),
            is(sameEventExceptCreatedAt(new Event<Integer, Recommendation>(Type.DELETE, 1, null)))
        );
        assertThat(
            reviewMessages.get(0),
            is(sameEventExceptCreatedAt(new Event<Integer, Review>(Type.DELETE, 1, null)))
        );
    }

    private void purgeMessages(String bindingName) {
        getMessages(bindingName);
    }

    private List<String> getMessages(String bindingName) {
        List<String> messages = new ArrayList<String>();
        while (true) {
            var message = getMessage(bindingName);
            if (message == null) {
                break;
            }
            messages.add(new String(message.getPayload()));
        }
        return messages;
    }

    private Message<byte[]> getMessage(String bindingName) {
        try {
            return target.receive(0, bindingName);
        } catch (NullPointerException npe) {
            // If the messageQueues member variable in the target object contains no queues when the receive method is called, it will cause a NPE to be thrown.
            // So we catch the NPE here and return null to indicate that no messages were found.
            LOG.error("getMessage() received a NPE with binding = {}", bindingName);
            return null;
        }
    }

    private void postAndVerifyProduct(ProductAggregate compositeProduct, HttpStatus expectedStatus) {
        client.post()
            .uri("/product-composite")
            .body(Mono.just(compositeProduct), ProductAggregate.class)
            .exchange()
            .expectStatus().isEqualTo(expectedStatus);
      }
    
    private void deleteAndVerifyProduct(int productId, HttpStatus expectedStatus) {
        client.delete()
            .uri("/product-composite/" + productId)
            .exchange()
            .expectStatus().isEqualTo(expectedStatus);
    }
}
