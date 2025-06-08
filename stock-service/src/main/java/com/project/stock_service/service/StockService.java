package com.project.stock_service.service;
import com.project.stock_service.model.Stock;
import java.util.List;

public interface StockService {
    Stock addStock(Stock stock);
    Stock updateStock(Long productId, Stock stock);
    void deleteStock(Long productId);
    List<Stock> getAllStock();
    Stock getStockByProductId(Long productId);
}
