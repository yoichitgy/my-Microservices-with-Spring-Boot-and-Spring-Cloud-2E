package com.yoichitgy.api.core.product;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class Product {
    private final int productId;
    private final String name;
    private final int weight;
    private final String serviceAddress;
}
