package com.project.order_service.service;

import com.project.order_service.dto.OrderResponseDto;
import com.project.order_service.dto.ProductDto;
import com.project.order_service.dto.StockDto;
import com.project.order_service.entity.Order;
import com.project.order_service.exception.OrderNotFoundException;
import com.project.order_service.feignclient.ProductClient;
import com.project.order_service.feignclient.StockClient;
import com.project.order_service.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the OrderServiceImpl.
 * Uses Mockito to mock dependencies (OrderRepository, ProductClient, StockClient)
 * to test the service layer logic in isolation.
 *
 * This test does not interact with a real database or external services,
 * making it independent of the datasource and Feign client URLs in application.properties.
 */
@ExtendWith(MockitoExtension.class) // Enables Mockito annotations
public class OrderServiceTest {

    @Mock // Creates a mock instance of OrderRepository
    private OrderRepository orderRepository;

    @Mock // Creates a mock instance of ProductClient
    private ProductClient productClient;

    @Mock // Creates a mock instance of StockClient
    private StockClient stockClient;

    @InjectMocks // Injects the mocks into OrderServiceImpl
    private OrderServiceImpl orderService;

    private Order sampleOrder;
    private ProductDto sampleProductDto;
    private StockDto sampleStockDto;

    @BeforeEach
    void setUp() {
        // Initialize common test data before each test
        sampleOrder = new Order(1L, 101L, 201L, 5, LocalDate.now(), "Pending");
        sampleProductDto = new ProductDto(201L, "Test Product", "Description", 100.0, 10, "url");
        sampleStockDto = new StockDto(201L, 10, 2, false);
    }

    // --- createOrder Tests ---
    @Test
    void createOrder_shouldCreateOrderSuccessfully_whenProductAndStockAreAvailable() {
        // Mock behavior of external clients
        when(productClient.getProductById(sampleOrder.getProductId())).thenReturn(Optional.of(sampleProductDto));
        when(stockClient.getStockByProductId(sampleOrder.getProductId())).thenReturn(sampleStockDto);
        // Mock behavior of repository save
        when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);
        when(stockClient.updateStock(eq(sampleOrder.getProductId()), any(StockDto.class))).thenReturn(sampleStockDto); // Mock updateStock

        Order createdOrder = orderService.createOrder(sampleOrder);

        // Assertions
        assertThat(createdOrder).isNotNull();
        assertThat(createdOrder.getOrderId()).isEqualTo(1L);
        assertThat(createdOrder.getStatus()).isEqualTo("Pending");
        assertThat(createdOrder.getOrderDate()).isEqualTo(LocalDate.now());

        // Verify interactions with mocks
        verify(productClient, times(1)).getProductById(sampleOrder.getProductId());
        verify(stockClient, times(1)).getStockByProductId(sampleOrder.getProductId());
        verify(stockClient, times(1)).updateStock(eq(sampleOrder.getProductId()), any(StockDto.class));
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void createOrder_shouldThrowException_whenProductNotFound() {
        // Mock product client to return empty optional
        when(productClient.getProductById(sampleOrder.getProductId())).thenReturn(Optional.empty());

        // Assert that OrderNotFoundException is thrown
        OrderNotFoundException thrown = assertThrows(OrderNotFoundException.class, () -> {
            orderService.createOrder(sampleOrder);
        });

        assertThat(thrown.getMessage()).contains("Product with ID " + sampleOrder.getProductId() + " not found.");

        // Verify interactions
        verify(productClient, times(1)).getProductById(sampleOrder.getProductId());
        verify(stockClient, never()).getStockByProductId(anyLong()); // Should not call stock service
        verify(orderRepository, never()).save(any(Order.class));     // Should not save order
    }

