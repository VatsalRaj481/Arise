
package com.project.supplier_service.feignclient;

import com.project.supplier_service.dto.ProductDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

// 'name' must match the spring.application.name of the product-service
// 'url' should point to the base URL of the product-service's API (via Gateway or direct)
// If using API Gateway, it would be http://localhost:8080/api/products
// If direct, it would be http://localhost:8081/api/products (assuming product-service runs on 8081)
@FeignClient(name = "product-service", url = "http://localhost:8081/api/products")
public interface ProductClient {

    @GetMapping("/{id}")
    Optional<ProductDto> getProductById(@PathVariable("id") Long id);

    // You might also add other methods if needed, e.g.,
    // @GetMapping("/ids")
    // List<ProductDto> getProductsByIds(@RequestParam List<Long> ids);
}
