package com.project.report_service.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.project.report_service.dto.InventoryReportDto;
import com.project.report_service.dto.OrderReportDto;
import com.project.report_service.dto.ReportRequest;
import com.project.report_service.dto.SupplierReportDto;

import com.project.report_service.dto.ProductDto;
import com.project.report_service.dto.OrderDto;
import com.project.report_service.dto.StockDto;
import com.project.report_service.dto.SupplierDto;

import com.project.report_service.exception.InvalidDateRangeException;
import com.project.report_service.feignclient.OrderClient;
import com.project.report_service.feignclient.ProductClient;
import com.project.report_service.feignclient.StockClient;
import com.project.report_service.feignclient.SupplierClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ReportServiceImpl implements ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportServiceImpl.class);

    @Autowired
    private ProductClient productClient;
    @Autowired
    private OrderClient orderClient;
    @Autowired
    private StockClient stockClient;
    @Autowired
    private SupplierClient supplierClient;


    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        log.debug("Validating date range: Start Date = {}, End Date = {}", startDate, endDate);
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            log.warn("Invalid date range detected: End date {} is before start date {}", endDate, startDate);
            throw new InvalidDateRangeException("End date cannot be before the start date.");
        }
        log.debug("Date range validation successful.");
    }

    @Override
    public List<InventoryReportDto> generateInventoryReport(ReportRequest request) {
        log.info("Generating inventory report for request: {}", request);
        try {
            validateDateRange(request.getStartDate(), request.getEndDate());

            List<ProductDto> products = productClient.getAllProducts();
            List<StockDto> stocks = stockClient.getAllStocks();
            Map<Long, StockDto> stockMap = stocks.stream()
                    .collect(Collectors.toMap(StockDto::getProductId, stock -> stock));

            log.debug("Found {} products and {} stock entries for inventory report.", products.size(), stocks.size());

            List<InventoryReportDto> reportData = new ArrayList<>();

            for (ProductDto product : products) {
                StockDto stock = stockMap.get(product.getId());
                InventoryReportDto dto = new InventoryReportDto();
                dto.setProductId(product.getId());
                dto.setProductName(product.getName());
                dto.setInitialStock(stock != null ? stock.getQuantity() : 0);

                Long stockRemoved = orderClient.sumQuantityByProductIdAndDateRange(
                        product.getId(), request.getStartDate(), request.getEndDate());
                dto.setStockRemoved(stockRemoved != null ? stockRemoved.intValue() : 0);

                dto.setStockAdded(0); // Assuming no stock added logic here, or would need another service call

                int finalStock = (stock != null ? stock.getQuantity() : 0) - dto.getStockRemoved() + dto.getStockAdded();
                dto.setFinalStock(Math.max(0, finalStock));
                dto.setReorderLevel(stock != null ? stock.getReorderLevel() : 0);
                dto.setIsLowStock(dto.getFinalStock() < dto.getReorderLevel());
                reportData.add(dto);
                log.debug("Processed inventory data for product ID {}: {}", product.getId(), dto);
            }

            // --- FILTERING LOGIC FOR INVENTORY REPORT (minStock) ---
            List<InventoryReportDto> filteredReportData = reportData; // Start with all processed data

            if (request.getParameters() != null && request.getParameters().containsKey("minStock")) {
                Object minStockObj = request.getParameters().get("minStock");
                Integer minStock = null;

                if (minStockObj instanceof Integer) {
                    minStock = (Integer) minStockObj;
                } else if (minStockObj instanceof String) {
                    try {
                        minStock = Integer.parseInt((String) minStockObj);
                    } catch (NumberFormatException e) {
                        log.warn("minStock parameter found but cannot be parsed as an integer: {}. Not applying filter.", minStockObj);
                    }
                }

                if (minStock != null) {
                    final Integer filterValue = minStock; // Make it effectively final for lambda
                    filteredReportData = reportData.stream()
                            .filter(inventoryDto -> inventoryDto.getFinalStock() >= filterValue)
                            .collect(Collectors.toList());
                    log.debug("Filtered inventory report by minStock: {}. Resulting entries: {}", filterValue, filteredReportData.size());
                } else {
                    log.warn("minStock parameter found but is not a valid number. Not applying filter.");
                }
            }
            // --- END NEW FILTERING LOGIC FOR INVENTORY REPORT ---

            log.info("Inventory report generated successfully with {} entries (after filtering).", filteredReportData.size());
            return filteredReportData;
        } catch (InvalidDateRangeException e) {
            log.error("Failed to generate inventory report due to invalid date range: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("An unexpected error occurred while generating inventory report: {}", e.getMessage(), e);
            throw new RuntimeException("Error generating inventory report", e);
        }
    }

    @Override
    public OrderReportDto generateOrderReport(ReportRequest request) {
        log.info("Generating order report for request: {}", request);
        try {
            validateDateRange(request.getStartDate(), request.getEndDate());

            List<OrderDto> orders = orderClient.getOrdersByDateRange(request.getStartDate(), request.getEndDate());
            log.debug("Initially found {} orders for order report between {} and {}.", orders.size(), request.getStartDate(), request.getEndDate());

            // --- FILTERING LOGIC FOR ORDER REPORT (status, customerId) ---
            String statusFilter = request.getParameters() != null ? (String) request.getParameters().get("status") : null;

            Long customerIdFilter = null;
            if (request.getParameters() != null && request.getParameters().containsKey("customerId")) {
                Object customerIdObj = request.getParameters().get("customerId");
                if (customerIdObj instanceof Integer) {
                    customerIdFilter = ((Integer) customerIdObj).longValue();
                } else if (customerIdObj instanceof Long) {
                    customerIdFilter = (Long) customerIdObj;
                } else if (customerIdObj instanceof String) { // Handle string input for robustness
                    try {
                        customerIdFilter = Long.parseLong((String) customerIdObj);
                    } catch (NumberFormatException e) {
                        log.warn("customerId parameter found but cannot be parsed as a number: {}. Not applying customer ID filter.", customerIdObj);
                    }
                }
            }

            // FIX: Introduce a final variable for customerIdFilter before lambda usage
            final Long finalCustomerIdFilter = customerIdFilter;


            // Helper to strip quotes from status string if present (e.g., "'Delivered'" -> "Delivered")
            java.util.function.Function<String, String> stripQuotes = s -> {
                if (s != null && s.length() > 1 && s.startsWith("'") && s.endsWith("'")) {
                    return s.substring(1, s.length() - 1);
                }
                return s;
            };

            List<OrderDto> filteredOrders = orders.stream()
                    .filter(order -> {
                        boolean matchesStatus = true;
                        if (statusFilter != null && !statusFilter.isEmpty()) {
                            String processedStatusFilter = stripQuotes.apply(statusFilter);
                            String processedOrderStatus = stripQuotes.apply(order.getStatus());
                            matchesStatus = processedStatusFilter.equalsIgnoreCase(processedOrderStatus);
                        }

                        boolean matchesCustomerId = true;
                        // Use the effectively final variable here
                        if (finalCustomerIdFilter != null) {
                            matchesCustomerId = finalCustomerIdFilter.equals(order.getCustomerId());
                        }
                        return matchesStatus && matchesCustomerId;
                    })
                    .collect(Collectors.toList());

            log.debug("After applying parameters (status: {}, customerId: {}), filtered down to {} orders.",
                    statusFilter, finalCustomerIdFilter, filteredOrders.size());
            // --- END FILTERING LOGIC FOR ORDER REPORT ---


            // All subsequent calculations should use 'filteredOrders'
            long totalOrders = filteredOrders.size();

            long pendingOrders = filteredOrders.stream()
                    .filter(order -> "Pending".equalsIgnoreCase(stripQuotes.apply(order.getStatus())))
                    .count();
            long shippedOrders = filteredOrders.stream()
                    .filter(order -> "Shipped".equalsIgnoreCase(stripQuotes.apply(order.getStatus())))
                    .count();
            long deliveredOrders = filteredOrders.stream()
                    .filter(order -> "Delivered".equalsIgnoreCase(stripQuotes.apply(order.getStatus())))
                    .count();

            List<ProductDto> allProducts = productClient.getAllProducts();
            Map<Long, ProductDto> productMap = allProducts.stream()
                    .collect(Collectors.toMap(ProductDto::getId, product -> product));

            double totalRevenue = filteredOrders.stream()
                    .mapToDouble(order -> {
                        ProductDto product = productMap.get(order.getProductId());
                        if (product != null && product.getPrice() != null) {
                            return order.getQuantity() * product.getPrice();
                        } else {
                            log.warn("Product or price not found for product ID {} in order ID {}. Skipping from revenue calculation.", order.getProductId(), order.getOrderId());
                            return 0.0;
                        }
                    }).sum();

            List<OrderReportDto.TopSellingProductDto> topSellingProducts = getTopSellingProducts(filteredOrders, productMap);

            OrderReportDto orderReportDto = new OrderReportDto();
            orderReportDto.setTotalOrders(totalOrders);
            orderReportDto.setPendingOrders(pendingOrders);
            orderReportDto.setShippedOrders(shippedOrders);
            orderReportDto.setDeliveredOrders(deliveredOrders);
            orderReportDto.setTotalRevenue(totalRevenue);
            orderReportDto.setTopSellingProducts(topSellingProducts);

            log.info("Order report generated successfully. Total orders: {}, Total Revenue: {}", totalOrders, totalRevenue);
            return orderReportDto;
        } catch (InvalidDateRangeException e) {
            log.error("Failed to generate order report due to invalid date range: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("An unexpected error occurred while generating order report: {}", e.getMessage(), e);
            throw new RuntimeException("Error generating order report", e);
        }
    }


    @Override
    public List<SupplierReportDto> generateSupplierReport(ReportRequest request) {
        log.info("Generating supplier report for request: {}", request);
        try {
            validateDateRange(request.getStartDate(), request.getEndDate());

            List<SupplierDto> suppliers = supplierClient.getAllSuppliers();
            log.debug("Found {} suppliers for supplier report.", suppliers.size());

            List<SupplierReportDto> supplierReportData = new ArrayList<>();

            for (SupplierDto supplier : suppliers) {
                Long totalQuantitySupplied = getTotalQuantitySupplied(supplier.getSupplierId(), request.getStartDate(), request.getEndDate());
                Long productsSuppliedCount = supplierClient.getProductsSuppliedCountBySupplier(supplier.getSupplierId());

                SupplierReportDto dto = new SupplierReportDto();
                dto.setSupplierId(supplier.getSupplierId());
                dto.setSupplierName(supplier.getName());
                dto.setProductsSupplied(productsSuppliedCount);
                dto.setTotalQuantitySupplied(totalQuantitySupplied);
                supplierReportData.add(dto);
                log.debug("Processed supplier data for supplier ID {}: {}", supplier.getSupplierId(), dto);
            }

            // --- FILTERING LOGIC FOR SUPPLIER REPORT (minProductsSupplied) ---
            List<SupplierReportDto> filteredReportData = supplierReportData; // Start with all processed data

            if (request.getParameters() != null && request.getParameters().containsKey("minProductsSupplied")) {
                Object minProductsSuppliedObj = request.getParameters().get("minProductsSupplied");
                Long minProductsSupplied = null;

                if (minProductsSuppliedObj instanceof Integer) { // Handle both Integer and Long types
                    minProductsSupplied = ((Integer) minProductsSuppliedObj).longValue();
                } else if (minProductsSuppliedObj instanceof Long) {
                    minProductsSupplied = (Long) minProductsSuppliedObj;
                } else if (minProductsSuppliedObj instanceof String) { // Handle string input for robustness
                    try {
                        minProductsSupplied = Long.parseLong((String) minProductsSuppliedObj);
                    } catch (NumberFormatException e) {
                        log.warn("minProductsSupplied parameter found but cannot be parsed as a number: {}. Not applying filter.", minProductsSuppliedObj);
                    }
                }

                if (minProductsSupplied != null) {
                    final Long filterValue = minProductsSupplied;
                    filteredReportData = supplierReportData.stream()
                            .filter(supplierDto -> supplierDto.getProductsSupplied() >= filterValue)
                            .collect(Collectors.toList());
                    log.debug("Filtered supplier report by minProductsSupplied: {}. Resulting entries: {}", filterValue, filteredReportData.size());
                } else {
                    log.warn("minProductsSupplied parameter found but is not a valid number. Not applying filter.");
                }
            }
            // --- END FILTERING LOGIC FOR SUPPLIER REPORT ---

            log.info("Supplier report generated successfully with {} entries (after filtering).", filteredReportData.size());
            return filteredReportData;
        } catch (InvalidDateRangeException e) {
            log.error("Failed to generate supplier report due to invalid date range: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("An unexpected error occurred while generating supplier report: {}", e.getMessage(), e);
            throw new RuntimeException("Error generating supplier report", e);
        }
    }

    private List<OrderReportDto.TopSellingProductDto> getTopSellingProducts(List<OrderDto> orders, Map<Long, ProductDto> productMap) {
        log.debug("Calculating top selling products from {} orders.", orders.size());
        Map<Long, Long> unitsSoldPerProduct = orders.stream()
                .collect(Collectors.groupingBy(OrderDto::getProductId, Collectors.summingLong(OrderDto::getQuantity)));

        return unitsSoldPerProduct.entrySet().stream()
                .map(entry -> {
                    Long productId = entry.getKey();
                    Long unitsSold = entry.getValue();
                    ProductDto product = productMap.get(productId);
                    String productName = (product != null) ? product.getName() : "Unknown Product";
                    Double totalRevenue = (product != null && product.getPrice() != null) ? unitsSold * product.getPrice() : 0.0;

                    OrderReportDto.TopSellingProductDto dto = new OrderReportDto.TopSellingProductDto();
                    dto.setProductName(productName);
                    dto.setUnitsSold(unitsSold);
                    dto.setTotalRevenue(totalRevenue);
                    return dto;
                })
                .sorted((d1, d2) -> Long.compare(d2.getUnitsSold(), d1.getUnitsSold()))
                .collect(Collectors.toList());
    }

    private Long getTotalQuantitySupplied(Long supplierId, LocalDate startDate, LocalDate endDate) {
        log.debug("Calculating total quantity supplied by supplier ID {} between {} and {}.", supplierId, startDate, endDate);

        List<OrderDto> orders = orderClient.getOrdersByDateRange(startDate, endDate);
        List<ProductDto> products = productClient.getAllProducts();
        Map<Long, ProductDto> productMap = products.stream()
                .collect(Collectors.toMap(ProductDto::getId, p -> p));

        long totalQuantity = orders.stream()
                .filter(order -> {
                    ProductDto product = productMap.get(order.getProductId());
                    return product != null && product.getSupplierId() != null && product.getSupplierId().equals(supplierId);
                })
                .mapToLong(OrderDto::getQuantity)
                .sum();
        log.debug("Total quantity supplied by supplier ID {}: {}", supplierId, totalQuantity);
        return totalQuantity;
    }
}