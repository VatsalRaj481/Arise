//package com.project.order_service.service;
//
//import java.time.LocalDate;
//import java.util.List;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import com.project.order_service.entity.Order;
//import com.project.order_service.exception.OrderNotFoundException;
//import com.project.order_service.repository.OrderRepository;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.transaction.annotation.Transactional;
//
//import com.project.order_service.feignclient.ProductClient;
//import com.project.order_service.feignclient.StockClient;
//import com.project.order_service.dto.ProductDto;
//import com.project.order_service.dto.StockDto;
//
//@Service
//@Slf4j
//public class OrderServiceImpl implements OrderService {
//
//    @Autowired
//    private OrderRepository orderRepository;
//
//    @Autowired
//    private ProductClient productClient;
//    @Autowired
//    private StockClient stockClient;
//
//    @Override
//    public Order createOrder(Order order) {
//        log.debug("Attempting to create order: {}", order);
//
//        // 1. Validate Product Existence and get price from Product Service
//        ProductDto product = productClient.getProductById(order.getProductId())
//                .orElseThrow(() -> {
//                    log.warn("Product with ID {} not found for order creation.", order.getProductId());
//                    return new OrderNotFoundException("Product with ID " + order.getProductId() + " not found.");
//                });
//        log.info("Product found: {} with price: {}", product.getName(), product.getPrice());
//
//
//        // 2. Check and Deduct Stock from Stock Service
//        StockDto stock = stockClient.getStockByProductId(order.getProductId());
//        if (stock == null) {
//            log.warn("Stock information not found for product ID {} during order creation.", order.getProductId());
//            throw new RuntimeException("Stock information not available for product ID: " + order.getProductId());
//        }
//
//        if (stock.getQuantity() < order.getQuantity()) {
//            log.warn("Insufficient stock for product ID {}. Available: {}, Requested: {}",
//                    order.getProductId(), stock.getQuantity(), order.getQuantity());
//            throw new RuntimeException("Insufficient stock for product ID: " + order.getProductId());
//        }
//
//        // Deduct stock
//        stock.setQuantity(stock.getQuantity() - order.getQuantity());
//        stockClient.updateStock(order.getProductId(), stock);
//        log.info("Stock updated for product ID {}. New quantity: {}", order.getProductId(), stock.getQuantity());
//
//
//        // 3. Set order details and save
//        order.setOrderDate(LocalDate.now());
//        order.setStatus("Pending"); // Initial status
//        try {
//            Order savedOrder = orderRepository.save(order);
//            log.info("Order saved successfully with ID: {}", savedOrder.getOrderId());
//            return savedOrder;
//        } catch (Exception e) {
//            log.error("Failed to save order: {}", e.getMessage(), e);
//            // Consider rolling back stock if order saving fails
//            // For simplicity, we're not implementing distributed transaction rollback here,
//            // but in a real-world scenario, you'd need Sagas or similar patterns.
//            throw new RuntimeException("Failed to create order: " + e.getMessage(), e);
//        }
//    }
//
//    @Override
//    public List<Order> getAllOrders() {
//        log.info("Fetching all orders.");
//        List<Order> orders = orderRepository.findAll();
//        log.info("Found {} orders.", orders.size());
//        return orders;
//    }
//
//    @Override
//    @Transactional // Ensures the update operation is atomic
//    public Order updateOrder(Long id, Order orderDetails) throws OrderNotFoundException {
//        // First, find the existing order
//        Order existingOrder = orderRepository.findById(id)
//                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + id));
//
//        existingOrder.setCustomerId(orderDetails.getCustomerId());
//        existingOrder.setProductId(orderDetails.getProductId());
//        existingOrder.setQuantity(orderDetails.getQuantity());
//        existingOrder.setOrderDate(orderDetails.getOrderDate());
//        existingOrder.setStatus(orderDetails.getStatus());
//
//        return orderRepository.save(existingOrder);
//    }
//
//    @Override
//    public Order getOrderById(Long id) {
//        log.info("Attempting to fetch order with ID: {}", id);
//        return orderRepository.findById(id)
//                .orElseThrow(() -> {
//                    log.warn("Order with ID {} not found.", id);
//                    return new OrderNotFoundException("Order with ID " + id + " not found.");
//                });
//    }
//
//    @Override
//    public void deleteOrder(Long id) {
//        log.info("Attempting to delete order with ID: {}", id);
//        if (!orderRepository.existsById(id)) {
//            log.warn("Attempted to delete non-existent order with ID: {}", id);
//            throw new OrderNotFoundException("Order with ID " + id + " not found, cannot delete.");
//        }
//        orderRepository.deleteById(id);
//        log.info("Order with ID: {} deleted successfully.", id);
//    }
//
//    @Override
//    public Order updateStatus(Long id, String status) {
//        log.info("Attempting to update status for order ID: {} to '{}'", id, status);
//        Order order = orderRepository.findById(id)
//                .orElseThrow(() -> {
//                    log.warn("Order with ID {} not found for status update.", id);
//                    return new OrderNotFoundException("Order with ID " + id + " not found, cannot update status.");
//                });
//        order.setStatus(status.trim());
//        try {
//            Order updatedOrder = orderRepository.save(order);
//            log.info("Order ID: {} status successfully updated to '{}'", id, updatedOrder.getStatus());
//            return updatedOrder;
//        } catch (Exception e) {
//            log.error("Failed to update status for order ID {}: {}", id, e.getMessage(), e);
//            throw new RuntimeException("Failed to update order status: " + e.getMessage(), e);
//        }
//    }
//
//    @Override
//    public List<Order> getOrdersByDateRange(LocalDate startDate, LocalDate endDate) {
//        log.info("Fetching orders between {} and {}", startDate, endDate);
//        List<Order> orders = orderRepository.findByOrderDateBetween(startDate, endDate);
//        log.info("Found {} orders between {} and {}.", orders.size(), startDate, endDate);
//        return orders;
//    }
//
//    @Override
//    public Long sumQuantityByProductIdAndDateRange(Long productId, LocalDate startDate, LocalDate endDate) {
//        log.info("Summing quantity for product ID {} between {} and {}", productId, startDate, endDate);
//        // The repository method might return null if no orders are found, handle that.
//        Long sum = orderRepository.sumQuantityByProductIdAndOrderDateBetween(productId, startDate, endDate);
//        if (sum == null) {
//            log.info("No orders found for product ID {} between {} and {}.", productId, startDate, endDate);
//            return 0L; // Return 0 if no orders or no quantity to sum
//        }
//        log.info("Total quantity for product ID {} between {} and {}: {}", productId, startDate, endDate, sum);
//        return sum;
//    }
//
//    @Override
//    public Double getOrderProductPrice(Long orderId) throws OrderNotFoundException {
//        log.info("Attempting to get product price for order ID: {}", orderId);
//        // 1. Get the order details
//        Order order = orderRepository.findById(orderId)
//                .orElseThrow(() -> {
//                    log.warn("Order with ID {} not found for product price retrieval.", orderId);
//                    return new OrderNotFoundException("Order with ID " + orderId + " not found.");
//                });
//
//        // 2. Fetch product details from Product Service
//        ProductDto product = productClient.getProductById(order.getProductId())
//                .orElseThrow(() -> {
//                    // This scenario means an order exists for a product that Product Service doesn't know about.
//                    // This indicates a data inconsistency, but we handle it gracefully here.
//                    log.error("Product with ID {} associated with order ID {} not found in Product Service.",
//                            order.getProductId(), orderId);
//                    return new OrderNotFoundException("Product for order ID " + orderId + " not found in Product Service.");
//                });
//
//        if (product.getPrice() == null) {
//            log.warn("Price not available for product ID {} associated with order ID {}.",
//                    order.getProductId(), orderId);
//            throw new RuntimeException("Product price not available for order ID: " + orderId);
//        }
//
//        log.info("Product price for order ID {}: {}", orderId, product.getPrice());
//        return product.getPrice();
//    }
//
//    @Override
//    public Double calculateOrderTotalPrice(Long orderId) throws OrderNotFoundException {
//        log.info("Attempting to calculate total price for order ID: {}", orderId);
//        // 1. Get the order details
//        Order order = orderRepository.findById(orderId)
//                .orElseThrow(() -> {
//                    log.warn("Order with ID {} not found for total price calculation.", orderId);
//                    return new OrderNotFoundException("Order with ID " + orderId + " not found.");
//                });
//
//        // 2. Get the product's individual price
//        Double productPrice = getOrderProductPrice(orderId); // Reusing the helper method
//
//        // 3. Calculate total price
//        Double totalPrice = order.getQuantity() * productPrice;
//        log.info("Total price for order ID {}: {}", orderId, totalPrice);
//        return totalPrice;
//    }
//}

