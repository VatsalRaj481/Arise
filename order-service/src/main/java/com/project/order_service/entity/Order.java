package com.project.order_service.entity;

import java.time.LocalDate;

import jakarta.persistence.Column; // Import for @Column
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data; // Using Lombok for getters/setters, etc.
import lombok.NoArgsConstructor; // Add NoArgsConstructor
import lombok.AllArgsConstructor; // Add AllArgsConstructor

@Entity
@Table(name="orders")
@Data // Generates getters, setters, toString, equals, hashCode
@NoArgsConstructor // Generates a no-argument constructor
@AllArgsConstructor // Generates a constructor with all fields
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @Column(nullable = false) // Ensures customer_id cannot be null
    private Long customerId;

    @Column(nullable = false) // Ensures product_id cannot be null
    private Long productId;

    @Column(nullable = false) // Ensures quantity cannot be null
    private int quantity;

    @Column(nullable = false) // Ensures order_date cannot be null
    private LocalDate orderDate;

    @Column(nullable = false) // Ensures status cannot be null
    private String status;

    // Lombok's @Data will generate all getters and setters.
    // You don't need to write them manually.
}
