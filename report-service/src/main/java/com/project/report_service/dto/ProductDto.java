package com.project.report_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {
    private Long id;
    private String name;
    private String description;
    private Double price;
    private Integer quantity; // This is the product's own stock, not order quantity
    private String imageUrl;
    // Add supplierId if product entity has it and you need it for reporting
    private Long supplierId;
}
