package com.project.product_service.service;

import com.project.product_service.dto.ProductRequestDto;
import com.project.product_service.dto.ProductResponseDto;
import com.project.product_service.dto.StockDto;
import com.project.product_service.exception.ProductNotFoundException;
import com.project.product_service.feignclient.StockClient;
import com.project.product_service.model.Product;
import com.project.product_service.repository.ProductRepository;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockClient stockClient;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product product1;
    private Product product2;
    private ProductRequestDto productRequestDtoWithStock;
    private ProductRequestDto productRequestDtoNoStock;
    private StockDto stockDto1;
    private StockDto stockDto2;

    @BeforeEach
    void setUp() {
        reset(productRepository, stockClient); // Reset mocks for isolation

        product1 = Product.builder()
                .id(1L)
                .name("Laptop")
                .description("Powerful laptop")
                .price(1200.00)
                .imageUrl("laptop.jpg")
                .build();

        product2 = Product.builder()
                .id(2L)
                .name("Mouse")
                .description("Wireless mouse")
                .price(25.00)
                .imageUrl("mouse.jpg")
                .build();

        productRequestDtoWithStock = ProductRequestDto.builder()
                .name("Keyboard")
                .description("Mechanical keyboard")
                .price(75.00)
                .imageUrl("keyboard.jpg")
                .initialStockQuantity(50)
                .reorderLevel(10)
                .build();

        productRequestDtoNoStock = ProductRequestDto.builder()
                .name("Monitor")
                .description("Gaming monitor")
                .price(300.00)
                .imageUrl("monitor.jpg")
                .initialStockQuantity(null)
                .reorderLevel(null)
                .build();

        stockDto1 = StockDto.builder()
                .productId(1L)
                .quantity(100)
                .reorderLevel(20)
                .lowStock(false)
                .build();

        stockDto2 = StockDto.builder()
                .productId(2L)
                .quantity(5) // Low stock
                .reorderLevel(10)
                .lowStock(true)
                .build();
    }

    // 1. Test for successful product creation with and without initial stock
    @Test
    @DisplayName("1. Should create a product: with initial stock and without initial stock")
    void createProduct_SuccessScenarios() {
        // Scenario 1: With initial stock
        when(productRepository.save(any(Product.class))).thenReturn(product1);
        Product createdProduct1 = productService.createProduct(productRequestDtoWithStock);
        assertNotNull(createdProduct1);
        assertEquals(product1.getId(), createdProduct1.getId());
        verify(productRepository, times(1)).save(any(Product.class));
        verify(stockClient, times(1)).addStock(any(StockDto.class));

        // Reset for the next scenario in the same test
        reset(productRepository, stockClient);
        when(productRepository.save(any(Product.class))).thenReturn(product2);

        // Scenario 2: Without initial stock
        Product createdProduct2 = productService.createProduct(productRequestDtoNoStock);
        assertNotNull(createdProduct2);
        assertEquals(product2.getId(), createdProduct2.getId());
        verify(productRepository, times(1)).save(any(Product.class));
        verify(stockClient, never()).addStock(any(StockDto.class));
    }

    // 2. Test for product creation failure (repository error)
    @Test
    @DisplayName("2. Should throw RuntimeException if product saving fails during creation")
    void createProduct_RepositoryFailure() {
        when(productRepository.save(any(Product.class))).thenThrow(new RuntimeException("DB error"));
        assertThrows(RuntimeException.class, () -> productService.createProduct(productRequestDtoWithStock));
        verify(productRepository).save(any(Product.class));
        verify(stockClient, never()).addStock(any(StockDto.class));
    }

    // 3. Test for getting all products with varied stock statuses
    @Test
    @DisplayName("3. Should return all products with stock information, handling missing/error stock records")
    void getAllProductsWithStock_Comprehensive() {
        List<Product> products = Arrays.asList(product1, product2);
        when(productRepository.findAll()).thenReturn(products);

        // product1 has stock, product2 stock service error
        when(stockClient.getStockByProductId(1L)).thenReturn(Optional.of(stockDto1));
        doThrow(new RuntimeException("Connection refused")).when(stockClient).getStockByProductId(2L);

        List<ProductResponseDto> result = productService.getAllProductsWithStock();

        assertNotNull(result);
        assertEquals(2, result.size());

        ProductResponseDto response1 = result.stream().filter(p -> p.getId().equals(1L)).findFirst().orElseThrow();
        assertEquals("In Stock", response1.getStockStatus());
        assertEquals(stockDto1, response1.getStockDetails());

        ProductResponseDto response2 = result.stream().filter(p -> p.getId().equals(2L)).findFirst().orElseThrow();
        assertEquals("Stock Service Error", response2.getStockStatus());
        assertNull(response2.getStockDetails());

        verify(productRepository).findAll();
        verify(stockClient, times(2)).getStockByProductId(anyLong()); // Both attempts for stock fetch
    }

    // 4. Test for getting a single product with stock info, and ProductNotFoundException
    @Test
    @DisplayName("4. Should return product by ID with stock info, or throw ProductNotFoundException")
    void getProductByIdWithStock_SuccessAndNotFound() {
        // Scenario 1: Product found with stock
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(stockClient.getStockByProductId(1L)).thenReturn(Optional.of(stockDto1));
        ProductResponseDto result = productService.getProductByIdWithStock(1L);
        assertNotNull(result);
        assertEquals(product1.getId(), result.getId());
        assertEquals("In Stock", result.getStockStatus());
        assertEquals(stockDto1, result.getStockDetails());
        verify(productRepository, times(1)).findById(1L);
        verify(stockClient, times(1)).getStockByProductId(1L);

        // Scenario 2: Product not found
        reset(productRepository, stockClient); // Reset for the next scenario
        when(productRepository.findById(anyLong())).thenReturn(Optional.empty());
        assertThrows(ProductNotFoundException.class, () -> productService.getProductByIdWithStock(99L));
        verify(productRepository, times(1)).findById(99L);
        verify(stockClient, never()).getStockByProductId(anyLong());
    }

    // 5. Test for successful product update, with stock update and stock creation (if not found)
    @Test
    @DisplayName("5. Should update product: with stock and create stock if not found")
    void updateProduct_SuccessAndCreateStock() {
        ProductRequestDto updateRequestDto = ProductRequestDto.builder()
                .name("Laptop Pro")
                .description("Updated powerful laptop")
                .price(1500.00)
                .imageUrl("laptop_pro.jpg")
                .initialStockQuantity(90) // Updated quantity
                .reorderLevel(15) // Updated reorder level
                .build();

        // Scenario 1: Successful update with existing stock
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(productRepository.save(any(Product.class))).thenReturn(product1);
        when(stockClient.updateStock(anyLong(), any(StockDto.class))).thenReturn(new StockDto());

        Product updatedProduct1 = productService.updateProduct(1L, updateRequestDto);
        assertNotNull(updatedProduct1);
        verify(productRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).save(any(Product.class));
        verify(stockClient, times(1)).updateStock(eq(1L), any(StockDto.class));
        verify(stockClient, never()).addStock(any(StockDto.class));

        // Reset for the next scenario
        reset(productRepository, stockClient);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(productRepository.save(any(Product.class))).thenReturn(product1);

        // Scenario 2: Update product and create stock if stock record not found during update
        doThrow(new FeignException.NotFound("Not Found", Request.create(Request.HttpMethod.PUT, "", new HashMap<>(), null, Charset.defaultCharset(), new RequestTemplate()), null, new HashMap<>()))
                .when(stockClient).updateStock(eq(1L), any(StockDto.class));
        when(stockClient.addStock(any(StockDto.class))).thenReturn(new StockDto());

        Product updatedProduct2 = productService.updateProduct(1L, updateRequestDto);
        assertNotNull(updatedProduct2);
        verify(productRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).save(any(Product.class));
        verify(stockClient, times(1)).updateStock(eq(1L), any(StockDto.class)); // update was attempted
        verify(stockClient, times(1)).addStock(any(StockDto.class)); // add was called after 404
    }

    // 6. Test for updating product when no stock changes are requested (initialStockQuantity is null)
    @Test
    @DisplayName("6. Should update product details only if stock quantity/reorder level are null")
    void updateProduct_NoStockUpdateRequested() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(productRepository.save(any(Product.class))).thenReturn(product1);

        Product updatedProduct = productService.updateProduct(1L, productRequestDtoNoStock);

        verify(productRepository).findById(1L);
        verify(productRepository).save(any(Product.class));
        verify(stockClient, never()).updateStock(anyLong(), any(StockDto.class));
        verify(stockClient, never()).addStock(any(StockDto.class));

        assertNotNull(updatedProduct);
        assertEquals(product1.getId(), updatedProduct.getId());
    }

    // 7. Test for product update failure (product not found, repository error)
    @Test
    @DisplayName("7. Should throw ProductNotFoundException or RuntimeException on update failure")
    void updateProduct_FailureScenarios() {
        // Scenario 1: Product to update not found
        when(productRepository.findById(anyLong())).thenReturn(Optional.empty());
        assertThrows(ProductNotFoundException.class, () -> productService.updateProduct(99L, productRequestDtoWithStock));
        verify(productRepository, times(1)).findById(99L);
        verify(productRepository, never()).save(any(Product.class));
        verify(stockClient, never()).updateStock(anyLong(), any(StockDto.class));

        // Scenario 2: Repository save fails during update
        reset(productRepository, stockClient);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(productRepository.save(any(Product.class))).thenThrow(new RuntimeException("DB error during save"));
        assertThrows(RuntimeException.class, () -> productService.updateProduct(1L, productRequestDtoWithStock));
        verify(productRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).save(any(Product.class));
        verify(stockClient, never()).updateStock(anyLong(), any(StockDto.class));
    }

    // 8. Test for successful product deletion, handling stock existence and stock service errors
    @Test
    @DisplayName("8. Should delete product: with stock, without stock, and gracefully on stock service error")
    void deleteProduct_Comprehensive() {
        // Scenario 1: Product exists and stock exists -> both deleted
        when(productRepository.existsById(1L)).thenReturn(true);
        when(stockClient.getStockByProductId(1L)).thenReturn(Optional.of(stockDto1));
        productService.deleteProduct(1L);
        verify(productRepository, times(1)).existsById(1L);
        verify(productRepository, times(1)).deleteById(1L);
        verify(stockClient, times(1)).getStockByProductId(1L);
        verify(stockClient, times(1)).deleteStock(1L);

        // Reset for next scenario
        reset(productRepository, stockClient);

        // Scenario 2: Product exists, but no stock record -> product deleted, no stock client call
        when(productRepository.existsById(2L)).thenReturn(true);
        when(stockClient.getStockByProductId(2L)).thenReturn(Optional.empty());
        productService.deleteProduct(2L);
        verify(productRepository, times(1)).existsById(2L);
        verify(productRepository, times(1)).deleteById(2L);
        verify(stockClient, times(1)).getStockByProductId(2L);
        verify(stockClient, never()).deleteStock(anyLong());

        // Reset for next scenario
        reset(productRepository, stockClient);

        // Scenario 3: Product exists, stock exists, but stock deletion fails with FeignException.NotFound -> product still deleted
        when(productRepository.existsById(1L)).thenReturn(true);
        when(stockClient.getStockByProductId(1L)).thenReturn(Optional.of(stockDto1));
        doThrow(new FeignException.NotFound("Not Found", Request.create(Request.HttpMethod.DELETE, "", new HashMap<>(), null, Charset.defaultCharset(), new RequestTemplate()), null, new HashMap<>()))
                .when(stockClient).deleteStock(1L);
        productService.deleteProduct(1L);
        verify(productRepository, times(1)).existsById(1L);
        verify(productRepository, times(1)).deleteById(1L);
        verify(stockClient, times(1)).getStockByProductId(1L);
        verify(stockClient, times(1)).deleteStock(1L); // Delete attempt is still made

        // Reset for next scenario
        reset(productRepository, stockClient);

        // Scenario 4: Product not found for deletion
        when(productRepository.existsById(99L)).thenReturn(false);
        assertThrows(ProductNotFoundException.class, () -> productService.deleteProduct(99L));
        verify(productRepository, times(1)).existsById(99L);
        verify(productRepository, never()).deleteById(anyLong());
        verify(stockClient, never()).getStockByProductId(anyLong());
        verify(stockClient, never()).deleteStock(anyLong());
    }
}