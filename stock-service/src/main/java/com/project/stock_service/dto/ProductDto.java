// src/main/java/com/project/stock_service/dto/ProductDto.java
package com.project.stock_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// This DTO should match the structure of what your Product Service returns for a single product.
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDto {
    private Long id;
    private String name;
    private String description;
    private double price;
    private Integer quantity; // This might be null or not directly applicable if stock is separate
    private String imageUrl;
}