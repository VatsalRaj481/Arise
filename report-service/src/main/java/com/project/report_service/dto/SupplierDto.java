package com.project.report_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplierDto {
    private Long supplierId;
    private String name;
    private String contactInfo;
    private List<String> productsSupplied; // Assuming this is a list of product names or IDs
}
