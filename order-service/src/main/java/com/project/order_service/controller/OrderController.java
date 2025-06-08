//package com.project.order_service.controller;
//
//import com.project.order_service.entity.Order;
//import com.project.order_service.service.OrderService;
//import com.project.order_service.exception.OrderNotFoundException;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.time.LocalDate;
//import java.util.List;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/orders")
//public class OrderController {
//
//    @Autowired
//    private OrderService orderService;
//
//    // Retrieves a list of orders within a specified date range.
//    // Endpoint: GET /api/orders/by-date-range?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD
//    @GetMapping("/by-date-range")
//    public ResponseEntity<List<Order>> getOrdersByDateRange(
//            @RequestParam("startDate") LocalDate startDate,
//            @RequestParam("endDate") LocalDate endDate) {
//        List<Order> orders = orderService.getOrdersByDateRange(startDate, endDate);
//        return ResponseEntity.ok(orders);
//    }
//
//    // Calculates and returns the sum of quantities for a specific product within a date range.
//    // Endpoint: GET /api/orders/sum-quantity-by-product?productId={id}&startDate=YYYY-MM-DD&endDate=YYYY-MM-DD
//    @GetMapping("/sum-quantity-by-product")
//    public ResponseEntity<Long> sumQuantityByProductIdAndDateRange(
//            @RequestParam("productId") Long productId,
//            @RequestParam("startDate") LocalDate startDate,
//            @RequestParam("endDate") LocalDate endDate) {
//        Long totalQuantity = orderService.sumQuantityByProductIdAndDateRange(productId, startDate, endDate);
//        // It's good practice to handle null if sum returns no quantity, though Long usually defaults to 0
//        return ResponseEntity.ok(totalQuantity != null ? totalQuantity : 0L);
//    }
//
//    // Endpoint: GET /api/orders
//    @GetMapping
//    public ResponseEntity<List<Order>> getAllOrders() {
//        List<Order> orders = orderService.getAllOrders();
//        return ResponseEntity.ok(orders);
//    }
//
//    // Retrieves a single order by its ID.
//    // Endpoint: GET /api/orders/{id}
//    @GetMapping("/{id}")
//    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
//        try {
//            Order order = orderService.getOrderById(id);
//            return ResponseEntity.ok(order);
//        } catch (OrderNotFoundException e) {
//            return ResponseEntity.notFound().build();
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // Return 500 Internal Server Error
//        }
//    }
//
//    // Creates a new order.
//    // Endpoint: POST /api/orders
//    // Request Body: {"customerId": 102, "productId": 3, "quantity": 1, "orderDate": "2025-06-01", "status": "Pending"}
//    @PostMapping
//    @ResponseStatus(HttpStatus.CREATED)
//    public Order createOrder(@RequestBody Order order) {
//        return orderService.createOrder(order);
//    }
//
//    // Updates an existing order.
//    // Endpoint: PUT /api/orders/{id}
//    // Request Body: {"orderId": 1, "customerId": 101, "productId": 2, "quantity": 3, "orderDate": "2025-05-30", "status": "Shipped"}
//    @PutMapping("/{id}")
//    public ResponseEntity<Order> updateOrder(@PathVariable Long id, @RequestBody Order orderDetails) {
//        try {
//            if (!id.equals(orderDetails.getOrderId())) {
//                return ResponseEntity.badRequest().build();
//            }
//            Order updatedOrder = orderService.updateOrder(id, orderDetails);
//            return ResponseEntity.ok(updatedOrder);
//        } catch (OrderNotFoundException e) {
//            return ResponseEntity.notFound().build();
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//    }
//
//    // Deletes an order by its ID.
//    // Endpoint: DELETE /api/orders/{id}
//    @DeleteMapping("/{id}")
//    @ResponseStatus(HttpStatus.NO_CONTENT)
//    public void deleteOrder(@PathVariable Long id) {
//        orderService.deleteOrder(id);
//    }
//
//    @GetMapping("/{orderId}/product-price")  //order id
//    public ResponseEntity<Double> getProductPriceForOrder(@PathVariable Long orderId) {
//        //log.info("Fetching product price for order ID: {}", orderId);
//        try {
//            Double price = orderService.getOrderProductPrice(orderId);
//            return ResponseEntity.ok(price);
//        } catch (OrderNotFoundException e) {
//            //log.warn("Order or associated product not found for product price retrieval: {}", orderId);
//            return ResponseEntity.notFound().build();
//        } catch (Exception e) {
//            //log.error("Error fetching product price for order ID {}: {}", orderId, e.getMessage(), e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//    }
//
//    // Calculates and returns the total price of a specific order.
//    // Endpoint: GET /api/orders/7/total-price
//    @GetMapping("/{orderId}/total-price")
//    public ResponseEntity<Double> getOrderTotalPrice(@PathVariable Long orderId) {
//        //log.info("Calculating total price for order ID: {}", orderId);
//        try {
//            Double totalPrice = orderService.calculateOrderTotalPrice(orderId);
//            return ResponseEntity.ok(totalPrice);
//        } catch (OrderNotFoundException e) {
//            //log.warn("Order or associated product not found for total price calculation: {}", orderId);
//            return ResponseEntity.notFound().build();
//        } catch (Exception e) {
//            //log.error("Error calculating total price for order ID {}: {}", orderId, e.getMessage(), e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//    }
//    @PutMapping("/{id}/status")
//    public ResponseEntity<Order> updateOrderStatus(@PathVariable Long id, @RequestBody Map<String, String> statusMap) {
//        try {
//            String status = statusMap.get("status"); // Extract status correctly
//            Order updatedOrder = orderService.updateStatus(id, status); // Send only the raw status
//            return ResponseEntity.ok(updatedOrder);
//        } catch (OrderNotFoundException e) {
//            return ResponseEntity.notFound().build();
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//    }
//
//
//
//}

