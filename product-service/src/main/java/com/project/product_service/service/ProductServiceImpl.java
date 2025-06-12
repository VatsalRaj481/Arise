//package com.project.product_service.service;
//
//import com.project.product_service.dto.ProductRequestDto; // NEW
//import com.project.product_service.exception.ProductNotFoundException;
//import com.project.product_service.model.Product;
//import com.project.product_service.repository.ProductRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//import java.util.Optional;
//import java.util.stream.Collectors;
//
//// NEW IMPORTS for Feign Client and DTOs
//import com.project.product_service.feignclient.StockClient;
//import com.project.product_service.dto.StockDto;
//import com.project.product_service.dto.ProductResponseDto;
//import com.project.product_service.feignclient.StockClient; // NEW Feign Client for sending stock updates
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class ProductServiceImpl implements ProductService {
//
//    private final ProductRepository productRepository;
//    private final StockClient stockClient; // For GETting stock (ProductResponseDto mapping)
//    private final StockClient stockManipulationClient; // NEW For POST/PUTting stock
//
//    private ProductResponseDto mapProductToProductResponseDto(Product product) {
//        // ... (existing logic for mapping remains the same) ...
//        StockDto stockDetails = null;
//        String stockStatus = "Stock Info Unavailable";
//
//        try {
//            Optional<StockDto> stockOptional = stockClient.getStockByProductId(product.getId());
//            if (stockOptional.isPresent()) {
//                stockDetails = stockOptional.get();
//                if (stockDetails.getQuantity() <= 0) {
//                    stockStatus = "Out of Stock";
//                } else if (stockDetails.isLowStock()) {
//                    stockStatus = "Low Stock";
//                } else {
//                    stockStatus = "In Stock";
//                }
//                log.debug("Stock found for product {}: Quantity={}, Low Stock={}",
//                        product.getId(), stockDetails.getQuantity(), stockDetails.isLowStock());
//            } else {
//                log.warn("Stock information not found for product ID: {} from Stock Service.", product.getId());
//                stockStatus = "No Stock Record";
//            }
//        } catch (feign.FeignException.NotFound e) {
//            log.warn("Stock record not found for product ID {} in Stock Service. Assuming no stock: {}", product.getId(), e.getMessage());
//            stockStatus = "No Stock Record";
//        } catch (Exception e) {
//            log.error("Error fetching stock for product ID {}: {}", product.getId(), e.getMessage(), e);
//            stockStatus = "Stock Service Error";
//        }
//
//        return ProductResponseDto.builder()
//                .id(product.getId())
//                .name(product.getName())
//                .description(product.getDescription())
//                .price(product.getPrice())
//                .imageUrl(product.getImageUrl())
//                .stockDetails(stockDetails)
//                .stockStatus(stockStatus)
//                .build();
//    }
//
//    @Override
//    public Product createProduct(ProductRequestDto productRequestDto) {
//        log.info("Creating product: {}", productRequestDto.getName());
//        Product productToSave = Product.builder()
//                .name(productRequestDto.getName())
//                .description(productRequestDto.getDescription())
//                .price(productRequestDto.getPrice())
//                .imageUrl(productRequestDto.getImageUrl())
//                .build();
//
//        Product savedProduct;
//        try {
//            savedProduct = productRepository.save(productToSave);
//            log.info("Product saved successfully with ID: {}", savedProduct.getId());
//        } catch (Exception e) {
//            log.error("Error occurred while saving product: {}", productRequestDto.getName(), e);
//            throw new RuntimeException("Failed to create product", e);
//        }
//
//        // --- NEW: Call Stock Service to add initial stock ---
//        if (productRequestDto.getInitialStockQuantity() != null) {
//            try {
//                StockDto stockToCreate = new StockDto();
//                stockToCreate.setProductId(savedProduct.getId());
//                stockToCreate.setQuantity(productRequestDto.getInitialStockQuantity());
//                stockToCreate.setReorderLevel(productRequestDto.getReorderLevel() != null ? productRequestDto.getReorderLevel() : 10); // Default reorder level
//
//                // This will call the POST /api/stocks endpoint in the stock-service
//                stockManipulationClient.addStock(stockToCreate);
//                log.info("Initial stock added for new product ID {}: Quantity={}",
//                        savedProduct.getId(), productRequestDto.getInitialStockQuantity());
//            } catch (feign.FeignException.Conflict e) {
//                log.warn("Stock record already exists for product ID {} during creation. Skipping initial stock add.", savedProduct.getId());
//            } catch (Exception e) {
//                log.error("Error adding initial stock for product ID {}: {}", savedProduct.getId(), e.getMessage(), e);
//                // Decide on rollback strategy: do you delete the product if stock fails?
//                // For now, we'll log and let the product exist without stock.
//            }
//        } else {
//            log.info("No initial stock quantity provided for product ID {}.", savedProduct.getId());
//        }
//        // --- END NEW ---
//
//        return savedProduct;
//    }
//
//    @Override
//    public List<Product> getAllProducts() {
//        // ... (existing logic) ...
//        log.info("Fetching all products from database.");
//        try {
//            return productRepository.findAll();
//        } catch (Exception e) {
//            log.error("Error occurred while fetching all products from database", e);
//            throw new RuntimeException("Failed to fetch products from database", e);
//        }
//    }
//
//    public List<ProductResponseDto> getAllProductsWithStock() {
//        // ... (existing logic) ...
//        log.info("Fetching all products and enriching with stock information.");
//        List<Product> products = getAllProducts();
//        return products.stream()
//                .map(this::mapProductToProductResponseDto)
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    public Optional<Product> getProductById(Long id) {
//        // ... (existing logic) ...
//        log.info("Fetching product with id: {} from database.", id);
//        try {
//            return Optional.ofNullable(productRepository.findById(id)
//                    .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id)));
//        } catch (ProductNotFoundException e) {
//            log.warn("Product not found with id: {}", id);
//            throw e;
//        } catch (Exception e) {
//            log.error("Error occurred while fetching product with id: {} from database", id, e);
//            throw new RuntimeException("Failed to fetch product from database", e);
//        }
//    }
//
//    public ProductResponseDto getProductByIdWithStock(Long id) {
//        // ... (existing logic) ...
//        log.info("Fetching product with id: {} and enriching with stock information.", id);
//        Product product = getProductById(id)
//                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));
//
//        return mapProductToProductResponseDto(product);
//    }
//
//    @Override
//    public Product updateProduct(Long id, ProductRequestDto productRequestDto) { // Changed parameter
//        log.info("Updating product with id: {}", id);
//        Product existingProduct = productRepository.findById(id)
//                .orElseThrow(() -> {
//                    log.warn("Attempted to update non-existent product with id: {}", id);
//                    return new ProductNotFoundException("Product not found with id: " + id);
//                });
//
//        try {
//            // Update product details
//            existingProduct.setName(productRequestDto.getName());
//            existingProduct.setDescription(productRequestDto.getDescription());
//            existingProduct.setPrice(productRequestDto.getPrice());
//            existingProduct.setImageUrl(productRequestDto.getImageUrl());
//
//            Product updatedProduct = productRepository.save(existingProduct);
//            log.info("Product details updated successfully for ID: {}", updatedProduct.getId());
//
//            // --- NEW: Call Stock Service to update stock (or add if it doesn't exist) ---
//            if (productRequestDto.getInitialStockQuantity() != null || productRequestDto.getReorderLevel() != null) {
//                try {
//                    StockDto stockToUpdate = new StockDto();
//                    stockToUpdate.setProductId(id);
//                    // Only update quantity if provided, otherwise keep existing
//                    if (productRequestDto.getInitialStockQuantity() != null) {
//                        stockToUpdate.setQuantity(productRequestDto.getInitialStockQuantity());
//                    } else {
//                        // If quantity is null, fetch existing stock and use its quantity
//                        Optional<StockDto> currentStock = stockClient.getStockByProductId(id);
//                        if (currentStock.isPresent()) {
//                            stockToUpdate.setQuantity(currentStock.get().getQuantity());
//                        } else {
//                            // If no quantity provided and no existing stock, default to 0 or handle error
//                            stockToUpdate.setQuantity(0);
//                            log.warn("No stock quantity provided and no existing stock for product {}. Setting quantity to 0.", id);
//                        }
//                    }
//
//                    // Only update reorderLevel if provided
//                    if (productRequestDto.getReorderLevel() != null) {
//                        stockToUpdate.setReorderLevel(productRequestDto.getReorderLevel());
//                    } else {
//                        // If reorderLevel is null, fetch existing stock and use its reorderLevel
//                        Optional<StockDto> currentStock = stockClient.getStockByProductId(id);
//                        if (currentStock.isPresent()) {
//                            stockToUpdate.setReorderLevel(currentStock.get().getReorderLevel());
//                        } else {
//                            stockToUpdate.setReorderLevel(0); // Default if no existing stock
//                            log.warn("No reorder level provided and no existing stock for product {}. Setting reorder level to 0.", id);
//                        }
//                    }
//
//
//                    // Attempt to update stock. If it's a 404 (stock record doesn't exist), create it.
//                    try {
//                        stockManipulationClient.updateStock(id, stockToUpdate);
//                        log.info("Stock updated for product ID {}: Quantity={}, ReorderLevel={}",
//                                id, stockToUpdate.getQuantity(), stockToUpdate.getReorderLevel());
//                    } catch (feign.FeignException.NotFound e) {
//                        // If update fails because stock record doesn't exist, create it
//                        log.warn("Stock record not found for product ID {} during update. Attempting to add new stock.", id);
//                        stockManipulationClient.addStock(stockToUpdate); // Add new stock if not found
//                        log.info("New stock record created for product ID {}: Quantity={}, ReorderLevel={}",
//                                id, stockToUpdate.getQuantity(), stockToUpdate.getReorderLevel());
//                    }
//                } catch (Exception e) {
//                    log.error("Error updating/adding stock for product ID {}: {}", id, e.getMessage(), e);
//                    // Do not rethrow, just log, as product update might still be successful
//                }
//            } else {
//                log.info("No stock quantity or reorder level provided for product ID {} during update. Stock was not modified.", id);
//            }
//            // --- END NEW ---
//
//            return updatedProduct;
//        } catch (ProductNotFoundException e) {
//            throw e;
//        } catch (Exception e) {
//            log.error("Error occurred while updating product with id: {}", id, e);
//            throw new RuntimeException("Failed to update product", e);
//        }
//    }
//
//    @Override
//    public void deleteProduct(Long id) {
//        log.info("Deleting product with id: {}", id);
//        try {
//            if (!productRepository.existsById(id)) {
//                log.warn("Attempted to delete non-existent product with id: {}", id);
//                throw new ProductNotFoundException("Product not found with id: " + id);
//            }
//            productRepository.deleteById(id);
//            log.info("Product deleted for ID: {}", id);
//
//            // --- NEW: Also delete stock record when product is deleted ---
//            try {
//                // Check if stock exists before attempting to delete to avoid unnecessary errors
//                Optional<StockDto> stockOptional = stockClient.getStockByProductId(id);
//                if (stockOptional.isPresent()) {
//                    stockManipulationClient.deleteStock(id); // Call new Feign method
//                    log.info("Stock record deleted for product ID: {}", id);
//                } else {
//                    log.warn("No stock record found for product ID {} to delete. Skipping stock deletion.", id);
//                }
//            } catch (feign.FeignException.NotFound e) {
//                log.warn("Stock record not found for product ID {} during deletion. This is expected if it didn't exist.", id);
//            } catch (Exception e) {
//                log.error("Error deleting stock for product ID {}: {}", id, e.getMessage(), e);
//                // Decide if product deletion should rollback if stock deletion fails
//                // For now, product is deleted, stock might remain orphaned.
//            }
//            // --- END NEW ---
//
//        } catch (ProductNotFoundException e) {
//            throw e;
//        } catch (Exception e) {
//            log.error("Error occurred while deleting product with id: {}", id, e);
//            throw new RuntimeException("Failed to delete product", e);
//        }
//    }
//}

package com.project.product_service.service;

import com.project.product_service.dto.ProductRequestDto;
import com.project.product_service.exception.ProductNotFoundException;
import com.project.product_service.model.Product;
import com.project.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.project.product_service.feignclient.StockClient;
import com.project.product_service.dto.StockDto;
import com.project.product_service.dto.ProductResponseDto;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final StockClient stockClient; // Correct: Single instance injected

    private ProductResponseDto mapProductToProductResponseDto(Product product) {
        StockDto stockDetails = null;
        String stockStatus = "Stock Info Unavailable";

        try {
            // Correct: Call method on the injected 'stockClient' instance
            Optional<StockDto> stockOptional = stockClient.getStockByProductId(product.getId());
            if (stockOptional.isPresent()) {
                stockDetails = stockOptional.get();
                if (stockDetails.getQuantity() <= 0) {
                    stockStatus = "Out of Stock";
                } else if (stockDetails.isLowStock()) {
                    stockStatus = "Low Stock";
                } else {
                    stockStatus = "In Stock";
                }
                log.debug("Stock found for product {}: Quantity={}, Low Stock={}",
                        product.getId(), stockDetails.getQuantity(), stockDetails.isLowStock());
            } else {
                log.warn("Stock information not found for product ID: {} from Stock Service.", product.getId());
                stockStatus = "No Stock Record";
            }
        } catch (feign.FeignException.NotFound e) {
            log.warn("Stock record not found for product ID {} in Stock Service. Assuming no stock: {}", product.getId(), e.getMessage());
            stockStatus = "No Stock Record";
        } catch (Exception e) {
            log.error("Error fetching stock for product ID {}: {}", product.getId(), e.getMessage(), e);
            stockStatus = "Stock Service Error";
        }

        return ProductResponseDto.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .imageUrl(product.getImageUrl())
                .stockDetails(stockDetails)
                .stockStatus(stockStatus)
                .build();
    }

    @Override
    public Product createProduct(ProductRequestDto productRequestDto) {
        log.info("Creating product: {}", productRequestDto.getName());
        Product productToSave = Product.builder()
                .name(productRequestDto.getName())
                .description(productRequestDto.getDescription())
                .price(productRequestDto.getPrice())
                .imageUrl(productRequestDto.getImageUrl())
                .build();

        Product savedProduct;
        try {
            savedProduct = productRepository.save(productToSave);
            log.info("Product saved successfully with ID: {}", savedProduct.getId());
        } catch (Exception e) {
            log.error("Error occurred while saving product: {}", productRequestDto.getName(), e);
            throw new RuntimeException("Failed to create product", e);
        }

        // --- NEW: Call Stock Service to add initial stock ---
        if (productRequestDto.getInitialStockQuantity() != null) {
            try {
                StockDto stockToCreate = new StockDto();
                stockToCreate.setProductId(savedProduct.getId());
                stockToCreate.setQuantity(productRequestDto.getInitialStockQuantity());
                stockToCreate.setReorderLevel(productRequestDto.getReorderLevel() != null ? productRequestDto.getReorderLevel() : 10); // Default reorder level

                // Correct: Call method on the injected 'stockClient' instance
                stockClient.addStock(stockToCreate);
                log.info("Initial stock added for new product ID {}: Quantity={}",
                        savedProduct.getId(), productRequestDto.getInitialStockQuantity());
            } catch (feign.FeignException.Conflict e) {
                log.warn("Stock record already exists for product ID {} during creation. Skipping initial stock add.", savedProduct.getId());
            } catch (Exception e) {
                log.error("Error adding initial stock for product ID {}: {}", savedProduct.getId(), e.getMessage(), e);
                // Decide on rollback strategy: do you delete the product if stock fails?
                // For now, we'll log and let the product exist without stock.
            }
        } else {
            log.info("No initial stock quantity provided for product ID {}.", savedProduct.getId());
        }
        // --- END NEW ---

        return savedProduct;
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
        List<Product> products = getAllProducts();
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
            throw e;
        } catch (Exception e) {
            log.error("Error occurred while fetching product with id: {} from database", id, e);
            throw new RuntimeException("Failed to fetch product from database", e);
        }
    }

    public ProductResponseDto getProductByIdWithStock(Long id) {
        log.info("Fetching product with id: {} and enriching with stock information.", id);
        Product product = getProductById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));

        return mapProductToProductResponseDto(product);
    }

    @Override
    public Product updateProduct(Long id, ProductRequestDto productRequestDto) {
        log.info("Updating product with id: {}", id);
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Attempted to update non-existent product with id: {}", id);
                    return new ProductNotFoundException("Product not found with id: " + id);
                });

        try {
            // Update product details
            existingProduct.setName(productRequestDto.getName());
            existingProduct.setDescription(productRequestDto.getDescription());
            existingProduct.setPrice(productRequestDto.getPrice());
            existingProduct.setImageUrl(productRequestDto.getImageUrl());

            Product updatedProduct = productRepository.save(existingProduct);
            log.info("Product details updated successfully for ID: {}", updatedProduct.getId());

            // --- NEW: Call Stock Service to update stock (or add if it doesn't exist) ---
            if (productRequestDto.getInitialStockQuantity() != null || productRequestDto.getReorderLevel() != null) {
                try {
                    StockDto stockToUpdate = new StockDto();
                    stockToUpdate.setProductId(id);

                    // Only update quantity if provided, otherwise keep existing
                    if (productRequestDto.getInitialStockQuantity() != null) {
                        stockToUpdate.setQuantity(productRequestDto.getInitialStockQuantity());
                    } else {
                        // If quantity is null, fetch existing stock and use its quantity
                        // Correct: Call method on the injected 'stockClient' instance
                        Optional<StockDto> currentStock = stockClient.getStockByProductId(id);
                        if (currentStock.isPresent()) {
                            stockToUpdate.setQuantity(currentStock.get().getQuantity());
                        } else {
                            // If no quantity provided and no existing stock, default to 0 or handle error
                            stockToUpdate.setQuantity(0);
                            log.warn("No stock quantity provided and no existing stock for product {}. Setting quantity to 0.", id);
                        }
                    }

                    // Only update reorderLevel if provided
                    if (productRequestDto.getReorderLevel() != null) {
                        stockToUpdate.setReorderLevel(productRequestDto.getReorderLevel());
                    } else {
                        // If reorderLevel is null, fetch existing stock and use its reorderLevel
                        // Correct: Call method on the injected 'stockClient' instance
                        Optional<StockDto> currentStock = stockClient.getStockByProductId(id);
                        if (currentStock.isPresent()) {
                            stockToUpdate.setReorderLevel(currentStock.get().getReorderLevel());
                        } else {
                            stockToUpdate.setReorderLevel(0); // Default if no existing stock
                            log.warn("No reorder level provided and no existing stock for product {}. Setting reorder level to 0.", id);
                        }
                    }

                    // Attempt to update stock. If it's a 404 (stock record doesn't exist), create it.
                    try {
                        // Correct: Call method on the injected 'stockClient' instance
                        stockClient.updateStock(id, stockToUpdate);
                        log.info("Stock updated for product ID {}: Quantity={}, ReorderLevel={}",
                                id, stockToUpdate.getQuantity(), stockToUpdate.getReorderLevel());
                    } catch (feign.FeignException.NotFound e) {
                        // If update fails because stock record doesn't exist, create it
                        log.warn("Stock record not found for product ID {} during update. Attempting to add new stock.", id);
                        // Correct: Call method on the injected 'stockClient' instance
                        stockClient.addStock(stockToUpdate); // Add new stock if not found
                        log.info("New stock record created for product ID {}: Quantity={}, ReorderLevel={}",
                                id, stockToUpdate.getQuantity(), stockToUpdate.getReorderLevel());
                    }
                } catch (Exception e) {
                    log.error("Error updating/adding stock for product ID {}: {}", id, e.getMessage(), e);
                    // Do not rethrow, just log, as product update might still be successful
                }
            } else {
                log.info("No stock quantity or reorder level provided for product ID {} during update. Stock was not modified.", id);
            }
            // --- END NEW ---

            return updatedProduct;
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
            log.info("Product deleted for ID: {}", id);

            // --- NEW: Also delete stock record when product is deleted ---
            try {
                // Check if stock exists before attempting to delete to avoid unnecessary errors
                // Correct: Call method on the injected 'stockClient' instance
                Optional<StockDto> stockOptional = stockClient.getStockByProductId(id);
                if (stockOptional.isPresent()) {
                    // Correct: Call method on the injected 'stockClient' instance
                    stockClient.deleteStock(id);
                    log.info("Stock record deleted for product ID: {}", id);
                } else {
                    log.warn("No stock record found for product ID {} to delete. Skipping stock deletion.", id);
                }
            } catch (feign.FeignException.NotFound e) {
                log.warn("Stock record not found for product ID {} during deletion. This is expected if it didn't exist.", id);
            } catch (Exception e) {
                log.error("Error deleting stock for product ID {}: {}", id, e.getMessage(), e);
                // Decide if product deletion should rollback if stock deletion fails
                // For now, product is deleted, stock might remain orphaned.
            }
            // --- END NEW ---

        } catch (ProductNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error occurred while deleting product with id: {}", id, e);
            throw new RuntimeException("Failed to delete product", e);
        }
    }
}