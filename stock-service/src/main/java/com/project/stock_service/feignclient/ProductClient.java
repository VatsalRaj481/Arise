// src/main/java/com/project/stock_service/feignclient/ProductClient.java
package com.project.stock_service.feignclient;

import com.project.stock_service.dto.ProductDto; // You'll need to define this DTO
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

@FeignClient(name = "product-service", url = "${product-service.url:http://localhost:8081}") // Assuming product-service runs on 8081
public interface ProductClient {

    @GetMapping("/api/products/{productId}")
    Optional<ProductDto> getProductById(@PathVariable("productId") Long productId);

    // You might also need a method to check if a product exists without fetching full details
    // @GetMapping("/api/products/exists/{productId}")
    // boolean productExists(@PathVariable("productId") Long productId);
}