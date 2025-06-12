// src/main/java/com/project/product_service/dto/StockDto.java
package com.project.product_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockDto {
    private Long productId;
    private int quantity;
    private int reorderLevel;
    private boolean lowStock; // This is often a derived property in the Stock entity
}