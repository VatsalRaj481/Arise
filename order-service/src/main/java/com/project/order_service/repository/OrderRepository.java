package com.project.order_service.repository;

import com.project.order_service.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Custom method to find orders within a date range
    List<Order> findByOrderDateBetween(LocalDate startDate, LocalDate endDate);

    // Custom method to sum quantity by product ID and date range
    @Query("SELECT SUM(o.quantity) FROM Order o WHERE o.productId = :productId AND o.orderDate BETWEEN :startDate AND :endDate")
    Long sumQuantityByProductIdAndOrderDateBetween(
            @Param("productId") Long productId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}