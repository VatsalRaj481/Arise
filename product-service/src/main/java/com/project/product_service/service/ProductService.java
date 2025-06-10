package com.project.product_service.service;

import com.project.product_service.dto.ProductRequestDto; // NEW
import com.project.product_service.dto.ProductResponseDto; // Keep for output
import com.project.product_service.model.Product;

import java.util.List;
import java.util.Optional;

public interface ProductService {
    // Changed parameter type to ProductRequestDto
    Product createProduct(ProductRequestDto productRequestDto);
    List<Product> getAllProducts(); // No change
    Optional<Product> getProductById(Long id); // No change, returns raw Product for internal mapping
    ProductResponseDto getProductByIdWithStock(Long id); // Returns enriched DTO
    List<ProductResponseDto> getAllProductsWithStock(); // Returns enriched DTO

    // Changed parameter type to ProductRequestDto
    Product updateProduct(Long id, ProductRequestDto productRequestDto);
    void deleteProduct(Long id);
}
