package com.project.order_service.feignclient;

import com.project.order_service.dto.ProductDto; // DTO for product data
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

@FeignClient(name = "product-service", url = "${product-service.url:http://localhost:8081}") // 'name' is Eureka service ID, 'url' is fallback
public interface ProductClient {

    @GetMapping("/api/products/{id}")
    Optional<ProductDto> getProductById(@PathVariable("id") Long id);
}