package com.project.order_service.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional; // NEW IMPORT
import java.util.stream.Collectors; // NEW IMPORT

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.project.order_service.entity.Order;
import com.project.order_service.exception.OrderNotFoundException;
import com.project.order_service.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import com.project.order_service.feignclient.ProductClient;
import com.project.order_service.feignclient.StockClient;
import com.project.order_service.dto.ProductDto;
import com.project.order_service.dto.StockDto;
import com.project.order_service.dto.OrderResponseDto; // NEW IMPORT

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductClient productClient;
    @Autowired
    private StockClient stockClient;

    @Override
    public Order createOrder(Order order) {
        log.debug("Attempting to create order: {}", order);

        // 1. Validate Product Existence and get price from Product Service
        ProductDto product = productClient.getProductById(order.getProductId())
                .orElseThrow(() -> {
                    log.warn("Product with ID {} not found for order creation.", order.getProductId());
                    return new OrderNotFoundException("Product with ID " + order.getProductId() + " not found.");
                });
        log.info("Product found: {} with price: {}", product.getName(), product.getPrice());


        // 2. Check and Deduct Stock from Stock Service
        StockDto stock = stockClient.getStockByProductId(order.getProductId());
        if (stock == null) {
            log.warn("Stock information not found for product ID {} during order creation.", order.getProductId());
            throw new RuntimeException("Stock information not available for product ID: " + order.getProductId());
        }

        if (stock.getQuantity() < order.getQuantity()) {
            log.warn("Insufficient stock for product ID {}. Available: {}, Requested: {}",
                    order.getProductId(), stock.getQuantity(), order.getQuantity());
            throw new RuntimeException("Insufficient stock for product ID: " + order.getProductId());
        }

        // Increase stock
        stock.setQuantity(stock.getQuantity() + order.getQuantity());
        stockClient.updateStock(order.getProductId(), stock);
        log.info("Stock updated for product ID {}. New quantity: {}", order.getProductId(), stock.getQuantity());


        // 3. Set order details and save
        order.setOrderDate(LocalDate.now());
        order.setStatus("Pending"); // Initial status
        try {
            Order savedOrder = orderRepository.save(order);
            log.info("Order saved successfully with ID: {}", savedOrder.getOrderId());
            return savedOrder;
        } catch (Exception e) {
            log.error("Failed to save order: {}", e.getMessage(), e);
            // Consider rolling back stock if order saving fails
            // For simplicity, we're not implementing distributed transaction rollback here,
            // but in a real-world scenario, you'd need Sagas or similar patterns.
            throw new RuntimeException("Failed to create order: " + e.getMessage(), e);
        }
    }

    @Override
    public List<OrderResponseDto> getAllOrders() { // MODIFIED METHOD SIGNATURE
        log.info("Fetching all orders and enriching with product names.");
        List<Order> orders = orderRepository.findAll();

        return orders.stream()
                .map(order -> {
                    String productName = "Unknown Product"; // Default value
                    try {
                        Optional<ProductDto> productOptional = productClient.getProductById(order.getProductId());
                        if (productOptional.isPresent()) {
                            productName = productOptional.get().getName();
                        } else {
                            log.warn("Product with ID {} not found for order ID {} during enrichment.", order.getProductId(), order.getOrderId());
                        }
                    } catch (Exception e) {
                        log.error("Error fetching product details for ID {} (Order ID {}): {}", order.getProductId(), order.getOrderId(), e.getMessage());
                    }

                    return OrderResponseDto.builder()
                            .orderId(order.getOrderId())
                            .customerId(order.getCustomerId())
                            .productId(order.getProductId())
                            .productName(productName) // Set the fetched product name
                            .quantity(order.getQuantity())
                            .orderDate(order.getOrderDate())
                            .status(order.getStatus())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional // Ensures the update operation is atomic
    public Order updateOrder(Long id, Order orderDetails) throws OrderNotFoundException {
        // First, find the existing order
        Order existingOrder = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + id));

        existingOrder.setCustomerId(orderDetails.getCustomerId());
        existingOrder.setProductId(orderDetails.getProductId());
        existingOrder.setQuantity(orderDetails.getQuantity());
        existingOrder.setOrderDate(orderDetails.getOrderDate());
        existingOrder.setStatus(orderDetails.getStatus());

        return orderRepository.save(existingOrder);
    }

    @Override
    public Order getOrderById(Long id) {
        log.info("Attempting to fetch order with ID: {}", id);
        return orderRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Order with ID {} not found.", id);
                    return new OrderNotFoundException("Order with ID " + id + " not found.");
                });
    }

    @Override
    public void deleteOrder(Long id) {
        log.info("Attempting to delete order with ID: {}", id);
        if (!orderRepository.existsById(id)) {
            log.warn("Attempted to delete non-existent order with ID: {}", id);
            throw new OrderNotFoundException("Order with ID " + id + " not found, cannot delete.");
        }
        orderRepository.deleteById(id);
        log.info("Order with ID: {} deleted successfully.", id);
    }

    @Override
    public Order updateStatus(Long id, String status) {
        log.info("Attempting to update status for order ID: {} to '{}'", id, status);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Order with ID {} not found for status update.", id);
                    return new OrderNotFoundException("Order with ID " + id + " not found, cannot update status.");
                });
        order.setStatus(status.trim());
        try {
            Order updatedOrder = orderRepository.save(order);
            log.info("Order ID: {} status successfully updated to '{}'", id, updatedOrder.getStatus());
            return updatedOrder;
        } catch (Exception e) {
            log.error("Failed to update status for order ID {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to update order status: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Order> getOrdersByDateRange(LocalDate startDate, LocalDate endDate) {
        log.info("Fetching orders between {} and {}", startDate, endDate);
        List<Order> orders = orderRepository.findByOrderDateBetween(startDate, endDate);
        log.info("Found {} orders between {} and {}.", orders.size(), startDate, endDate);
        return orders;
    }

    @Override
    public Long sumQuantityByProductIdAndDateRange(Long productId, LocalDate startDate, LocalDate endDate) {
        log.info("Summing quantity for product ID {} between {} and {}", productId, startDate, endDate);
        Long sum = orderRepository.sumQuantityByProductIdAndOrderDateBetween(productId, startDate, endDate);
        if (sum == null) {
            log.info("No orders found for product ID {} between {} and {}.", productId, startDate, endDate);
            return 0L;
        }
        log.info("Total quantity for product ID {} between {} and {}: {}", productId, startDate, endDate, sum);
        return sum;
    }

    @Override
    public Double getOrderProductPrice(Long orderId) throws OrderNotFoundException {
        log.info("Attempting to get product price for order ID: {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.warn("Order with ID {} not found for product price retrieval.", orderId);
                    return new OrderNotFoundException("Order with ID " + orderId + " not found.");
                });

        ProductDto product = productClient.getProductById(order.getProductId())
                .orElseThrow(() -> {
                    log.error("Product with ID {} associated with order ID {} not found in Product Service.",
                            order.getProductId(), orderId);
                    return new OrderNotFoundException("Product for order ID " + orderId + " not found in Product Service.");
                });

        if (product.getPrice() == null) {
            log.warn("Price not available for product ID {} associated with order ID {}.",
                    order.getProductId(), orderId);
            throw new RuntimeException("Product price not available for order ID: " + orderId);
        }

        log.info("Product price for order ID {}: {}", orderId, product.getPrice());
        return product.getPrice();
    }

    @Override
    public Double calculateOrderTotalPrice(Long orderId) throws OrderNotFoundException {
        log.info("Attempting to calculate total price for order ID: {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.warn("Order with ID {} not found for total price calculation.", orderId);
                    return new OrderNotFoundException("Order with ID " + orderId + " not found.");
                });

        Double productPrice = getOrderProductPrice(orderId);

        Double totalPrice = order.getQuantity() * productPrice;
        log.info("Total price for order ID {}: {}", orderId, totalPrice);
        return totalPrice;
    }
}