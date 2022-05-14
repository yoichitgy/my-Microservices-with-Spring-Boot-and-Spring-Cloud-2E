package com.yoichitgy.api.composite.product;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class ServiceAddresses {
    private final String composite;
    private final String product;
    private final String recommendation;
    private final String review;
}
