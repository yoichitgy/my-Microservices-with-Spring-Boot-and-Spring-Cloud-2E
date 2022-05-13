package com.yoichitgy.api.composite.product;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class RecommendationSummary {
    private final int recommendationid;
    private final String author;
    private final int rate;
}
