package com.project.product_service.service;

import com.project.product_service.dto.StockDto;
import com.project.product_service.model.Product;

import java.util.List;
import java.util.Optional;

public interface ProductService {
    Product createProduct(Product product);
    List<Product> getAllProducts();
    Optional<Product> getProductById(Long id);
    Optional<Product> updateProduct(Long id, Product product);
    void deleteProduct(Long id);
}

