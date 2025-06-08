package com.project.supplier_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

// This DTO is used to send the combined Supplier and Product data back to the client.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplierResponseDto {
    private Long supplierId;
    private String name;
    private String contactInfo;
    private List<ProductDto> suppliedProducts = new ArrayList<>(); // Now holds actual ProductDto objects
}