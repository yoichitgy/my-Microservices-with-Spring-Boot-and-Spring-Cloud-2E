package com.yoichitgy.microservices.composite.product.services;

import static com.yoichitgy.microservices.composite.product.services.IsSameEvent.sameEventExceptCreatedAt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yoichitgy.api.core.product.Product;
import com.yoichitgy.api.event.Event;
import com.yoichitgy.api.event.Event.Type;

import org.junit.jupiter.api.Test;

class IsSameEventTests {
    ObjectMapper mapper = new ObjectMapper();

    @Test
    void testEventObjectCompare() throws JsonProcessingException {
      // Event #1 and #2 are the same event, but occurs as different times
      // Event #3 and #4 are different events
      var event1 = new Event<Integer, Product>(Type.CREATE, 1, new Product(1, "name", 1, null));
      var event2 = new Event<Integer, Product>(Type.CREATE, 1, new Product(1, "name", 1, null));
      var event3 = new Event<Integer, Product>(Type.DELETE, 1, null);
      var event4 = new Event<Integer, Product>(Type.CREATE, 1, new Product(2, "name", 1, null));
  
      String event1Json = mapper.writeValueAsString(event1);
      assertThat(event1Json, is(sameEventExceptCreatedAt(event2)));
      assertThat(event1Json, not(sameEventExceptCreatedAt(event3)));
      assertThat(event1Json, not(sameEventExceptCreatedAt(event4)));
    }
}
