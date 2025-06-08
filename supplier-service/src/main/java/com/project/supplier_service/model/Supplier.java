package com.project.supplier_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long supplierId;

    private String name;
    private String contactInfo;

    // This now stores the IDs of products from the product-service that this supplier provides.
    // It's an @ElementCollection, meaning it's a collection of simple types (Longs)
    // stored in a separate table managed by the supplier-service's own database.
    // It is NOT a direct JPA relationship to the Product entity from the product-service.
    @ElementCollection(fetch = FetchType.LAZY) // Lazy fetch to avoid fetching by default
    @CollectionTable(name = "supplier_provided_product_ids", // Custom table name for clarity
                     joinColumns = @JoinColumn(name = "supplier_id"))
    @Column(name = "product_id") // Column name for the product ID in the join table
    private List<Long> providedProductIds = new ArrayList<>(); // Initialize to prevent NullPointerException
}