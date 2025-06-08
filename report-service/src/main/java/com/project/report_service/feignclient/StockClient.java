package com.project.report_service.feignclient;

import com.project.report_service.dto.StockDto; // DTO for stock data
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;

@FeignClient(name = "stock-service", url = "${stock-service.url:http://localhost:8090}")
public interface StockClient {

    @GetMapping("/api/stocks")
    List<StockDto> getAllStocks();

    @GetMapping("/api/stocks/{productId}")
    StockDto getStockByProductId(@PathVariable("productId") Long productId);
}