    @Test
    void createOrder_shouldThrowException_whenStockInfoNotFound() {
        // Mock product client to return product
        when(productClient.getProductById(sampleOrder.getProductId())).thenReturn(Optional.of(sampleProductDto));
        // Mock stock client to return null for stock info
        when(stockClient.getStockByProductId(sampleOrder.getProductId())).thenReturn(null);

        // Assert that RuntimeException is thrown
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            orderService.createOrder(sampleOrder);
        });

        assertThat(thrown.getMessage()).contains("Stock information not available for product ID: " + sampleOrder.getProductId());

        // Verify interactions
        verify(productClient, times(1)).getProductById(sampleOrder.getProductId());
        verify(stockClient, times(1)).getStockByProductId(sampleOrder.getProductId());
        verify(stockClient, never()).updateStock(anyLong(), any(StockDto.class));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createOrder_shouldThrowException_whenInsufficientStock() {
        sampleOrder.setQuantity(15); // Request more than available stock (10)
        // Mock product client to return product
        when(productClient.getProductById(sampleOrder.getProductId())).thenReturn(Optional.of(sampleProductDto));
        // Mock stock client to return stock with quantity 10
        when(stockClient.getStockByProductId(sampleOrder.getProductId())).thenReturn(sampleStockDto);

        // Assert that RuntimeException is thrown
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            orderService.createOrder(sampleOrder);
        });

        assertThat(thrown.getMessage()).contains("Insufficient stock for product ID: " + sampleOrder.getProductId());

        // Verify interactions
        verify(productClient, times(1)).getProductById(sampleOrder.getProductId());
        verify(stockClient, times(1)).getStockByProductId(sampleOrder.getProductId());
        verify(stockClient, never()).updateStock(anyLong(), any(StockDto.class)); // Should not update stock
        verify(orderRepository, never()).save(any(Order.class));                 // Should not save order
    }

    @Test
    void createOrder_shouldThrowException_whenOrderRepositoryFailsToSave() {
        // Mock successful product and stock checks
        when(productClient.getProductById(sampleOrder.getProductId())).thenReturn(Optional.of(sampleProductDto));
        when(stockClient.getStockByProductId(sampleOrder.getProductId())).thenReturn(sampleStockDto);
        when(stockClient.updateStock(eq(sampleOrder.getProductId()), any(StockDto.class))).thenReturn(sampleStockDto);
        // Mock repository save to throw an exception
        when(orderRepository.save(any(Order.class))).thenThrow(new RuntimeException("DB save error"));

        // Assert that RuntimeException is thrown
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            orderService.createOrder(sampleOrder);
        });

        assertThat(thrown.getMessage()).contains("Failed to create order: DB save error");

        // Verify interactions
        verify(productClient, times(1)).getProductById(sampleOrder.getProductId());
        verify(stockClient, times(1)).getStockByProductId(sampleOrder.getProductId());
        verify(stockClient, times(1)).updateStock(eq(sampleOrder.getProductId()), any(StockDto.class)); // Stock was updated
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    // --- getAllOrders Tests ---
    @Test
    void getAllOrders_shouldReturnListOfOrders() {
        List<Order> orders = Arrays.asList(sampleOrder, new Order(2L, 102L, 202L, 1, LocalDate.now(), "Completed"));
        when(orderRepository.findAll()).thenReturn(orders);

        List<Order> result = orderService.getAllOrders()
                .stream()
                .map(orderResponseDto -> {
                    Order order = new Order();
                    order.setOrderId(orderResponseDto.getOrderId());
                    order.setCustomerId(orderResponseDto.getCustomerId());
                    order.setProductId(orderResponseDto.getProductId());
                    order.setQuantity(orderResponseDto.getQuantity());
                    order.setOrderDate(orderResponseDto.getOrderDate());
                    order.setStatus(orderResponseDto.getStatus());
                    return order;
                })
                .toList();

        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(sampleOrder, new Order(2L, 102L, 202L, 1, LocalDate.now(), "Completed"));
        verify(orderRepository, times(1)).findAll();
    }

    @Test
    void getAllOrders_shouldReturnEmptyList_whenNoOrdersExist() {
        when(orderRepository.findAll()).thenReturn(Collections.emptyList());

        List<Order> result = orderService.getAllOrders()
                .stream()
                .map(orderResponseDto -> {
                    Order order = new Order();
                    order.setOrderId(orderResponseDto.getOrderId());
                    order.setCustomerId(orderResponseDto.getCustomerId());
                    order.setProductId(orderResponseDto.getProductId());
                    order.setQuantity(orderResponseDto.getQuantity());
                    order.setOrderDate(orderResponseDto.getOrderDate());
                    order.setStatus(orderResponseDto.getStatus());
                    return order;
                })
                .toList();

        assertThat(result).isEmpty();
        verify(orderRepository, times(1)).findAll();
    }

    // --- getOrderById Tests ---
    @Test
    void getOrderById_shouldReturnOrder_whenFound() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));

        Order foundOrder = orderService.getOrderById(1L);

        assertThat(foundOrder).isEqualTo(sampleOrder);
        verify(orderRepository, times(1)).findById(1L);
    }

    @Test
    void getOrderById_shouldThrowOrderNotFoundException_whenNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.getOrderById(99L));
        verify(orderRepository, times(1)).findById(99L);
    }

    // --- deleteOrder Tests ---
    @Test
    void deleteOrder_shouldDeleteOrderSuccessfully() {
        when(orderRepository.existsById(1L)).thenReturn(true);
        doNothing().when(orderRepository).deleteById(1L);

        assertDoesNotThrow(() -> orderService.deleteOrder(1L));

        verify(orderRepository, times(1)).existsById(1L);
        verify(orderRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteOrder_shouldThrowOrderNotFoundException_whenOrderDoesNotExist() {
        when(orderRepository.existsById(99L)).thenReturn(false);

        assertThrows(OrderNotFoundException.class, () -> orderService.deleteOrder(99L));

        verify(orderRepository, times(1)).existsById(99L);
        verify(orderRepository, never()).deleteById(anyLong());
    }

    // --- updateStatus Tests ---
    @Test
    void updateStatus_shouldUpdateOrderStatusSuccessfully() {
        String newStatus = "Delivered";
        Order existingOrder = new Order(1L, 101L, 201L, 5, LocalDate.now(), "Pending");
        Order updatedOrder = new Order(1L, 101L, 201L, 5, LocalDate.now(), newStatus);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(updatedOrder);

        Order result = orderService.updateStatus(1L, newStatus);

        assertThat(result.getStatus()).isEqualTo(newStatus);
        verify(orderRepository, times(1)).findById(1L);
        verify(orderRepository, times(1)).save(existingOrder); // Verify save was called with the modified existingOrder
    }

    @Test
    void updateStatus_shouldThrowOrderNotFoundException_whenOrderDoesNotExist() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.updateStatus(99L, "Delivered"));

        verify(orderRepository, times(1)).findById(99L);
        verify(orderRepository, never()).save(any(Order.class));
    }

    // --- updateOrder Tests ---
    @Test
    void updateOrder_shouldUpdateAllOrderDetailsSuccessfully() {
        Long orderId = 1L;
        Order existingOrder = new Order(orderId, 101L, 201L, 5, LocalDate.of(2024, 1, 1), "Pending");
        Order updatedDetails = new Order(orderId, 102L, 202L, 10, LocalDate.of(2024, 2, 1), "Shipped");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(updatedDetails);

        Order result = orderService.updateOrder(orderId, updatedDetails);

        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(orderId);
        assertThat(result.getCustomerId()).isEqualTo(updatedDetails.getCustomerId());
        assertThat(result.getProductId()).isEqualTo(updatedDetails.getProductId());
        assertThat(result.getQuantity()).isEqualTo(updatedDetails.getQuantity());
        assertThat(result.getOrderDate()).isEqualTo(updatedDetails.getOrderDate());
        assertThat(result.getStatus()).isEqualTo(updatedDetails.getStatus());

        verify(orderRepository, times(1)).findById(orderId);
        verify(orderRepository, times(1)).save(existingOrder); // Ensure save is called with the modified existingOrder
    }

    @Test
    void updateOrder_shouldThrowOrderNotFoundException_whenOrderToUpdateDoesNotExist() {
        Long orderId = 99L;
        Order updatedDetails = new Order(orderId, 102L, 202L, 10, LocalDate.of(2024, 2, 1), "Shipped");

        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.updateOrder(orderId, updatedDetails));

        verify(orderRepository, times(1)).findById(orderId);
        verify(orderRepository, never()).save(any(Order.class));
    }

    // --- getOrdersByDateRange Tests ---
    @Test
    void getOrdersByDateRange_shouldReturnOrdersInGivenRange() {
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 1, 31);
        List<Order> ordersInDateRange = Arrays.asList(
                new Order(1L, 101L, 201L, 2, LocalDate.of(2025, 1, 15), "Pending"),
                new Order(2L, 102L, 202L, 1, LocalDate.of(2025, 1, 20), "Completed")
        );

        when(orderRepository.findByOrderDateBetween(startDate, endDate)).thenReturn(ordersInDateRange);

        List<Order> result = orderService.getOrdersByDateRange(startDate, endDate);

        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrderElementsOf(ordersInDateRange);
        verify(orderRepository, times(1)).findByOrderDateBetween(startDate, endDate);
    }

    @Test
    void getOrdersByDateRange_shouldReturnEmptyList_whenNoOrdersInGivenRange() {
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 1, 31);

        when(orderRepository.findByOrderDateBetween(startDate, endDate)).thenReturn(Collections.emptyList());

        List<Order> result = orderService.getOrdersByDateRange(startDate, endDate);

        assertThat(result).isEmpty();
        verify(orderRepository, times(1)).findByOrderDateBetween(startDate, endDate);
    }

    // --- sumQuantityByProductIdAndDateRange Tests ---
    @Test
    void sumQuantityByProductIdAndDateRange_shouldReturnCorrectSum() {
        Long productId = 201L;
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 1, 31);
        Long expectedSum = 15L;

        when(orderRepository.sumQuantityByProductIdAndOrderDateBetween(productId, startDate, endDate)).thenReturn(expectedSum);

        Long result = orderService.sumQuantityByProductIdAndDateRange(productId, startDate, endDate);

        assertThat(result).isEqualTo(expectedSum);
        verify(orderRepository, times(1)).sumQuantityByProductIdAndOrderDateBetween(productId, startDate, endDate);
    }

    @Test
    void sumQuantityByProductIdAndDateRange_shouldReturnZero_whenNoOrdersFound() {
        Long productId = 201L;
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 1, 31);

        when(orderRepository.sumQuantityByProductIdAndOrderDateBetween(productId, startDate, endDate)).thenReturn(null);

        Long result = orderService.sumQuantityByProductIdAndDateRange(productId, startDate, endDate);

        assertThat(result).isEqualTo(0L); // Service returns 0L if repository returns null
        verify(orderRepository, times(1)).sumQuantityByProductIdAndOrderDateBetween(productId, startDate, endDate);
    }
}