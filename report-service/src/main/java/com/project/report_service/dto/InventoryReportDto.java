package com.project.report_service.dto; // Changed package to com.project.report_service.dto for consistency

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Lombok annotation for Getters, Setters, toString, equals, hashCode
@NoArgsConstructor // Lombok annotation for no-arg constructor
@AllArgsConstructor // Lombok annotation for all-arg constructor
public class InventoryReportDto {
    private Long productId;
    private String productName;
    private Integer initialStock;
    private Integer stockAdded;
    private Integer stockRemoved;
    private Integer finalStock;
    private Integer reorderLevel;
    private Boolean isLowStock;
}
