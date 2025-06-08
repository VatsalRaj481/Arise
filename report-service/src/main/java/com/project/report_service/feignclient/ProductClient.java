package com.project.report_service.feignclient;

import com.project.report_service.dto.ProductDto; // DTO for product data
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;
import java.util.Optional;

@FeignClient(name = "product-service", url = "${product-service.url:http://localhost:8081}")
public interface ProductClient {

    @GetMapping("/api/products")
    List<ProductDto> getAllProducts();

    @GetMapping("/api/products/{id}")
    Optional<ProductDto> getProductById(@PathVariable("id") Long id);
}
