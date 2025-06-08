package com.project.stock_service.controller;

import com.project.stock_service.model.Stock;
import com.project.stock_service.service.StockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus; 
import org.springframework.http.ResponseEntity; 
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
@CrossOrigin(origins = "*") // Consider more restrictive origins for production
public class StockController {

    private static final Logger logger = LoggerFactory.getLogger(StockController.class);

    @Autowired
    private StockService stockService;

    // http://localhost:8080/api/stocks
    // {
    //     "productId": 2,
    //     "quantity": 50, // Enough quantity for testing
    //     "location": "Warehouse A"
    // }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // Indicate successful creation status
    public Stock addStock(@RequestBody Stock stock){
        logger.info("Received request to add stock: {}", stock);
        // Any validation on the stock object itself (e.g. @Valid) could be added here
        Stock newStock = stockService.addStock(stock);
        logger.info("Stock added successfully: {}", newStock);
        return newStock;
    }

    //http://localhost:8080/api/stocks/2
    // {
    //     "productId": 2,      // Must match the path variable {productId}
    //     "quantity": 60,      // New quantity
    //     "location": "Warehouse B" // New location
    // }
    @PutMapping("/{productId}")
    public Stock updateStock(@PathVariable Long productId, @RequestBody Stock stock){
        logger.info("Received request to update stock for productId: {} with data: {}", productId, stock);
        Stock updatedStock = stockService.updateStock(productId, stock);
        logger.info("Stock updated successfully for productId: {}: {}", productId, updatedStock);
        return updatedStock;
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteStock(@PathVariable Long productId){ // Return ResponseEntity for more control
        logger.info("Received request to delete stock for productId: {}", productId);
        stockService.deleteStock(productId);
        logger.info("Stock deleted successfully for productId: {}", productId);
        return ResponseEntity.noContent().build(); // Standard practice for successful DELETE
    }

    @GetMapping
    public List<Stock> getAllStock(){
        logger.info("Received request to get all stocks");
        List<Stock> stocks = stockService.getAllStock();
        logger.info("Retrieved {} stocks", stocks.size());
        return stocks;
    }

    @GetMapping("/{productId}")
    public Stock getStockByProductId(@PathVariable Long productId){
        logger.info("Received request to get stock by productId: {}", productId);
        // The service will now throw ResourceNotFoundException if not found,
        // which will be handled by GlobalExceptionHandler.
        Stock stock = stockService.getStockByProductId(productId);
        logger.info("Stock found for productId {}: {}", productId, stock);
        return stock;
    }
}