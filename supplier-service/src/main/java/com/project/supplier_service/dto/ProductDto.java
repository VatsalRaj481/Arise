package com.project.supplier_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// This DTO defines the structure of product data received from the Product Service.
// It should match the relevant fields of your Product entity/response from product-service.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {
    private Long id;
    private String name;
    private String description;
    private double price;
    private Integer quantity; // Assuming ProductDto might have quantity
    private String imageUrl; // Assuming ProductDto might have imageUrl
    // Add any other fields you need from the Product Service's Product entity
}