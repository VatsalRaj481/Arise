// package com.project.stock_service.service;


// import com.project.stock_service.exception.InvalidInputException;
// import com.project.stock_service.exception.ResourceNotFoundException;
// import com.project.stock_service.exception.StockAlreadyExistsException;
// import com.project.stock_service.model.Stock;
// import com.project.stock_service.repository.StockRepository;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Service;

// import java.util.List;
// // import java.util.Optional; // No longer needed directly for checking, use orElseThrow

// @Service
// public class StockServiceImpl implements StockService {

//     private static final Logger logger = LoggerFactory.getLogger(StockServiceImpl.class);

//     @Autowired
//     private StockRepository stockRepository;

//     @Override
//     public Stock addStock(Stock stock) {
//         logger.debug("Attempting to add stock: {}", stock);
//         if (stock.getProductId() == null) {
//             logger.error("Attempted to add stock with null productId.");
//             throw new InvalidInputException("Product ID cannot be null for a new stock item.");
//         }
//         if (stockRepository.existsById(stock.getProductId())) {
//             logger.warn("Attempted to add stock with existing productId: {}", stock.getProductId());
//             throw new StockAlreadyExistsException("Stock item with Product ID " + stock.getProductId() + " already exists.");
//         }
//         // The lowStock status is determined by the isLowStock() method in the entity for responses.
//         // No need to set it manually here if it's a derived property.
//         Stock saved = stockRepository.save(stock);
//         logger.info("Stock added successfully: {}", saved);
//         return saved;
//     }

//     @Override
//     public Stock updateStock(Long productId, Stock stockDetails) {
//         logger.debug("Attempting to update stock for productId: {}", productId);
//         if (stockDetails.getProductId() != null && !productId.equals(stockDetails.getProductId())) {
//             logger.error("Path productId {} does not match request body productId {}.", productId, stockDetails.getProductId());
//             throw new InvalidInputException("Product ID in path (" + productId + ") does not match Product ID in request body (" + stockDetails.getProductId() + ").");
//         }

//         Stock existingStock = stockRepository.findById(productId)
//                 .orElseThrow(() -> {
//                     logger.error("Stock not found for productId during update: {}", productId);
//                     return new ResourceNotFoundException("Stock item not found with Product ID: " + productId);
//                 });

//         logger.debug("Found existing stock: {}", existingStock);

//         existingStock.setQuantity(stockDetails.getQuantity());
//         existingStock.setReorderLevel(stockDetails.getReorderLevel());
//         // lowStock status is handled by the entity's isLowStock() method.

//         Stock updated = stockRepository.save(existingStock);
//         logger.info("Stock updated successfully for productId {}: {}", productId, updated);
//         return updated;
//     }

//     @Override
//     public void deleteStock(Long productId) {
//         logger.debug("Attempting to delete stock for productId: {}", productId);
//         if (!stockRepository.existsById(productId)) {
//             logger.warn("Attempted to delete non-existent stock for productId: {}", productId);
//             throw new ResourceNotFoundException("Stock item not found with Product ID: " + productId + ". Cannot delete.");
//         }
//         stockRepository.deleteById(productId);
//         logger.info("Stock deleted for productId: {}", productId);
//     }

//     @Override
//     public List<Stock> getAllStock() {
//         logger.debug("Attempting to retrieve all stocks");
//         List<Stock> stocks = stockRepository.findAll();
//         // lowStock status is handled by the entity's isLowStock() method for each stock item in the list during serialization.
//         logger.info("Retrieved {} stocks.", stocks.size());
//         return stocks;
//     }

//     @Override
//     public Stock getStockByProductId(Long productId) {
//         logger.debug("Attempting to retrieve stock by productId: {}", productId);
//         Stock stock = stockRepository.findById(productId)
//                 .orElseThrow(() -> {
//                     logger.warn("No stock found for productId during retrieval: {}", productId);
//                     return new ResourceNotFoundException("Stock item not found with Product ID: " + productId);
//                 });
//         // lowStock status is handled by the entity's isLowStock() method.
//         logger.info("Stock found for productId {}: {}", productId, stock);
//         return stock;
//     }
// }


// src/main/java/com/project/stock_service/service/StockServiceImpl.java
package com.project.stock_service.service;

