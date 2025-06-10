package com.project.product_service.controller;

import com.project.product_service.dto.ProductRequestDto; // NEW
import com.project.product_service.exception.ProductNotFoundException;
import com.project.product_service.model.Product; // Keep Product import for create/update/delete internally in service
import com.project.product_service.service.ProductService;
import com.project.product_service.service.ProductServiceImpl;
import com.project.product_service.dto.ProductResponseDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus; // For CREATED status
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
//@CrossOrigin(origins = "http://localhost:3000")
public class ProductController {

    private final ProductService productService;
    private final ProductServiceImpl productServiceImpl; // Keep for getAllProductsWithStock/getProductByIdWithStock
    private static final String UPLOAD_DIR = "product-service/src/main/resources/static/images/";


    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody ProductRequestDto productRequestDto) { // Changed parameter
        log.info("Attempting to create product: {}", productRequestDto.getName());

        try {
            // Handle image URL if it's a file path or needs processing
            String imageUrl = productRequestDto.getImageUrl();
            if (imageUrl == null || imageUrl.isEmpty()) {
                log.warn("Image path not provided for product: {}", productRequestDto.getName());
                // You might return a BAD_REQUEST or handle a default image
                // For now, letting it proceed, but the image will be null
            } else {
                Path uploadPath = Paths.get(UPLOAD_DIR);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                    log.info("Created images directory.");
                }

                Path sourcePath = Paths.get(imageUrl); // Assuming imageUrl is a file path on the server where this service runs
                Path destinationPath = uploadPath.resolve(sourcePath.getFileName());

                Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                log.info("Image copied successfully: {}", destinationPath.toString());

                // Update the DTO's imageUrl to the public path for the product object
                productRequestDto.setImageUrl("/images/" + sourcePath.getFileName().toString());
            }

            // The service method now expects ProductRequestDto and handles stock creation
            Product savedProduct = productService.createProduct(productRequestDto);
            return new ResponseEntity<>(savedProduct, HttpStatus.CREATED); // Return 201 Created

        } catch (IOException e) {
            log.error("Error copying image for product: {}", productRequestDto.getName(), e);
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            log.error("Error creating product: {}", productRequestDto.getName(), e);
            return ResponseEntity.internalServerError().build(); // Catch other service exceptions
        }
    }

    @GetMapping
    public ResponseEntity<List<ProductResponseDto>> getAllProducts() {
        log.info("Fetching all products with stock information");
        try {
            List<ProductResponseDto> products = productServiceImpl.getAllProductsWithStock();
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            log.error("Error fetching products with stock information", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDto> getProductById(@PathVariable Long id) {
        log.info("Fetching product by ID: {} with stock information", id);
        try {
            ProductResponseDto product = productServiceImpl.getProductByIdWithStock(id);
            return ResponseEntity.ok(product);
        } catch (ProductNotFoundException e) {
            log.warn("Product not found: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error fetching product with stock information: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody ProductRequestDto productRequestDto) { // Changed parameter
        log.info("Updating product: {}", id);

        try {
            // Handle image URL if it's a file path or needs processing
            String imageUrl = productRequestDto.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Path uploadPath = Paths.get(UPLOAD_DIR);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                    log.info("Created images directory.");
                }

                Path sourcePath = Paths.get(imageUrl);
                Path destinationPath = uploadPath.resolve(sourcePath.getFileName());

                Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                log.info("Image copied successfully: {}", destinationPath.toString());

                productRequestDto.setImageUrl("/images/" + sourcePath.getFileName().toString());
            }

            // The service method now expects ProductRequestDto and handles stock update
            Product updatedProduct = productService.updateProduct(id, productRequestDto);
            return ResponseEntity.ok(updatedProduct);
        } catch (ProductNotFoundException e) {
            log.warn("Product not found for update: {}", id);
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            log.error("Error copying image for product update: {}", id, e);
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            log.error("Error updating product: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        log.info("Deleting product: {}", id);

        try {
            productService.deleteProduct(id); // This now handles stock deletion too
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