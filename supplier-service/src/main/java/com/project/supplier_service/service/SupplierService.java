package com.project.supplier_service.service;

import com.project.supplier_service.dto.SupplierResponseDto;
import com.project.supplier_service.model.Supplier;

import java.util.List;
import java.util.Optional;

public interface SupplierService {
    Supplier createSupplier(Supplier supplier);
    Supplier updateSupplier(Long supplierId, Supplier supplier);
    void deleteSupplier(Long supplierId);
    Optional<SupplierResponseDto> getSupplierById(Long supplierId); // Changed return type
    List<SupplierResponseDto> getAllSuppliers(); // Changed return type

    Long getProductsSuppliedCountBySupplier(Long supplierId);
}