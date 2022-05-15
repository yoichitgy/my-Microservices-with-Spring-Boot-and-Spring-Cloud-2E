package com.yoichitgy.api.core.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class Product {
    private int productId;
    private String name;
    private int weight;
    private String serviceAddress;
}
