package com.project.stock_service.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Stock {
    @Id
    @Column(name = "product_id")
    private Long productId;

    private int quantity;
    private int reorderLevel;

    public boolean isLowStock() { 
        return quantity <= reorderLevel;
    }


}
