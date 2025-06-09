package com.project.report_service.service;

import com.project.report_service.dto.*;
import com.project.report_service.exception.InvalidDateRangeException;
import com.project.report_service.feignclient.OrderClient;
import com.project.report_service.feignclient.ProductClient;
import com.project.report_service.feignclient.StockClient;
import com.project.report_service.feignclient.SupplierClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReportServiceImplTest {

    @Mock
    private ProductClient productClient;

    @Mock
    private OrderClient orderClient;

    @Mock
    private StockClient stockClient;

    @Mock
    private SupplierClient supplierClient;

    @InjectMocks
    private ReportServiceImpl reportService;

    private ReportRequest inventoryReportRequest;
    private ReportRequest orderReportRequest;
    private ReportRequest supplierReportRequest;
    private ReportRequest invalidDateRangeRequest;

    @BeforeEach
    void setUp() {
        // Common ReportRequest setup
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);

        inventoryReportRequest = new ReportRequest("inventory", startDate, endDate, null);

        Map<String, Object> orderParams = new HashMap<>();
        orderParams.put("status", "Delivered");
        orderParams.put("customerId", 101);
        orderReportRequest = new ReportRequest("order", startDate, endDate, orderParams);

        Map<String, Object> supplierParams = new HashMap<>();
        supplierParams.put("country", "USA");
        supplierReportRequest = new ReportRequest("supplier", startDate, endDate, supplierParams);

        invalidDateRangeRequest = new ReportRequest("inventory", endDate, startDate, null);
    }

    @Test
    @DisplayName("Should generate inventory report successfully")
    void shouldGenerateInventoryReportSuccessfully() {
        // Arrange
        ProductDto product1 = new ProductDto(1L, "Laptop", "Description", 1200.0, 50, "url1", 1L);
        ProductDto product2 = new ProductDto(2L, "Mouse", "Description", 25.0, 100, "url2", 2L);
        List<ProductDto> products = Arrays.asList(product1, product2);

        StockDto stock1 = new StockDto(1L, 40, 10, false);
        StockDto stock2 = new StockDto(2L, 90, 20, false);
        List<StockDto> stocks = Arrays.asList(stock1, stock2);

        when(productClient.getAllProducts()).thenReturn(products);
        when(stockClient.getAllStocks()).thenReturn(stocks);
        when(orderClient.sumQuantityByProductIdAndDateRange(eq(1L), any(LocalDate.class), any(LocalDate.class))).thenReturn(5L); // 5 Laptops sold
        when(orderClient.sumQuantityByProductIdAndDateRange(eq(2L), any(LocalDate.class), any(LocalDate.class))).thenReturn(10L); // 10 Mice sold

        // Act
        List<InventoryReportDto> result = reportService.generateInventoryReport(inventoryReportRequest);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());

        InventoryReportDto dto1 = result.get(0);
        assertEquals(1L, dto1.getProductId());
        assertEquals("Laptop", dto1.getProductName());
        assertEquals(40, dto1.getInitialStock());
        assertEquals(5, dto1.getStockRemoved());
        assertEquals(35, dto1.getFinalStock()); // 40 - 5
        assertEquals(0, dto1.getStockAdded());
        assertEquals(10, dto1.getReorderLevel());
        assertFalse(dto1.getIsLowStock());

        InventoryReportDto dto2 = result.get(1);
        assertEquals(2L, dto2.getProductId());
        assertEquals("Mouse", dto2.getProductName());
        assertEquals(90, dto2.getInitialStock());
        assertEquals(10, dto2.getStockRemoved());
        assertEquals(80, dto2.getFinalStock()); // 90 - 10
        assertEquals(0, dto2.getStockAdded());
        assertEquals(20, dto2.getReorderLevel());
        assertFalse(dto2.getIsLowStock());

        verify(productClient, times(1)).getAllProducts();
        verify(stockClient, times(1)).getAllStocks();
        verify(orderClient, times(2)).sumQuantityByProductIdAndDateRange(anyLong(), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    @DisplayName("Should handle empty product and stock data for inventory report")
    void shouldHandleEmptyProductAndStockDataForInventoryReport() {
        // Arrange
        when(productClient.getAllProducts()).thenReturn(Collections.emptyList());
        when(stockClient.getAllStocks()).thenReturn(Collections.emptyList());

        // Act
        List<InventoryReportDto> result = reportService.generateInventoryReport(inventoryReportRequest);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(productClient, times(1)).getAllProducts();
        verify(stockClient, times(1)).getAllStocks();
        verify(orderClient, never()).sumQuantityByProductIdAndDateRange(anyLong(), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    @DisplayName("Should throw InvalidDateRangeException for inventory report with invalid date range")
    void shouldThrowInvalidDateRangeExceptionForInventoryReport() {
        // Act & Assert
        assertThrows(InvalidDateRangeException.class, () -> reportService.generateInventoryReport(invalidDateRangeRequest));

        verify(productClient, never()).getAllProducts();
        verify(stockClient, never()).getAllStocks();
        verify(orderClient, never()).sumQuantityByProductIdAndDateRange(anyLong(), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    @DisplayName("Should generate order report successfully with filters")
    void shouldGenerateOrderReportSuccessfullyWithFilters() {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);

        OrderDto order1 = new OrderDto(1L, 101L, 10L, 2, LocalDate.of(2024, 1, 10), "Delivered");
        OrderDto order2 = new OrderDto(2L, 102L, 11L, 1, LocalDate.of(2024, 1, 15), "Pending");
        OrderDto order3 = new OrderDto(3L, 101L, 10L, 3, LocalDate.of(2024, 1, 20), "Delivered");
        OrderDto order4 = new OrderDto(4L, 101L, 12L, 1, LocalDate.of(2024, 1, 25), "Shipped"); // Should be filtered out by status

        List<OrderDto> allOrders = Arrays.asList(order1, order2, order3, order4);

        ProductDto product10 = new ProductDto(10L, "Tablet", "Description", 300.0, 20, "url", 1L);
        ProductDto product11 = new ProductDto(11L, "Keyboard", "Description", 75.0, 30, "url", 2L);
        ProductDto product12 = new ProductDto(12L, "Monitor", "Description", 200.0, 15, "url", 1L);
        List<ProductDto> allProducts = Arrays.asList(product10, product11, product12);

        when(orderClient.getOrdersByDateRange(startDate, endDate)).thenReturn(allOrders);
        when(productClient.getAllProducts()).thenReturn(allProducts);

        // Act
        OrderReportDto result = reportService.generateOrderReport(orderReportRequest);

        // Assert
        assertNotNull(result);
        assertEquals(2L, result.getTotalOrders()); // Only order1 and order3 match customerId 101 AND status "Delivered"
        assertEquals(0L, result.getPendingOrders());
        assertEquals(0L, result.getShippedOrders());
        assertEquals(2L, result.getDeliveredOrders());
        assertEquals(1500.0, result.getTotalRevenue()); // (2 * 300) + (3 * 300) = 600 + 900 = 1500

        List<OrderReportDto.TopSellingProductDto> topSellingProducts = result.getTopSellingProducts();
        assertNotNull(topSellingProducts);
        assertEquals(1, topSellingProducts.size()); // Only product 10 was sold by customer 101 with status delivered

        OrderReportDto.TopSellingProductDto topProduct = topSellingProducts.get(0);
        assertEquals("Tablet", topProduct.getProductName());
        assertEquals(5L, topProduct.getUnitsSold());
        assertEquals(1500.0, topProduct.getTotalRevenue());

        verify(orderClient, times(1)).getOrdersByDateRange(startDate, endDate);
        verify(productClient, times(1)).getAllProducts();
    }

    @Test
    @DisplayName("Should generate order report successfully without filters")
    void shouldGenerateOrderReportSuccessfullyWithoutFilters() {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        ReportRequest noFilterOrderRequest = new ReportRequest("order", startDate, endDate, null);

        OrderDto order1 = new OrderDto(1L, 101L, 10L, 2, LocalDate.of(2024, 1, 10), "Delivered");
        OrderDto order2 = new OrderDto(2L, 102L, 11L, 1, LocalDate.of(2024, 1, 15), "Pending");
        OrderDto order3 = new OrderDto(3L, 101L, 10L, 3, LocalDate.of(2024, 1, 20), "Delivered");
        OrderDto order4 = new OrderDto(4L, 103L, 12L, 1, LocalDate.of(2024, 1, 25), "Shipped");

        List<OrderDto> allOrders = Arrays.asList(order1, order2, order3, order4);

        ProductDto product10 = new ProductDto(10L, "Tablet", "Description", 300.0, 20, "url", 1L);
        ProductDto product11 = new ProductDto(11L, "Keyboard", "Description", 75.0, 30, "url", 2L);
        ProductDto product12 = new ProductDto(12L, "Monitor", "Description", 200.0, 15, "url", 1L);
        List<ProductDto> allProducts = Arrays.asList(product10, product11, product12);

        when(orderClient.getOrdersByDateRange(startDate, endDate)).thenReturn(allOrders);
        when(productClient.getAllProducts()).thenReturn(allProducts);

        // Act
        OrderReportDto result = reportService.generateOrderReport(noFilterOrderRequest);

        // Assert
        assertNotNull(result);
        assertEquals(4L, result.getTotalOrders());
        assertEquals(1L, result.getPendingOrders());
        assertEquals(1L, result.getShippedOrders());
        assertEquals(2L, result.getDeliveredOrders());
        assertEquals(1775.0, result.getTotalRevenue()); // Corrected expected value

        List<OrderReportDto.TopSellingProductDto> topSellingProducts = result.getTopSellingProducts();
        assertNotNull(topSellingProducts);
        assertEquals(3, topSellingProducts.size());

        // Verify Tablet (highest units sold)
        assertEquals("Tablet", topSellingProducts.get(0).getProductName());
        assertEquals(5L, topSellingProducts.get(0).getUnitsSold());
        assertEquals(1500.0, topSellingProducts.get(0).getTotalRevenue());

        // Instead of asserting exact indices for items with same units sold,
        // verify their presence and correctness in the list.
        assertTrue(topSellingProducts.stream()
                .anyMatch(p -> "Monitor".equals(p.getProductName()) && p.getUnitsSold() == 1L && p.getTotalRevenue() == 200.0));
        assertTrue(topSellingProducts.stream()
                .anyMatch(p -> "Keyboard".equals(p.getProductName()) && p.getUnitsSold() == 1L && p.getTotalRevenue() == 75.0));

        verify(orderClient, times(1)).getOrdersByDateRange(startDate, endDate);
        verify(productClient, times(1)).getAllProducts();
    }


    @Test
    @DisplayName("Should throw InvalidDateRangeException for order report with invalid date range")
    void shouldThrowInvalidDateRangeExceptionForOrderReport() {
        // Act & Assert
        assertThrows(InvalidDateRangeException.class, () -> reportService.generateOrderReport(invalidDateRangeRequest));

        verify(orderClient, never()).getOrdersByDateRange(any(LocalDate.class), any(LocalDate.class));
        verify(productClient, never()).getAllProducts();
    }

    @Test
    @DisplayName("Should generate supplier report successfully")
    void shouldGenerateSupplierReportSuccessfully() {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);

        SupplierDto supplier1 = new SupplierDto(1L, "Supplier A", "contactA", Arrays.asList("Prod1", "Prod2"));
        SupplierDto supplier2 = new SupplierDto(2L, "Supplier B", "contactB", Arrays.asList("Prod3"));
        List<SupplierDto> suppliers = Arrays.asList(supplier1, supplier2);

        ProductDto product1 = new ProductDto(10L, "Product X", "desc", 10.0, 100, "url", 1L);
        ProductDto product2 = new ProductDto(11L, "Product Y", "desc", 20.0, 50, "url", 1L);
        ProductDto product3 = new ProductDto(12L, "Product Z", "desc", 30.0, 200, "url", 2L);
        List<ProductDto> products = Arrays.asList(product1, product2, product3);

        OrderDto order1 = new OrderDto(101L, 1L, 10L, 5, LocalDate.of(2024, 1, 5), "Delivered");
        OrderDto order2 = new OrderDto(102L, 2L, 11L, 3, LocalDate.of(2024, 1, 10), "Pending");
        OrderDto order3 = new OrderDto(103L, 3L, 12L, 8, LocalDate.of(2024, 1, 15), "Delivered");
        List<OrderDto> orders = Arrays.asList(order1, order2, order3);


        when(supplierClient.getAllSuppliers()).thenReturn(suppliers);
        when(supplierClient.getProductsSuppliedCountBySupplier(1L)).thenReturn(2L); // For Supplier A
        when(supplierClient.getProductsSuppliedCountBySupplier(2L)).thenReturn(1L); // For Supplier B

        // Mock the internal calls within getTotalQuantitySupplied
        when(orderClient.getOrdersByDateRange(startDate, endDate)).thenReturn(orders);
        when(productClient.getAllProducts()).thenReturn(products);


        // Act
        List<SupplierReportDto> result = reportService.generateSupplierReport(supplierReportRequest);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());

        SupplierReportDto dto1 = result.get(0);
        assertEquals(1L, dto1.getSupplierId());
        assertEquals("Supplier A", dto1.getSupplierName());
        assertEquals(2L, dto1.getProductsSupplied());
        assertEquals(8L, dto1.getTotalQuantitySupplied()); // 5 (product 10) + 3 (product 11) = 8

        SupplierReportDto dto2 = result.get(1);
        assertEquals(2L, dto2.getSupplierId());
        assertEquals("Supplier B", dto2.getSupplierName());
        assertEquals(1L, dto2.getProductsSupplied());
        assertEquals(8L, dto2.getTotalQuantitySupplied()); // 8 (product 12)

        verify(supplierClient, times(1)).getAllSuppliers();
        verify(supplierClient, times(2)).getProductsSuppliedCountBySupplier(anyLong());
        verify(orderClient, times(2)).getOrdersByDateRange(startDate, endDate); // Called once per supplier inside the loop
        verify(productClient, times(2)).getAllProducts(); // Called once per supplier inside the loop
    }

    @Test
    @DisplayName("Should handle empty supplier data for supplier report")
    void shouldHandleEmptySupplierDataForSupplierReport() {
        // Arrange
        when(supplierClient.getAllSuppliers()).thenReturn(Collections.emptyList());

        // Act
        List<SupplierReportDto> result = reportService.generateSupplierReport(supplierReportRequest);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(supplierClient, times(1)).getAllSuppliers();
        verify(supplierClient, never()).getProductsSuppliedCountBySupplier(anyLong());
        verify(orderClient, never()).getOrdersByDateRange(any(LocalDate.class), any(LocalDate.class));
        verify(productClient, never()).getAllProducts();
    }

    @Test
    @DisplayName("Should throw InvalidDateRangeException for supplier report with invalid date range")
    void shouldThrowInvalidDateRangeExceptionForSupplierReport() {
        // Act & Assert
        assertThrows(InvalidDateRangeException.class, () -> reportService.generateSupplierReport(invalidDateRangeRequest));

        verify(supplierClient, never()).getAllSuppliers();
        verify(orderClient, never()).getOrdersByDateRange(any(LocalDate.class), any(LocalDate.class));
        verify(productClient, never()).getAllProducts();
    }
}