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
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
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
    private ProductRequestDto productRequestDto;
    private StockDto stockDto1;
    private StockDto stockDto2;

    @BeforeEach
    void setUp() {
        // Reset mocks before each test to ensure isolation
        reset(productRepository, stockClient);

        product1 = Product.builder()
                .id(1L)
                .name("Laptop")
                .description("Powerful laptop")
                .price(1200.00) // Reverted to double
                .imageUrl("laptop.jpg")
                .build();

        product2 = Product.builder()
                .id(2L)
                .name("Mouse")
                .description("Wireless mouse")
                .price(25.00) // Reverted to double
                .imageUrl("mouse.jpg")
                .build();

        productRequestDto = ProductRequestDto.builder()
                .name("Keyboard")
                .description("Mechanical keyboard")
                .price(75.00) // Reverted to double
                .imageUrl("keyboard.jpg")
                .initialStockQuantity(50)
                .reorderLevel(10)
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

    @Test
    @DisplayName("Should create a product and add initial stock successfully")
    void createProduct_Success() {
        // Mock productRepository.save to return the saved product with an ID
        when(productRepository.save(any(Product.class))).thenReturn(product1);

        // Call the service method
        Product createdProduct = productService.createProduct(productRequestDto);

        // Verify productRepository.save was called with the correct product details
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        Product capturedProduct = productCaptor.getValue();
        assertEquals(productRequestDto.getName(), capturedProduct.getName());
        assertEquals(productRequestDto.getDescription(), capturedProduct.getDescription());
        assertEquals(productRequestDto.getPrice(), capturedProduct.getPrice());
        assertEquals(productRequestDto.getImageUrl(), capturedProduct.getImageUrl());

        // Verify stockClient.addStock was called with the correct stock details
        ArgumentCaptor<StockDto> stockCaptor = ArgumentCaptor.forClass(StockDto.class);
        verify(stockClient).addStock(stockCaptor.capture());
        StockDto capturedStock = stockCaptor.getValue();
        assertEquals(product1.getId(), capturedStock.getProductId());
        assertEquals(productRequestDto.getInitialStockQuantity(), capturedStock.getQuantity());
        assertEquals(productRequestDto.getReorderLevel(), capturedStock.getReorderLevel());

        // Assert the returned product
        assertNotNull(createdProduct);
        assertEquals(product1.getId(), createdProduct.getId());
    }

    @Test
    @DisplayName("Should create a product without initial stock if quantity is null")
    void createProduct_NoInitialStock() {
        productRequestDto.setInitialStockQuantity(null); // No initial stock provided
        productRequestDto.setReorderLevel(null); // No reorder level provided

        when(productRepository.save(any(Product.class))).thenReturn(product1);

        Product createdProduct = productService.createProduct(productRequestDto);

        verify(productRepository).save(any(Product.class));
        verify(stockClient, never()).addStock(any(StockDto.class)); // Ensure stockClient.addStock is NOT called

        assertNotNull(createdProduct);
        assertEquals(product1.getId(), createdProduct.getId());
    }

    @Test
    @DisplayName("Should handle StockClient.addStock Conflict (409) gracefully during product creation")
    void createProduct_AddStockConflict() {
        when(productRepository.save(any(Product.class))).thenReturn(product1);
        // Simulate a 409 Conflict when adding stock
        doThrow(new FeignException.Conflict("Conflict", Request.create(Request.HttpMethod.POST, "", new HashMap<>(), null, Charset.defaultCharset(), new RequestTemplate()), null, new HashMap<>()))
                .when(stockClient).addStock(any(StockDto.class));

        Product createdProduct = productService.createProduct(productRequestDto);

        verify(productRepository).save(any(Product.class));
        verify(stockClient).addStock(any(StockDto.class)); // Still called, but throws exception

        assertNotNull(createdProduct); // Product should still be created
        assertEquals(product1.getId(), createdProduct.getId());
    }

    @Test
    @DisplayName("Should handle generic exception during StockClient.addStock gracefully during product creation")
    void createProduct_AddStockGenericError() {
        when(productRepository.save(any(Product.class))).thenReturn(product1);
        // Simulate a generic exception when adding stock
        doThrow(new RuntimeException("Stock service error")).when(stockClient).addStock(any(StockDto.class));

        Product createdProduct = productService.createProduct(productRequestDto);

        verify(productRepository).save(any(Product.class));
        verify(stockClient).addStock(any(StockDto.class)); // Still called, but throws exception

        assertNotNull(createdProduct); // Product should still be created
        assertEquals(product1.getId(), createdProduct.getId());
    }

    @Test
    @DisplayName("Should throw RuntimeException if product saving fails")
    void createProduct_RepositoryFailure() {
        when(productRepository.save(any(Product.class))).thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class, () -> productService.createProduct(productRequestDto));
        verify(productRepository).save(any(Product.class));
        verify(stockClient, never()).addStock(any(StockDto.class)); // Stock not added if product save fails
    }

    @Test
    @DisplayName("Should return all products successfully")
    void getAllProducts_Success() {
        List<Product> products = Arrays.asList(product1, product2);
        when(productRepository.findAll()).thenReturn(products);

        List<Product> result = productService.getAllProducts();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(product1));
        assertTrue(result.contains(product2));
        verify(productRepository).findAll();
    }

    @Test
    @DisplayName("Should throw RuntimeException if fetching all products fails")
    void getAllProducts_RepositoryFailure() {
        when(productRepository.findAll()).thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class, () -> productService.getAllProducts());
        verify(productRepository).findAll();
    }

    @Test
    @DisplayName("Should return all products with stock information successfully")
    void getAllProductsWithStock_Success() {
        List<Product> products = Arrays.asList(product1, product2);
        when(productRepository.findAll()).thenReturn(products);
        when(stockClient.getStockByProductId(1L)).thenReturn(Optional.of(stockDto1));
        when(stockClient.getStockByProductId(2L)).thenReturn(Optional.of(stockDto2));

        List<ProductResponseDto> result = productService.getAllProductsWithStock();

        assertNotNull(result);
        assertEquals(2, result.size());

        ProductResponseDto response1 = result.get(0);
        assertEquals(product1.getId(), response1.getId());
        assertEquals("In Stock", response1.getStockStatus());
        assertEquals(stockDto1, response1.getStockDetails());

        ProductResponseDto response2 = result.get(1);
        assertEquals(product2.getId(), response2.getId());
        assertEquals("Low Stock", response2.getStockStatus()); // quantity 5, reorder 10 -> low stock
        assertEquals(stockDto2, response2.getStockDetails());

        verify(productRepository).findAll();
        verify(stockClient, times(2)).getStockByProductId(anyLong());
    }

    @Test
    @DisplayName("Should handle no stock record found for some products when getting all products with stock")
    void getAllProductsWithStock_NoStockRecord() {
        List<Product> products = Arrays.asList(product1, product2);
        when(productRepository.findAll()).thenReturn(products);
        when(stockClient.getStockByProductId(1L)).thenReturn(Optional.of(stockDto1));
        // Simulate no stock record for product2
        when(stockClient.getStockByProductId(2L)).thenReturn(Optional.empty());

        List<ProductResponseDto> result = productService.getAllProductsWithStock();

        assertNotNull(result);
        assertEquals(2, result.size());

        ProductResponseDto response1 = result.get(0);
        assertEquals(product1.getId(), response1.getId());
        assertEquals("In Stock", response1.getStockStatus());
        assertEquals(stockDto1, response1.getStockDetails());

        ProductResponseDto response2 = result.get(1);
        assertEquals(product2.getId(), response2.getId());
        assertEquals("No Stock Record", response2.getStockStatus()); // Verify status when no stock record
        assertNull(response2.getStockDetails()); // No stock details

        verify(productRepository).findAll();
        verify(stockClient, times(2)).getStockByProductId(anyLong());
    }

    @Test
    @DisplayName("Should handle StockClient FeignException.NotFound when getting all products with stock")
    void getAllProductsWithStock_FeignNotFound() {
        List<Product> products = Arrays.asList(product1, product2);
        when(productRepository.findAll()).thenReturn(products);
        when(stockClient.getStockByProductId(1L)).thenReturn(Optional.of(stockDto1));
        // Simulate FeignException.NotFound for product2
        doThrow(new FeignException.NotFound("Not Found", Request.create(Request.HttpMethod.GET, "", new HashMap<>(), null, Charset.defaultCharset(), new RequestTemplate()), null, new HashMap<>()))
                .when(stockClient).getStockByProductId(2L);

        List<ProductResponseDto> result = productService.getAllProductsWithStock();

        assertNotNull(result);
        assertEquals(2, result.size());

        ProductResponseDto response1 = result.get(0);
        assertEquals(product1.getId(), response1.getId());
        assertEquals("In Stock", response1.getStockStatus());
        assertEquals(stockDto1, response1.getStockDetails());

        ProductResponseDto response2 = result.get(1);
        assertEquals(product2.getId(), response2.getId());
        assertEquals("No Stock Record", response2.getStockStatus()); // Verify status for Feign Not Found
        assertNull(response2.getStockDetails());

        verify(productRepository).findAll();
        verify(stockClient, times(2)).getStockByProductId(anyLong());
    }

    @Test
    @DisplayName("Should handle generic exception from StockClient when getting all products with stock")
    void getAllProductsWithStock_StockClientError() {
        List<Product> products = Arrays.asList(product1, product2);
        when(productRepository.findAll()).thenReturn(products);
        when(stockClient.getStockByProductId(1L)).thenReturn(Optional.of(stockDto1));
        // Simulate generic exception for product2
        doThrow(new RuntimeException("Connection refused")).when(stockClient).getStockByProductId(2L);

        List<ProductResponseDto> result = productService.getAllProductsWithStock();

        assertNotNull(result);
        assertEquals(2, result.size());

        ProductResponseDto response1 = result.get(0);
        assertEquals(product1.getId(), response1.getId());
        assertEquals("In Stock", response1.getStockStatus());
        assertEquals(stockDto1, response1.getStockDetails());

        ProductResponseDto response2 = result.get(1);
        assertEquals(product2.getId(), response2.getId());
        assertEquals("Stock Service Error", response2.getStockStatus()); // Verify status for generic error
        assertNull(response2.getStockDetails());

        verify(productRepository).findAll();
        verify(stockClient, times(2)).getStockByProductId(anyLong());
    }

    @Test
    @DisplayName("Should return product by ID successfully")
    void getProductById_Success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));

        Optional<Product> result = productService.getProductById(1L);

        assertTrue(result.isPresent());
        assertEquals(product1, result.get());
        verify(productRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw ProductNotFoundException if product by ID is not found")
    void getProductById_NotFound() {
        when(productRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.getProductById(99L));
        verify(productRepository).findById(99L);
    }

    @Test
    @DisplayName("Should throw RuntimeException if product by ID fetching fails")
    void getProductById_RepositoryFailure() {
        when(productRepository.findById(anyLong())).thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class, () -> productService.getProductById(1L));
        verify(productRepository).findById(1L);
    }

    @Test
    @DisplayName("Should return product by ID with stock information successfully")
    void getProductByIdWithStock_Success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(stockClient.getStockByProductId(1L)).thenReturn(Optional.of(stockDto1));

        ProductResponseDto result = productService.getProductByIdWithStock(1L);

        assertNotNull(result);
        assertEquals(product1.getId(), result.getId());
        assertEquals("In Stock", result.getStockStatus());
        assertEquals(stockDto1, result.getStockDetails());
        verify(productRepository).findById(1L);
        verify(stockClient).getStockByProductId(1L);
    }

    @Test
    @DisplayName("Should throw ProductNotFoundException if product by ID not found when getting with stock")
    void getProductByIdWithStock_ProductNotFound() {
        when(productRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.getProductByIdWithStock(99L));
        verify(productRepository).findById(99L);
        verify(stockClient, never()).getStockByProductId(anyLong()); // Stock client not called if product not found
    }

    @Test
    @DisplayName("Should update an existing product and its stock successfully (quantity and reorder level provided)")
    void updateProduct_SuccessWithStockUpdate() {
        ProductRequestDto updateRequestDto = ProductRequestDto.builder()
                .name("Laptop Pro")
                .description("Updated powerful laptop")
                .price(1500.00) // Reverted to double
                .imageUrl("laptop_pro.jpg")
                .initialStockQuantity(90) // Updated quantity
                .reorderLevel(15) // Updated reorder level
                .build();

        // Simulate existing product
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        // Simulate saving the updated product
        when(productRepository.save(any(Product.class))).thenReturn(product1);
        // Simulate stock update success
        when(stockClient.updateStock(anyLong(), any(StockDto.class))).thenReturn(new StockDto());

        Product updatedProduct = productService.updateProduct(1L, updateRequestDto);

        // Verify productRepository.findById and save
        verify(productRepository).findById(1L);
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        Product capturedProduct = productCaptor.getValue();
        assertEquals(updateRequestDto.getName(), capturedProduct.getName());
        assertEquals(updateRequestDto.getDescription(), capturedProduct.getDescription());
        assertEquals(updateRequestDto.getPrice(), capturedProduct.getPrice());
        assertEquals(updateRequestDto.getImageUrl(), capturedProduct.getImageUrl());

        // Verify stockClient.updateStock
        ArgumentCaptor<StockDto> stockCaptor = ArgumentCaptor.forClass(StockDto.class);
        verify(stockClient).updateStock(eq(1L), stockCaptor.capture());
        StockDto capturedStock = stockCaptor.getValue();
        assertEquals(updateRequestDto.getInitialStockQuantity(), capturedStock.getQuantity());
        assertEquals(updateRequestDto.getReorderLevel(), capturedStock.getReorderLevel());
        assertEquals(1L, capturedStock.getProductId()); // Ensure product ID is correctly set

        assertNotNull(updatedProduct);
        assertEquals(product1.getId(), updatedProduct.getId());
    }

    @Test
    @DisplayName("Should update product details only if stock quantity/reorder level are null")
    void updateProduct_NoStockUpdate() {
        ProductRequestDto updateRequestDto = ProductRequestDto.builder()
                .name("Laptop Pro Max")
                .description("Newest laptop")
                .price(1800.00) // Reverted to double
                .imageUrl("laptop_pro_max.jpg")
                .initialStockQuantity(null) // No quantity update
                .reorderLevel(null)         // No reorder level update
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(productRepository.save(any(Product.class))).thenReturn(product1);

        Product updatedProduct = productService.updateProduct(1L, updateRequestDto);

        verify(productRepository).findById(1L);
        verify(productRepository).save(any(Product.class));
        verify(stockClient, never()).updateStock(anyLong(), any(StockDto.class)); // Stock update should not be called
        verify(stockClient, never()).addStock(any(StockDto.class)); // Stock add should not be called

        assertNotNull(updatedProduct);
        assertEquals(product1.getId(), updatedProduct.getId());
    }

    @Test
    @DisplayName("Should update product and create stock if stock record not found during update")
    void updateProduct_CreateStockIfNotFound() {
        ProductRequestDto updateRequestDto = ProductRequestDto.builder()
                .name("Laptop Pro")
                .description("Updated powerful laptop")
                .price(1500.00) // Reverted to double
                .imageUrl("laptop_pro.jpg")
                .initialStockQuantity(90)
                .reorderLevel(15)
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(productRepository.save(any(Product.class))).thenReturn(product1);
        // Simulate 404 (Not Found) for updateStock, leading to addStock
        doThrow(new FeignException.NotFound("Not Found", Request.create(Request.HttpMethod.PUT, "", new HashMap<>(), null, Charset.defaultCharset(), new RequestTemplate()), null, new HashMap<>()))
                .when(stockClient).updateStock(eq(1L), any(StockDto.class));
        when(stockClient.addStock(any(StockDto.class))).thenReturn(new StockDto());

        Product updatedProduct = productService.updateProduct(1L, updateRequestDto);

        verify(productRepository).findById(1L);
        verify(productRepository).save(any(Product.class));
        verify(stockClient).updateStock(eq(1L), any(StockDto.class)); // update was attempted
        verify(stockClient).addStock(any(StockDto.class)); // add was called after 404

        assertNotNull(updatedProduct);
    }



    @Test
    @DisplayName("Should throw ProductNotFoundException if product to update is not found")
    void updateProduct_NotFound() {
        when(productRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.updateProduct(99L, productRequestDto));
        verify(productRepository).findById(99L);
        verify(productRepository, never()).save(any(Product.class));
        verify(stockClient, never()).updateStock(anyLong(), any(StockDto.class));
        verify(stockClient, never()).addStock(any(StockDto.class));
    }

    @Test
    @DisplayName("Should handle generic exception during product update save gracefully")
    void updateProduct_RepositorySaveFailure() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(productRepository.save(any(Product.class))).thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class, () -> productService.updateProduct(1L, productRequestDto));
        verify(productRepository).findById(1L);
        verify(productRepository).save(any(Product.class));
        verify(stockClient, never()).updateStock(anyLong(), any(StockDto.class)); // Stock not updated if product save fails
    }

    @Test
    @DisplayName("Should delete a product and its corresponding stock successfully")
    void deleteProduct_Success() {
        when(productRepository.existsById(1L)).thenReturn(true);
        when(stockClient.getStockByProductId(1L)).thenReturn(Optional.of(stockDto1)); // Simulate stock exists

        productService.deleteProduct(1L);

        verify(productRepository).existsById(1L);
        verify(productRepository).deleteById(1L);
        verify(stockClient).getStockByProductId(1L); // Verify stock existence check
        verify(stockClient).deleteStock(1L); // Verify stock deletion
    }

    @Test
    @DisplayName("Should delete a product even if no stock record exists for it")
    void deleteProduct_NoStockToDelete() {
        when(productRepository.existsById(1L)).thenReturn(true);
        when(stockClient.getStockByProductId(1L)).thenReturn(Optional.empty()); // Simulate no stock exists

        productService.deleteProduct(1L);

        verify(productRepository).existsById(1L);
        verify(productRepository).deleteById(1L);
        verify(stockClient).getStockByProductId(1L);
        verify(stockClient, never()).deleteStock(anyLong()); // Stock deletion should NOT be called
    }

    @Test
    @DisplayName("Should delete a product even if stock deletion fails with FeignException.NotFound")
    void deleteProduct_StockDeletionFeignNotFound() {
        when(productRepository.existsById(1L)).thenReturn(true);
        when(stockClient.getStockByProductId(1L)).thenReturn(Optional.of(stockDto1)); // Simulate stock exists
        // Simulate 404 (Not Found) for deleteStock
        doThrow(new FeignException.NotFound("Not Found", Request.create(Request.HttpMethod.DELETE, "", new HashMap<>(), null, Charset.defaultCharset(), new RequestTemplate()), null, new HashMap<>()))
                .when(stockClient).deleteStock(1L);

        productService.deleteProduct(1L);

        verify(productRepository).existsById(1L);
        verify(productRepository).deleteById(1L);
        verify(stockClient).getStockByProductId(1L);
        verify(stockClient).deleteStock(1L); // Deletion attempted, exception is caught
    }

    @Test
    @DisplayName("Should delete a product even if stock deletion fails with a generic exception")
    void deleteProduct_StockDeletionGenericError() {
        when(productRepository.existsById(1L)).thenReturn(true);
        when(stockClient.getStockByProductId(1L)).thenReturn(Optional.of(stockDto1)); // Simulate stock exists
        doThrow(new RuntimeException("Stock service unreachable"))
                .when(stockClient).deleteStock(1L);

        productService.deleteProduct(1L);

        verify(productRepository).existsById(1L);
        verify(productRepository).deleteById(1L);
        verify(stockClient).getStockByProductId(1L);
        verify(stockClient).deleteStock(1L); // Deletion attempted, exception is caught
    }

    @Test
    @DisplayName("Should throw ProductNotFoundException if product to delete is not found")
    void deleteProduct_NotFound() {
        when(productRepository.existsById(anyLong())).thenReturn(false);

        assertThrows(ProductNotFoundException.class, () -> productService.deleteProduct(99L));
        verify(productRepository).existsById(99L);
        verify(productRepository, never()).deleteById(anyLong());
        verify(stockClient, never()).getStockByProductId(anyLong());
        verify(stockClient, never()).deleteStock(anyLong());
    }

    @Test
    @DisplayName("Should throw RuntimeException if product deletion fails")
    void deleteProduct_RepositoryFailure() {
        when(productRepository.existsById(1L)).thenReturn(true);
        doThrow(new RuntimeException("DB error")).when(productRepository).deleteById(1L);

        assertThrows(RuntimeException.class, () -> productService.deleteProduct(1L));
        verify(productRepository).existsById(1L);
        verify(productRepository).deleteById(1L);
        verify(stockClient, never()).getStockByProductId(anyLong()); // Stock not touched if product delete fails
    }
}