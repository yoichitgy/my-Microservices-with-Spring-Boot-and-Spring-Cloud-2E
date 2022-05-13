package com.yoichitgy.api.composite.product;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class ReviewSummary {
    private final int reviewId;
    private final String author;
    private final String subject;
}
