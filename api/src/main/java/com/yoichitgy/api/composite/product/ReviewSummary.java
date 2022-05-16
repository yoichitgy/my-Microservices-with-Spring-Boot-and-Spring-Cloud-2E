package com.yoichitgy.api.composite.product;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class ReviewSummary {
    private final int reviewId;
    private final String author;
    private final String subject;
    private final String content;
}
