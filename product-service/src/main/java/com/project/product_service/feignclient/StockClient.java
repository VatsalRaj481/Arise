package com.project.product_service.feignclient;

import com.project.product_service.dto.StockDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

// This client will be used by product-service to SEND data to stock-service
@FeignClient(name = "stock-service", url = "${stock-service.url:http://localhost:8090}")
public interface StockClient {
    @GetMapping("/api/stocks/{productId}")
    Optional<StockDto> getStockByProductId(@PathVariable("productId") Long productId);

    @PostMapping("/api/stocks")
    StockDto addStock(@RequestBody StockDto stockDto);

    @PutMapping("/api/stocks/{productId}")
    StockDto updateStock(@PathVariable("productId") Long productId, @RequestBody StockDto stockDto);

    @DeleteMapping("/api/stocks/{productId}")
    void deleteStock(@PathVariable("productId") Long productId);
}