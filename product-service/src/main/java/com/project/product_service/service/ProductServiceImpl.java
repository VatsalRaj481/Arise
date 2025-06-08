package com.project.product_service.service;

import com.project.product_service.exception.ProductNotFoundException;
import com.project.product_service.model.Product;
import com.project.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor; // Make sure this is imported
import lombok.extern.slf4j.Slf4j; // Make sure this is imported
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// NEW IMPORTS for Feign Client and DTOs
import com.project.product_service.feignclient.StockClient;
import com.project.product_service.dto.StockDto;
import com.project.product_service.dto.ProductResponseDto;


@Service
@RequiredArgsConstructor // Lombok will generate constructor for final fields
@Slf4j
public class ProductServiceImpl implements ProductService {

    // These final fields will be injected by Lombok's @RequiredArgsConstructor
    private final ProductRepository productRepository;
    private final StockClient stockClient; // Correctly injected Feign client

    private ProductResponseDto mapProductToProductResponseDto(Product product) {
        // Default values if stock cannot be fetched or is not found
        StockDto stockDetails = null;
        String stockStatus = "Stock Info Unavailable"; // Default status

        try {
            // Call Stock Service via Feign Client to get stock for the product
            Optional<StockDto> stockOptional = stockClient.getStockByProductId(product.getId());
            if (stockOptional.isPresent()) {
                stockDetails = stockOptional.get();
                // Determine stock status based on fetched stock details
                if (stockDetails.getQuantity() <= 0) {
                    stockStatus = "Out of Stock";
                } else if (stockDetails.isLowStock()) { // Assuming StockDto has isLowStock()
                    stockStatus = "Low Stock";
                } else {
                    stockStatus = "In Stock";
                }
                log.debug("Stock found for product {}: Quantity={}, Low Stock={}",
                          product.getId(), stockDetails.getQuantity(), stockDetails.isLowStock());
            } else {
                log.warn("Stock information not found for product ID: {} from Stock Service.", product.getId());
                stockStatus = "Stock Info Unavailable"; // Explicitly state if not found
            }
        } catch (feign.FeignException.NotFound e) {
            // Specific handling for 404 from Stock Service (e.g., stock record doesn't exist for this product)
            log.warn("Stock record not found for product ID {} in Stock Service. Assuming no stock: {}", product.getId(), e.getMessage());
            stockStatus = "No Stock Record"; // Indicate no stock record
        }
        catch (Exception e) {
            // Catch any other exceptions (e.g., Stock Service is down, network issues)
            log.error("Error fetching stock for product ID {}: {}", product.getId(), e.getMessage(), e);
            stockStatus = "Stock Service Error"; // Indicate a service error

        }

        // Build the ProductResponseDto with the fetched stock details
        return ProductResponseDto.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .imageUrl(product.getImageUrl())
                .stockDetails(stockDetails) // Will be null if not found or error
                .stockStatus(stockStatus)
                .build();
    }


    @Override
    public Product createProduct(Product product) {
        log.info("Creating product: {}", product.getName());
        try {
            return productRepository.save(product);
        } catch (Exception e) {
            log.error("Error occurred while creating product: {}", product.getName(), e);
            throw new RuntimeException("Failed to create product", e);
        }
    }

    @Override
    public List<Product> getAllProducts() {
        log.info("Fetching all products from database.");
        try {
            return productRepository.findAll();
        } catch (Exception e) {
            log.error("Error occurred while fetching all products from database", e);
            throw new RuntimeException("Failed to fetch products from database", e);
        }
    }

    public List<ProductResponseDto> getAllProductsWithStock() {
        log.info("Fetching all products and enriching with stock information.");
        List<Product> products = getAllProducts(); // Get all products from local DB
        // Map each product to ProductResponseDto and enrich with stock
        return products.stream()
                .map(this::mapProductToProductResponseDto)
                .collect(Collectors.toList());
    }


    @Override
    public Optional<Product> getProductById(Long id) {
        log.info("Fetching product with id: {} from database.", id);
        try {
            return Optional.ofNullable(productRepository.findById(id)
                    .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id)));
        } catch (ProductNotFoundException e) {
            log.warn("Product not found with id: {}", id);
            throw e; // Re-throw ProductNotFoundException for ControllerAdvice to handle
        } catch (Exception e) {
            log.error("Error occurred while fetching product with id: {} from database", id, e);
            throw new RuntimeException("Failed to fetch product from database", e);
        }
    }
    public ProductResponseDto getProductByIdWithStock(Long id) {
        log.info("Fetching product with id: {} and enriching with stock information.", id);
        Product product = getProductById(id) // Get product from local DB
                            .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));

        return mapProductToProductResponseDto(product); // Enrich it with stock
    }


    @Override
    public Optional<Product> updateProduct(Long id, Product product) {
        log.info("Updating product with id: {}", id);
        try {
            if (!productRepository.existsById(id)) {
                log.warn("Attempted to update non-existent product with id: {}", id);
                throw new ProductNotFoundException("Product not found with id: " + id);
            }
            product.setId(id);
            return Optional.of(productRepository.save(product));
        } catch (ProductNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error occurred while updating product with id: {}", id, e);
            throw new RuntimeException("Failed to update product", e);
        }
    }

    @Override
    public void deleteProduct(Long id) {
        log.info("Deleting product with id: {}", id);
        try {
            if (!productRepository.existsById(id)) {
                log.warn("Attempted to delete non-existent product with id: {}", id);
                throw new ProductNotFoundException("Product not found with id: " + id);
            }
            productRepository.deleteById(id);
        } catch (ProductNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error occurred while deleting product with id: {}", id, e);
            throw new RuntimeException("Failed to delete product", e);
        }
    }

}

