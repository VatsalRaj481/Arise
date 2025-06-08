package com.project.product_service.controller;


import com.project.product_service.dto.StockDto;
import com.project.product_service.exception.ProductNotFoundException;
import com.project.product_service.model.Product; // Keep Product import for create/update/delete
import com.project.product_service.service.ProductService; // Keep ProductService interface
import com.project.product_service.service.ProductServiceImpl; // NEW: Import ProductServiceImpl directly
import com.project.product_service.dto.ProductResponseDto; // NEW: Import ProductResponseDto

import lombok.RequiredArgsConstructor; // Make sure this is imported
import lombok.extern.slf4j.Slf4j; // Make sure this is imported
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.*;
import java.io.IOException;
import java.util.List;
import java.util.Optional;


@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor 
@Slf4j
//@CrossOrigin(origins = "http://localhost:3000") // Enable CORS for frontend communication
public class ProductController {

    // These final fields will be injected by Lombok's @RequiredArgsConstructor
    private final ProductService productService; // Injects the interface
    private final ProductServiceImpl productServiceImpl; // Injects the implementation to access new methods
    private static final String UPLOAD_DIR = "product-service/src/main/resources/static/images/";


    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        log.info("Creating product: {}", product.getName());

        try {
            if (product.getImageUrl() == null || product.getImageUrl().isEmpty()) {
                log.warn("Image path not provided for product: {}", product.getName());
                return ResponseEntity.badRequest().body(null);
            }

            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("Created images directory.");
            }

            Path sourcePath = Paths.get(product.getImageUrl());
            Path destinationPath = uploadPath.resolve(sourcePath.getFileName());

            Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Image copied successfully: {}", destinationPath.toString());

            product.setImageUrl("/images/" + sourcePath.getFileName().toString());

            Product savedProduct = productService.createProduct(product); // Use interface method
            return ResponseEntity.ok(savedProduct);

        } catch (IOException e) {
            log.error("Error copying image for product: {}", product.getName(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<ProductResponseDto>> getAllProducts() { // Return enriched DTO
        log.info("Fetching all products with stock information");
        try {
            List<ProductResponseDto> products = productServiceImpl.getAllProductsWithStock(); // Use new service method
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            log.error("Error fetching products with stock information", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    // Fetch product by ID with enriched stock data
    // http://localhost:8081/api/products/1
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDto> getProductById(@PathVariable Long id) { // Return enriched DTO
        log.info("Fetching product by ID: {} with stock information", id);
        try {
            ProductResponseDto product = productServiceImpl.getProductByIdWithStock(id); // Use new service method
            return ResponseEntity.ok(product);
        } catch (ProductNotFoundException e) {
            log.warn("Product not found: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error fetching product with stock information: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Update product details (remains similar, no stock fetching on update)
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product product) {
        log.info("Updating product: {}", id);

        try {
            Product updatedProduct = productService.updateProduct(id, product)
                    .orElseThrow(() -> new ProductNotFoundException("Product not found"));
            return ResponseEntity.ok(updatedProduct);
        } catch (ProductNotFoundException e) {
            log.warn("Product not found for update: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error updating product: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Delete product
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        log.info("Deleting product: {}", id);

        try {
            productService.deleteProduct(id);
            return ResponseEntity.ok().build();
        } catch (ProductNotFoundException e) {
            log.warn("Product not found for deletion: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error deleting product: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

}