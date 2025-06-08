package com.project.stock_service.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import java.time.LocalDateTime;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorDetails> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        ErrorDetails errorDetails = new ErrorDetails(LocalDateTime.now(), ex.getMessage(), request.getDescription(false));
        logger.warn("ResourceNotFoundException: {} - Path: {}", ex.getMessage(), request.getDescription(false));
        return new ResponseEntity<>(errorDetails, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(StockAlreadyExistsException.class)
    public ResponseEntity<ErrorDetails> handleStockAlreadyExistsException(StockAlreadyExistsException ex, WebRequest request) {
        ErrorDetails errorDetails = new ErrorDetails(LocalDateTime.now(), ex.getMessage(), request.getDescription(false));
        logger.warn("StockAlreadyExistsException: {} - Path: {}", ex.getMessage(), request.getDescription(false));
        return new ResponseEntity<>(errorDetails, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<ErrorDetails> handleInvalidInputException(InvalidInputException ex, WebRequest request) {
        ErrorDetails errorDetails = new ErrorDetails(LocalDateTime.now(), ex.getMessage(), request.getDescription(false));
        logger.warn("InvalidInputException: {} - Path: {}", ex.getMessage(), request.getDescription(false));
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    // Handle other specific exceptions as needed, e.g., DataIntegrityViolationException
    // @ExceptionHandler(DataIntegrityViolationException.class)
    // public ResponseEntity<ErrorDetails> handleDataIntegrityViolationException(DataIntegrityViolationException ex, WebRequest request) {
    //     ErrorDetails errorDetails = new ErrorDetails(LocalDateTime.now(), "Database error: " + ex.getMostSpecificCause().getMessage(), request.getDescription(false));
    //     logger.error("DataIntegrityViolationException: {} - Path: {}", ex.getMessage(), request.getDescription(false), ex);
    //     return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST); // Or CONFLICT depending on the nature
    // }


    @ExceptionHandler(Exception.class) // Generic exception handler for unexpected errors
    public ResponseEntity<ErrorDetails> handleGlobalException(Exception ex, WebRequest request) {
        ErrorDetails errorDetails = new ErrorDetails(LocalDateTime.now(), "An unexpected error occurred.", request.getDescription(false) + " | Error: " + ex.getMessage());
        logger.error("Global unexpected exception: {} - Path: {}", ex.getMessage(), request.getDescription(false), ex);
        return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}