import com.project.stock_service.exception.InvalidInputException;
import com.project.stock_service.exception.ResourceNotFoundException;
import com.project.stock_service.exception.StockAlreadyExistsException;
import com.project.stock_service.model.Stock;
import com.project.stock_service.repository.StockRepository;
import com.project.stock_service.feignclient.ProductClient; // Import the new Feign client
import com.project.stock_service.dto.ProductDto; // Import the ProductDto

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StockServiceImpl implements StockService {

    private static final Logger logger = LoggerFactory.getLogger(StockServiceImpl.class);

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private ProductClient productClient; // Inject the ProductClient

    @Override
    public Stock addStock(Stock stock) {
        logger.debug("Attempting to add stock: {}", stock);
        if (stock.getProductId() == null) {
            logger.error("Attempted to add stock with null productId.");
            throw new InvalidInputException("Product ID cannot be null for a new stock item.");
        }

        // --- NEW VALIDATION: Check if product exists in Product Service ---
        try {
            Optional<ProductDto> productDto = productClient.getProductById(stock.getProductId());
            if (productDto.isEmpty()) {
                logger.error("Product with ID {} not found in Product Service. Cannot add stock.", stock.getProductId());
                throw new InvalidInputException("Product with ID " + stock.getProductId() + " does not exist.");
            }
            logger.debug("Product with ID {} found in Product Service for stock addition.", stock.getProductId());
        } catch (Exception e) {
            logger.error("Error communicating with Product Service to validate product ID {}: {}", stock.getProductId(), e.getMessage());
            // Depending on your error handling strategy, you might rethrow, or throw a custom service unavailable exception
            throw new RuntimeException("Failed to validate product with Product Service.", e);
        }
        // --- END NEW VALIDATION ---


        if (stockRepository.existsById(stock.getProductId())) {
            logger.warn("Attempted to add stock with existing productId: {}", stock.getProductId());
            throw new StockAlreadyExistsException("Stock item with Product ID " + stock.getProductId() + " already exists.");
        }

        Stock saved = stockRepository.save(stock);
        logger.info("Stock added successfully: {}", saved);
        return saved;
    }

    @Override
    public Stock updateStock(Long productId, Stock stockDetails) {
        logger.debug("Attempting to update stock for productId: {}", productId);
        if (stockDetails.getProductId() != null && !productId.equals(stockDetails.getProductId())) {
            logger.error("Path productId {} does not match request body productId {}.", productId, stockDetails.getProductId());
            throw new InvalidInputException("Product ID in path (" + productId + ") does not match Product ID in request body (" + stockDetails.getProductId() + ").");
        }

        // --- NEW VALIDATION: Check if product exists in Product Service (optional, but good for consistency) ---
        // If the product ID is updated or re-validated. For simple quantity updates, it might be redundant.
        // But if you allow changing the productId associated with a stock record, this is crucial.
        // Assuming productId in path is the definitive one for lookup, no need to re-validate if it's not changed.
        // However, if the stockDetails.getProductId() is different, you'd need to validate that new ID.
        // For simplicity, we'll assume productId passed in path is the key and it already exists.
        // If the update implies moving stock to a *new* productId, this logic needs to be more complex.
        // For now, only checking the existing productId's existence is enough.
        try {
            Optional<ProductDto> productDto = productClient.getProductById(productId); // Validate the productId being updated
            if (productDto.isEmpty()) {
                logger.error("Product with ID {} not found in Product Service for update. Cannot update stock.", productId);
                throw new ResourceNotFoundException("Product with ID " + productId + " does not exist. Cannot update stock.");
            }
            logger.debug("Product with ID {} found in Product Service for stock update.", productId);
        } catch (Exception e) {
            logger.error("Error communicating with Product Service to validate product ID {} during update: {}", productId, e.getMessage());
            throw new RuntimeException("Failed to validate product with Product Service during update.", e);
        }
        // --- END NEW VALIDATION ---


        Stock existingStock = stockRepository.findById(productId)
                .orElseThrow(() -> {
                    logger.error("Stock not found for productId during update: {}", productId);
                    return new ResourceNotFoundException("Stock item not found with Product ID: " + productId);
                });

        logger.debug("Found existing stock: {}", existingStock);

        existingStock.setQuantity(stockDetails.getQuantity());
        existingStock.setReorderLevel(stockDetails.getReorderLevel());

        Stock updated = stockRepository.save(existingStock);
        logger.info("Stock updated successfully for productId {}: {}", productId, updated);
        return updated;
    }

    @Override
    public void deleteStock(Long productId) {
        logger.debug("Attempting to delete stock for productId: {}", productId);
        if (!stockRepository.existsById(productId)) {
            logger.warn("Attempted to delete non-existent stock for productId: {}", productId);
            throw new ResourceNotFoundException("Stock item not found with Product ID: " + productId + ". Cannot delete.");
        }
        stockRepository.deleteById(productId);
        logger.info("Stock deleted for productId: {}", productId);
    }

    @Override
    public List<Stock> getAllStock() {
        logger.debug("Attempting to retrieve all stocks");
        List<Stock> stocks = stockRepository.findAll();
        logger.info("Retrieved {} stocks.", stocks.size());
        return stocks;
    }

    @Override
    public Stock getStockByProductId(Long productId) {
        logger.debug("Attempting to retrieve stock by productId: {}", productId);
        Stock stock = stockRepository.findById(productId)
                .orElseThrow(() -> {
                    logger.warn("No stock found for productId during retrieval: {}", productId);
                    return new ResourceNotFoundException("Stock item not found with Product ID: " + productId);
                });
        logger.info("Stock found for productId {}: {}", productId, stock);
        return stock;
    }
}