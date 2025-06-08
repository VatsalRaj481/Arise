package com.project.product_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder // Useful for creating instances easily
public class ProductResponseDto {
    private Long id;
    private String name;
    private String description;
    private double price;
    // Removed stockQuantity from here as it will come from StockDto

    private String imageUrl;

    // --- NEW: Include Stock information ---
    private StockDto stockDetails; // Embed the stock information
    private String stockStatus; // e.g., "In Stock", "Low Stock", "Out of Stock" (derived)
}