package com.project.order_service.controller;

import com.project.order_service.entity.Order;
import com.project.order_service.service.OrderService;
import com.project.order_service.exception.OrderNotFoundException;
import com.project.order_service.dto.OrderResponseDto; // NEW IMPORT

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    // Retrieves a list of orders within a specified date range.
    // Endpoint: GET /api/orders/by-date-range?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD
    @GetMapping("/by-date-range")
    public ResponseEntity<List<Order>> getOrdersByDateRange(
            @RequestParam("startDate") LocalDate startDate,
            @RequestParam("endDate") LocalDate endDate) {
        List<Order> orders = orderService.getOrdersByDateRange(startDate, endDate);
        return ResponseEntity.ok(orders);
    }

    // Calculates and returns the sum of quantities for a specific product within a date range.
    // Endpoint: GET /api/orders/sum-quantity-by-product?productId={id}&startDate=YYYY-MM-DD&endDate=YYYY-MM-DD
    @GetMapping("/sum-quantity-by-product")
    public ResponseEntity<Long> sumQuantityByProductIdAndDateRange(
            @RequestParam("productId") Long productId,
            @RequestParam("startDate") LocalDate startDate,
            @RequestParam("endDate") LocalDate endDate) {
        Long totalQuantity = orderService.sumQuantityByProductIdAndDateRange(productId, startDate, endDate);
        return ResponseEntity.ok(totalQuantity != null ? totalQuantity : 0L);
    }

    // Endpoint: GET /api/orders
    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> getAllOrders() { // MODIFIED RETURN TYPE
        List<OrderResponseDto> orders = orderService.getAllOrders(); // Calls the modified service method
        return ResponseEntity.ok(orders);
    }

    // Retrieves a single order by its ID.
    // Endpoint: GET /api/orders/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        try {
            Order order = orderService.getOrderById(id);
            return ResponseEntity.ok(order);
        } catch (OrderNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Creates a new order.
    // Endpoint: POST /api/orders
    // Request Body: {"customerId": 102, "productId": 3, "quantity": 1, "orderDate": "2025-06-01", "status": "Pending"}
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Order createOrder(@RequestBody Order order) {
        return orderService.createOrder(order);
    }

    // Updates an existing order.
    // Endpoint: PUT /api/orders/{id}
    // Request Body: {"orderId": 1, "customerId": 101, "productId": 2, "quantity": 3, "orderDate": "2025-05-30", "status": "Shipped"}
    @PutMapping("/{id}")
    public ResponseEntity<Order> updateOrder(@PathVariable Long id, @RequestBody Order orderDetails) {
        try {
            if (!id.equals(orderDetails.getOrderId())) {
                return ResponseEntity.badRequest().build();
            }
            Order updatedOrder = orderService.updateOrder(id, orderDetails);
            return ResponseEntity.ok(updatedOrder);
        } catch (OrderNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Deletes an order by its ID.
    // Endpoint: DELETE /api/orders/{id}
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
    }

    @GetMapping("/{orderId}/product-price")
    public ResponseEntity<Double> getProductPriceForOrder(@PathVariable Long orderId) {
        try {
            Double price = orderService.getOrderProductPrice(orderId);
            return ResponseEntity.ok(price);
        } catch (OrderNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Calculates and returns the total price of a specific order.
    // Endpoint: GET /api/orders/7/total-price
    @GetMapping("/{orderId}/total-price")
    public ResponseEntity<Double> getOrderTotalPrice(@PathVariable Long orderId) {
        try {
            Double totalPrice = orderService.calculateOrderTotalPrice(orderId);
            return ResponseEntity.ok(totalPrice);
        } catch (OrderNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    @PutMapping("/{id}/status")
    public ResponseEntity<Order> updateOrderStatus(@PathVariable Long id, @RequestBody Map<String, String> statusMap) {
        try {
            String status = statusMap.get("status");
            Order updatedOrder = orderService.updateStatus(id, status);
            return ResponseEntity.ok(updatedOrder);
        } catch (OrderNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}