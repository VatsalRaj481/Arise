package com.project.product_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequestDto {
    // Product details
    private String name;
    private String description;
    private double price;
    private String imageUrl; // This might be a base64 string or a path depending on your image handling

    // Stock details for initial creation/update
    private Integer initialStockQuantity; // Use Integer to allow null (optional for updates)
    private Integer reorderLevel;        // Use Integer to allow null (optional for updates)
}