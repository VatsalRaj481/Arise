package com.project.order_service.dto;

import com.project.order_service.entity.Order; // Import your Order entity
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponseDto {
    private Long orderId;
    private Long customerId;
    private Long productId;
    private String productName; // NEW FIELD: Product Name
    private Integer quantity;
    private LocalDate orderDate;
    private String status;

}