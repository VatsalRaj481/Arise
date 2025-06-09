package com.project.stock_service.service;

import com.project.stock_service.exception.InvalidInputException;
import com.project.stock_service.exception.ResourceNotFoundException;
import com.project.stock_service.exception.StockAlreadyExistsException;
import com.project.stock_service.model.Stock;
import com.project.stock_service.repository.StockRepository;
import com.project.stock_service.feignclient.ProductClient;
import com.project.stock_service.dto.ProductDto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private ProductClient productClient; // Mock the Feign client

    @InjectMocks
    private StockServiceImpl stockService; // Inject the service to be tested

    private Stock stock1;
    private Stock stock2;
    private ProductDto existingProductDto;

    @BeforeEach
    void setUp() {
        stock1 = new Stock(1L, 100, 10);
        stock2 = new Stock(2L, 50, 5);
        existingProductDto = new ProductDto(1L, "Test Product", "Description", 99.99, 10, "url"); // Mock product data
    }

    // --- addStock Tests ---

    @Test
    @DisplayName("addStock should successfully add a new stock item when product exists and stock doesn't")
    void addStock_shouldAddStock_whenProductExistsAndStockDoesNotExist() {
        // Arrange
        when(productClient.getProductById(stock1.getProductId())).thenReturn(Optional.of(existingProductDto)); // Product exists
        when(stockRepository.existsById(stock1.getProductId())).thenReturn(false); // Stock does not exist
        when(stockRepository.save(any(Stock.class))).thenReturn(stock1); // Save returns the stock

        // Act
        Stock result = stockService.addStock(stock1);

        // Assert
        assertNotNull(result);
        assertEquals(stock1.getProductId(), result.getProductId());
        assertEquals(stock1.getQuantity(), result.getQuantity());
        assertEquals(stock1.getReorderLevel(), result.getReorderLevel());

        verify(productClient, times(1)).getProductById(stock1.getProductId());
        verify(stockRepository, times(1)).existsById(stock1.getProductId());
        verify(stockRepository, times(1)).save(stock1);
    }

    @Test
    @DisplayName("addStock should throw InvalidInputException when productId is null")
    void addStock_shouldThrowInvalidInputException_whenProductIdIsNull() {
        // Arrange
        Stock stockWithNullProductId = new Stock(null, 100, 10);

        // Act & Assert
        InvalidInputException exception = assertThrows(InvalidInputException.class, () ->
                stockService.addStock(stockWithNullProductId)
        );

        assertEquals("Product ID cannot be null for a new stock item.", exception.getMessage());
        verifyNoInteractions(productClient); // Should not call product client
        verifyNoInteractions(stockRepository); // Should not call repository
    }

    @Test
    @DisplayName("addStock should throw StockAlreadyExistsException when stock with productId already exists")
    void addStock_shouldThrowStockAlreadyExistsException_whenStockAlreadyExists() {
        // Arrange
        when(productClient.getProductById(stock1.getProductId())).thenReturn(Optional.of(existingProductDto)); // Product exists
        when(stockRepository.existsById(stock1.getProductId())).thenReturn(true); // Stock already exists

        // Act & Assert
        StockAlreadyExistsException exception = assertThrows(StockAlreadyExistsException.class, () ->
                stockService.addStock(stock1)
        );

        assertEquals("Stock item with Product ID " + stock1.getProductId() + " already exists.", exception.getMessage());
        verify(productClient, times(1)).getProductById(stock1.getProductId());
        verify(stockRepository, times(1)).existsById(stock1.getProductId());
        verify(stockRepository, never()).save(any(Stock.class)); // Should not save
    }

    @Test
    @DisplayName("addStock should throw RuntimeException with InvalidInputException as cause when product does not exist in Product Service")
    void addStock_shouldThrowRuntimeException_whenProductDoesNotExistInProductService() {
        // Arrange
        when(productClient.getProductById(stock1.getProductId())).thenReturn(Optional.empty()); // Product not found

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                stockService.addStock(stock1)
        );

        // Assert the outer RuntimeException message
        assertEquals("Failed to validate product with Product Service.", exception.getMessage());
        // Assert the cause is the expected InvalidInputException
        assertTrue(exception.getCause() instanceof InvalidInputException);
        assertEquals("Product with ID " + stock1.getProductId() + " does not exist.", exception.getCause().getMessage());

        verify(productClient, times(1)).getProductById(stock1.getProductId());
        verify(stockRepository, never()).existsById(anyLong()); // Should not call existsById
        verify(stockRepository, never()).save(any(Stock.class)); // Should not save
    }

    @Test
    @DisplayName("addStock should throw RuntimeException when Product Client communication fails")
    void addStock_shouldThrowRuntimeException_whenProductClientCommunicationFails() {
        // Arrange
        // Simulate a Feign client exception (e.g., network error, service down)
        when(productClient.getProductById(stock1.getProductId())).thenThrow(new RuntimeException("Connection refused"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                stockService.addStock(stock1)
        );

        assertEquals("Failed to validate product with Product Service.", exception.getMessage());
        assertTrue(exception.getCause() instanceof RuntimeException);
        assertEquals("Connection refused", exception.getCause().getMessage());


        verify(productClient, times(1)).getProductById(stock1.getProductId());
        verify(stockRepository, never()).existsById(anyLong());
        verify(stockRepository, never()).save(any(Stock.class));
    }


    // --- updateStock Tests ---

    @Test
    @DisplayName("updateStock should successfully update an existing stock item")
    void updateStock_shouldUpdateStockSuccessfully() {
        // Arrange
        Stock updatedStockDetails = new Stock(1L, 150, 15);
        when(productClient.getProductById(stock1.getProductId())).thenReturn(Optional.of(existingProductDto)); // Product exists
        when(stockRepository.findById(stock1.getProductId())).thenReturn(Optional.of(stock1)); // Existing stock found
        when(stockRepository.save(any(Stock.class))).thenReturn(updatedStockDetails); // Save returns updated stock

        // Act
        Stock result = stockService.updateStock(stock1.getProductId(), updatedStockDetails);

        // Assert
        assertNotNull(result);
        assertEquals(stock1.getProductId(), result.getProductId());
        assertEquals(updatedStockDetails.getQuantity(), result.getQuantity());
        assertEquals(updatedStockDetails.getReorderLevel(), result.getReorderLevel());

        verify(productClient, times(1)).getProductById(stock1.getProductId());
        verify(stockRepository, times(1)).findById(stock1.getProductId());
        verify(stockRepository, times(1)).save(stock1); // It saves the *modified* existing stock object
    }

    @Test
    @DisplayName("updateStock should throw InvalidInputException if path productId and body productId mismatch")
    void updateStock_shouldThrowInvalidInputException_whenProductIdMismatch() {
        // Arrange
        Long pathProductId = 1L;
        Stock stockDetailsWithMismatch = new Stock(2L, 150, 15); // Different productId in body

        // Act & Assert
        InvalidInputException exception = assertThrows(InvalidInputException.class, () ->
                stockService.updateStock(pathProductId, stockDetailsWithMismatch)
        );

        assertEquals("Product ID in path (" + pathProductId + ") does not match Product ID in request body (" + stockDetailsWithMismatch.getProductId() + ").", exception.getMessage());
        verifyNoInteractions(productClient);
        verifyNoInteractions(stockRepository);
    }

    @Test
    @DisplayName("updateStock should throw ResourceNotFoundException if stock not found in repository")
    void updateStock_shouldThrowResourceNotFoundException_whenStockNotFoundInRepository() {
        // Arrange
        Long nonExistentProductId = 99L;
        Stock updatedStockDetails = new Stock(99L, 150, 15);
        // Even if product exists, if stock isn't found later, it's a ResourceNotFound
        when(productClient.getProductById(nonExistentProductId)).thenReturn(Optional.of(existingProductDto));
        when(stockRepository.findById(nonExistentProductId)).thenReturn(Optional.empty()); // Stock not found

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                stockService.updateStock(nonExistentProductId, updatedStockDetails)
        );

        assertEquals("Stock item not found with Product ID: " + nonExistentProductId, exception.getMessage());
        verify(productClient, times(1)).getProductById(nonExistentProductId);
        verify(stockRepository, times(1)).findById(nonExistentProductId);
        verify(stockRepository, never()).save(any(Stock.class));
    }

    @Test
    @DisplayName("updateStock should throw RuntimeException with ResourceNotFoundException as cause when product does not exist in Product Service")
    void updateStock_shouldThrowRuntimeException_whenProductDoesNotExistInProductService() {
        // Arrange
        when(productClient.getProductById(stock1.getProductId())).thenReturn(Optional.empty()); // Product not found

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                stockService.updateStock(stock1.getProductId(), stock1)
        );

        // Assert the outer RuntimeException message
        assertEquals("Failed to validate product with Product Service during update.", exception.getMessage());
        // Assert the cause is the expected ResourceNotFoundException
        assertTrue(exception.getCause() instanceof ResourceNotFoundException);
        assertEquals("Product with ID " + stock1.getProductId() + " does not exist. Cannot update stock.", exception.getCause().getMessage());

        verify(productClient, times(1)).getProductById(stock1.getProductId());
        verify(stockRepository, never()).findById(anyLong()); // Should not call findById
        verify(stockRepository, never()).save(any(Stock.class));
    }

    @Test
    @DisplayName("updateStock should throw RuntimeException when Product Client communication fails")
    void updateStock_shouldThrowRuntimeException_whenProductClientFails() {
        // Arrange
        when(productClient.getProductById(stock1.getProductId())).thenThrow(new RuntimeException("Service Unavailable"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                stockService.updateStock(stock1.getProductId(), stock1)
        );

        assertEquals("Failed to validate product with Product Service during update.", exception.getMessage());
        assertTrue(exception.getCause() instanceof RuntimeException);
        assertEquals("Service Unavailable", exception.getCause().getMessage());

        verify(productClient, times(1)).getProductById(stock1.getProductId());
        verify(stockRepository, never()).findById(anyLong());
        verify(stockRepository, never()).save(any(Stock.class));
    }


    // --- deleteStock Tests ---

    @Test
    @DisplayName("deleteStock should successfully delete an existing stock item")
    void deleteStock_shouldDeleteStockSuccessfully() {
        // Arrange
        when(stockRepository.existsById(stock1.getProductId())).thenReturn(true);

        // Act
        stockService.deleteStock(stock1.getProductId());

        // Assert
        verify(stockRepository, times(1)).existsById(stock1.getProductId());
        verify(stockRepository, times(1)).deleteById(stock1.getProductId());
    }

    @Test
    @DisplayName("deleteStock should throw ResourceNotFoundException if stock not found")
    void deleteStock_shouldThrowResourceNotFoundException_whenStockNotFound() {
        // Arrange
        Long nonExistentProductId = 99L;
        when(stockRepository.existsById(nonExistentProductId)).thenReturn(false);

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                stockService.deleteStock(nonExistentProductId)
        );

        assertEquals("Stock item not found with Product ID: " + nonExistentProductId + ". Cannot delete.", exception.getMessage());
        verify(stockRepository, times(1)).existsById(nonExistentProductId);
        verify(stockRepository, never()).deleteById(anyLong()); // Should not attempt to delete
    }

    // --- getAllStock Tests ---

    @Test
    @DisplayName("getAllStock should return all stock items")
    void getAllStock_shouldReturnAllStocks() {
        // Arrange
        List<Stock> stocks = Arrays.asList(stock1, stock2);
        when(stockRepository.findAll()).thenReturn(stocks);

        // Act
        List<Stock> result = stockService.getAllStock();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(stock1, result.get(0));
        assertEquals(stock2, result.get(1));
        verify(stockRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAllStock should return an empty list if no stocks exist")
    void getAllStock_shouldReturnEmptyList_whenNoStocksExist() {
        // Arrange
        when(stockRepository.findAll()).thenReturn(Arrays.asList());

        // Act
        List<Stock> result = stockService.getAllStock();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(stockRepository, times(1)).findAll();
    }

    // --- getStockByProductId Tests ---

    @Test
    @DisplayName("getStockByProductId should return stock when found")
    void getStockByProductId_shouldReturnStock_whenFound() {
        // Arrange
        when(stockRepository.findById(stock1.getProductId())).thenReturn(Optional.of(stock1));

        // Act
        Stock result = stockService.getStockByProductId(stock1.getProductId());

        // Assert
        assertNotNull(result);
        assertEquals(stock1.getProductId(), result.getProductId());
        verify(stockRepository, times(1)).findById(stock1.getProductId());
    }

    @Test
    @DisplayName("getStockByProductId should throw ResourceNotFoundException when stock not found")
    void getStockByProductId_shouldThrowResourceNotFoundException_whenNotFound() {
        // Arrange
        Long nonExistentProductId = 99L;
        when(stockRepository.findById(nonExistentProductId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                stockService.getStockByProductId(nonExistentProductId)
        );

        assertEquals("Stock item not found with Product ID: " + nonExistentProductId, exception.getMessage());
        verify(stockRepository, times(1)).findById(nonExistentProductId);
    }
}