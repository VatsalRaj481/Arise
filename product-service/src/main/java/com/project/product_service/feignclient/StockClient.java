// src/main/java/com/project/product_service/feignclient/StockClient.java
package com.project.product_service.feignclient;

import com.project.product_service.dto.StockDto; // Import the StockDto
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

@FeignClient(name = "stock-service", url = "${stock-service.url:http://localhost:8090}") // Assuming stock-service runs on 8083
public interface StockClient {

    @GetMapping("/api/stocks/{productId}")
    Optional<StockDto> getStockByProductId(@PathVariable("productId") Long productId);
}