package com.project.report_service.feignclient;

import com.project.report_service.dto.SupplierDto; // DTO for supplier data
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;
import java.util.Optional;

@FeignClient(name = "supplier-service", url = "${supplier-service.url:http://localhost:8089}")
public interface SupplierClient {

    @GetMapping("/api/suppliers")
    List<SupplierDto> getAllSuppliers();

    @GetMapping("/api/suppliers/{id}")
    Optional<SupplierDto> getSupplierById(@PathVariable("id") Long id);

    @GetMapping("/api/suppliers/products-supplied-count/{supplierId}")
    Long getProductsSuppliedCountBySupplier(@PathVariable("supplierId") Long supplierId);

}