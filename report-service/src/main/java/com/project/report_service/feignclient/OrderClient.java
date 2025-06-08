package com.project.report_service.feignclient;

import com.project.report_service.dto.OrderDto; // DTO for order data
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat; // Import this!

@FeignClient(name = "order-service", url = "${order-service.url:http://localhost:8087}")
public interface OrderClient {

    @GetMapping("/api/orders")
    List<OrderDto> getAllOrders();

    // Assuming Order Service will add an endpoint to filter by date range
    @GetMapping("/api/orders/by-date-range")
    List<OrderDto> getOrdersByDateRange(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate, // Add @DateTimeFormat
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate); // Add @DateTimeFormat

    // Assuming Order Service will add an endpoint to sum quantity by product and date range
    @GetMapping("/api/orders/sum-quantity-by-product")
    Long sumQuantityByProductIdAndDateRange(
            @RequestParam("productId") Long productId,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate, // Add @DateTimeFormat
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate); // Add @DateTimeFormat
}