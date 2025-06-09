package com.project.product_service.service;

import com.project.product_service.dto.ProductResponseDto;
import com.project.product_service.dto.StockDto;
import com.project.product_service.exception.ProductNotFoundException;
import com.project.product_service.feignclient.StockClient;
import com.project.product_service.model.Product;
import com.project.product_service.repository.ProductRepository;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the ProductServiceImpl.
 * Uses Mockito to mock dependencies (ProductRepository, StockClient)
 * to test the service layer logic in isolation.
 *
 * This test does not interact with a real database or external services,
 * making it independent of the datasource and Feign client URLs in application.properties.
 */
@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockClient stockClient;

    @InjectMocks
    private ProductServiceImpl productService; // Injecting the implementation to test its specific methods

    private Product sampleProduct;
    private StockDto sampleStockDto;

    @BeforeEach
    void setUp() {
        sampleProduct = new Product(1L, "Laptop", "Powerful laptop", 1200.0, "/images/laptop.jpg");
        sampleStockDto = new StockDto(1L, 10, 2, false);
    }

    // --- createProduct Tests ---
    @Test
    void createProduct_shouldSaveProductSuccessfully() {
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        Product createdProduct = productService.createProduct(new Product(null, "New Product", "Desc", 100.0, "/images/new.jpg"));

        assertThat(createdProduct).isNotNull();
        assertThat(createdProduct.getName()).isEqualTo("Laptop"); // Mocked to return sampleProduct
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void createProduct_shouldThrowRuntimeException_whenRepositoryFails() {
        when(productRepository.save(any(Product.class))).thenThrow(new RuntimeException("DB error"));

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            productService.createProduct(new Product(null, "New Product", "Desc", 100.0, "/images/new.jpg"));
        });

        assertThat(thrown.getMessage()).contains("Failed to create product");
        verify(productRepository, times(1)).save(any(Product.class));
    }

    // --- getAllProducts Tests (from ProductService interface) ---
    @Test
    void getAllProducts_shouldReturnAllProducts() {
        List<Product> products = Arrays.asList(sampleProduct, new Product(2L, "Mouse", "Gaming mouse", 50.0, "/images/mouse.jpg"));
        when(productRepository.findAll()).thenReturn(products);

        List<Product> result = productService.getAllProducts();

        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrderElementsOf(products);
        verify(productRepository, times(1)).findAll();
    }

    @Test
    void getAllProducts_shouldReturnEmptyList_whenNoProductsExist() {
        when(productRepository.findAll()).thenReturn(Collections.emptyList());

        List<Product> result = productService.getAllProducts();

        assertThat(result).isEmpty();
        verify(productRepository, times(1)).findAll();
    }

    @Test
    void getAllProducts_shouldThrowRuntimeException_whenRepositoryFails() {
        when(productRepository.findAll()).thenThrow(new RuntimeException("DB connection error"));

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> productService.getAllProducts());

        assertThat(thrown.getMessage()).contains("Failed to fetch products from database");
        verify(productRepository, times(1)).findAll();
    }

    // --- getProductById Tests (from ProductService interface) ---
    @Test
    void getProductById_shouldReturnProduct_whenFound() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));

        Optional<Product> foundProduct = productService.getProductById(1L);

        assertThat(foundProduct).isPresent();
        assertThat(foundProduct.get()).isEqualTo(sampleProduct);
        verify(productRepository, times(1)).findById(1L);
    }

    @Test
    void getProductById_shouldThrowProductNotFoundException_whenNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.getProductById(99L));
        verify(productRepository, times(1)).findById(99L);
    }

    @Test
    void getProductById_shouldThrowRuntimeException_whenRepositoryFails() {
        when(productRepository.findById(1L)).thenThrow(new RuntimeException("DB error"));

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> productService.getProductById(1L));

        assertThat(thrown.getMessage()).contains("Failed to fetch product from database");
        verify(productRepository, times(1)).findById(1L);
    }

    // --- updateProduct Tests ---
    @Test
    void updateProduct_shouldUpdateProductSuccessfully() {
        Product updatedProduct = new Product(1L, "Updated Laptop", "Better specs", 1300.0, "/images/updated_laptop.jpg");
        when(productRepository.existsById(1L)).thenReturn(true);
        when(productRepository.save(any(Product.class))).thenReturn(updatedProduct);

        Optional<Product> result = productService.updateProduct(1L, updatedProduct);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Updated Laptop");
        verify(productRepository, times(1)).existsById(1L);
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void updateProduct_shouldThrowProductNotFoundException_whenProductDoesNotExist() {
        Product updatedProduct = new Product(99L, "NonExistent", "Desc", 10.0, "/images/nonexistent.jpg");
        when(productRepository.existsById(99L)).thenReturn(false);

        assertThrows(ProductNotFoundException.class, () -> productService.updateProduct(99L, updatedProduct));

        verify(productRepository, times(1)).existsById(99L);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void updateProduct_shouldThrowRuntimeException_whenRepositoryFails() {
        Product updatedProduct = new Product(1L, "Updated Laptop", "Better specs", 1300.0, "/images/updated_laptop.jpg");
        when(productRepository.existsById(1L)).thenReturn(true);
        when(productRepository.save(any(Product.class))).thenThrow(new RuntimeException("DB update error"));

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> productService.updateProduct(1L, updatedProduct));

        assertThat(thrown.getMessage()).contains("Failed to update product");
        verify(productRepository, times(1)).existsById(1L);
        verify(productRepository, times(1)).save(any(Product.class));
    }

    // --- deleteProduct Tests ---
    @Test
    void deleteProduct_shouldDeleteProductSuccessfully() {
        when(productRepository.existsById(1L)).thenReturn(true);
        doNothing().when(productRepository).deleteById(1L);

        assertDoesNotThrow(() -> productService.deleteProduct(1L));

        verify(productRepository, times(1)).existsById(1L);
        verify(productRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteProduct_shouldThrowProductNotFoundException_whenProductDoesNotExist() {
        when(productRepository.existsById(99L)).thenReturn(false);

        assertThrows(ProductNotFoundException.class, () -> productService.deleteProduct(99L));

        verify(productRepository, times(1)).existsById(99L);
        verify(productRepository, never()).deleteById(anyLong());
    }

    @Test
    void deleteProduct_shouldThrowRuntimeException_whenRepositoryFails() {
        when(productRepository.existsById(1L)).thenReturn(true);
        doThrow(new RuntimeException("DB deletion error")).when(productRepository).deleteById(1L);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> productService.deleteProduct(1L));

        assertThat(thrown.getMessage()).contains("Failed to delete product");
        verify(productRepository, times(1)).existsById(1L);
        verify(productRepository, times(1)).deleteById(1L);
    }

    // --- getAllProductsWithStock Tests (specific to ProductServiceImpl) ---
    @Test
    void getAllProductsWithStock_shouldReturnEnrichedProducts_whenStockAvailable() {
        Product product2 = new Product(2L, "Keyboard", "Mechanical", 80.0, "/images/keyboard.jpg");
        List<Product> products = Arrays.asList(sampleProduct, product2);
        StockDto stock2 = new StockDto(2L, 5, 1, true); // Low stock

        when(productRepository.findAll()).thenReturn(products);
        when(stockClient.getStockByProductId(1L)).thenReturn(Optional.of(sampleStockDto));
        when(stockClient.getStockByProductId(2L)).thenReturn(Optional.of(stock2));

        List<ProductResponseDto> result = productService.getAllProductsWithStock();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getStockDetails()).isEqualTo(sampleStockDto);
        assertThat(result.get(0).getStockStatus()).isEqualTo("In Stock"); // Based on sampleStockDto quantity 10, reorder 2, lowStock false

        assertThat(result.get(1).getId()).isEqualTo(2L);
        assertThat(result.get(1).getStockDetails()).isEqualTo(stock2);
        assertThat(result.get(1).getStockStatus()).isEqualTo("Low Stock"); // Based on stock2 lowStock true

        verify(productRepository, times(1)).findAll();
        verify(stockClient, times(1)).getStockByProductId(1L);
        verify(stockClient, times(1)).getStockByProductId(2L);
    }

    @Test
    void getAllProductsWithStock_shouldHandleMissingStockInfo() {
        Product product2 = new Product(2L, "Keyboard", "Mechanical", 80.0, "/images/keyboard.jpg");
        List<Product> products = Arrays.asList(sampleProduct, product2);

        when(productRepository.findAll()).thenReturn(products);
        when(stockClient.getStockByProductId(1L)).thenReturn(Optional.of(sampleStockDto));
        when(stockClient.getStockByProductId(2L)).thenReturn(Optional.empty()); // Stock not found for product 2

        List<ProductResponseDto> result = productService.getAllProductsWithStock();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getStockDetails()).isEqualTo(sampleStockDto);
        assertThat(result.get(0).getStockStatus()).isEqualTo("In Stock");

        assertThat(result.get(1).getId()).isEqualTo(2L);
        assertThat(result.get(1).getStockDetails()).isNull(); // Stock details should be null
        assertThat(result.get(1).getStockStatus()).isEqualTo("Stock Info Unavailable"); // Default status

        verify(productRepository, times(1)).findAll();
        verify(stockClient, times(1)).getStockByProductId(1L);
        verify(stockClient, times(1)).getStockByProductId(2L);
    }

    @Test
    void getAllProductsWithStock_shouldHandleStockServiceError() {
        Product product2 = new Product(2L, "Keyboard", "Mechanical", 80.0, "/images/keyboard.jpg");
        List<Product> products = Arrays.asList(sampleProduct, product2);

        when(productRepository.findAll()).thenReturn(products);
        when(stockClient.getStockByProductId(1L)).thenReturn(Optional.of(sampleStockDto));
        // Simulate Feign client throwing an exception for product 2 (e.g., service down)
        when(stockClient.getStockByProductId(2L)).thenThrow(mock(FeignException.class));

        List<ProductResponseDto> result = productService.getAllProductsWithStock();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getStockDetails()).isEqualTo(sampleStockDto);
        assertThat(result.get(0).getStockStatus()).isEqualTo("In Stock");

        assertThat(result.get(1).getId()).isEqualTo(2L);
        assertThat(result.get(1).getStockDetails()).isNull(); // Stock details should be null
        assertThat(result.get(1).getStockStatus()).isEqualTo("Stock Service Error"); // Error status

        verify(productRepository, times(1)).findAll();
        verify(stockClient, times(1)).getStockByProductId(1L);
        verify(stockClient, times(1)).getStockByProductId(2L);
    }

    // --- getProductByIdWithStock Tests (specific to ProductServiceImpl) ---
    @Test
    void getProductByIdWithStock_shouldReturnEnrichedProduct_whenStockAvailable() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(stockClient.getStockByProductId(1L)).thenReturn(Optional.of(sampleStockDto));

        ProductResponseDto result = productService.getProductByIdWithStock(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Laptop");
        assertThat(result.getStockDetails()).isEqualTo(sampleStockDto);
        assertThat(result.getStockStatus()).isEqualTo("In Stock");

        verify(productRepository, times(1)).findById(1L);
        verify(stockClient, times(1)).getStockByProductId(1L);
    }

    @Test
    void getProductByIdWithStock_shouldThrowProductNotFoundException_whenProductNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.getProductByIdWithStock(99L));

        verify(productRepository, times(1)).findById(99L);
        verify(stockClient, never()).getStockByProductId(anyLong()); // Should not call stock service
    }

    @Test
    void getProductByIdWithStock_shouldHandleMissingStockInfo() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(stockClient.getStockByProductId(1L)).thenReturn(Optional.empty()); // Stock not found

        ProductResponseDto result = productService.getProductByIdWithStock(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStockDetails()).isNull();
        assertThat(result.getStockStatus()).isEqualTo("Stock Info Unavailable");

        verify(productRepository, times(1)).findById(1L);
        verify(stockClient, times(1)).getStockByProductId(1L);
    }

    @Test
    void getProductByIdWithStock_shouldHandleStockServiceError() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(stockClient.getStockByProductId(1L)).thenThrow(mock(FeignException.class)); // Simulate Feign error

        ProductResponseDto result = productService.getProductByIdWithStock(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStockDetails()).isNull();
        assertThat(result.getStockStatus()).isEqualTo("Stock Service Error");

        verify(productRepository, times(1)).findById(1L);
        verify(stockClient, times(1)).getStockByProductId(1L);
    }
}

