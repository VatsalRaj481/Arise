package com.project.report_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockDto {
    private Long productId;
    private int quantity;
    private int reorderLevel;
    private boolean isLowStock;
}
