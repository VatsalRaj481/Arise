// src/main/java/com/project/product_service/dto/StockDto.java
package com.project.product_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// This DTO should mirror the structure of what your Stock Service returns for a single stock item.
// Based on your stock-service's Stock entity.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockDto {
    private Long productId;
    private int quantity;
    private int reorderLevel;
    private boolean lowStock; // This is often a derived property in the Stock entity
}