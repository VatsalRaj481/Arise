package com.project.order_service.feignclient;

import com.project.order_service.dto.StockDto; // DTO for stock data
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "stock-service", url = "${stock-service.url:http://localhost:8090}") // 'name' is Eureka service ID, 'url' is fallback
public interface StockClient {

    @GetMapping("/api/stocks/{productId}")
    StockDto getStockByProductId(@PathVariable("productId") Long productId);

    @PutMapping("/api/stocks/{productId}")
    StockDto updateStock(@PathVariable("productId") Long productId, @RequestBody StockDto stockDto);
}