//package com.project.order_service.service;
//
//import com.project.order_service.entity.Order;
//import com.project.order_service.exception.OrderNotFoundException;
//
//import java.util.List;
//import java.time.LocalDate;
//
//public interface OrderService {
//
//    Order createOrder(Order order);
//    List<Order> getAllOrders();
//    Order getOrderById(Long id);
//    void deleteOrder(Long id);
//    Order updateStatus(Long id, String status);
//    Order updateOrder(Long id, Order orderDetails) throws OrderNotFoundException;
//    List<Order> getOrdersByDateRange(LocalDate startDate, LocalDate endDate);
//    Long sumQuantityByProductIdAndDateRange(Long productId, LocalDate startDate, LocalDate endDate);
//    Double getOrderProductPrice(Long orderId) throws OrderNotFoundException;
//    Double calculateOrderTotalPrice(Long orderId) throws OrderNotFoundException;
//}

package com.project.order_service.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map; // Ensure this is imported if used in controller/service
import com.project.order_service.entity.Order;
import com.project.order_service.exception.OrderNotFoundException;
import com.project.order_service.dto.OrderResponseDto; // NEW IMPORT

public interface OrderService {
    Order createOrder(Order order);
    List<OrderResponseDto> getAllOrders(); // MODIFIED RETURN TYPE
    Order updateOrder(Long id, Order orderDetails) throws OrderNotFoundException;
    Order getOrderById(Long id);
    void deleteOrder(Long id);
    Order updateStatus(Long id, String status);
    List<Order> getOrdersByDateRange(LocalDate startDate, LocalDate endDate);
    Long sumQuantityByProductIdAndDateRange(Long productId, LocalDate startDate, LocalDate endDate);
    Double getOrderProductPrice(Long orderId) throws OrderNotFoundException;
    Double calculateOrderTotalPrice(Long orderId) throws OrderNotFoundException;
}