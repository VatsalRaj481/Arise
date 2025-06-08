package com.project.product_service.exception;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<String> handleProductNotFound(ProductNotFoundException e)
    {
        return  new ResponseEntity<>("An error occurred: "+e.getMessage(),HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DuplicateProductException.class)
    public ResponseEntity<String> handleDuplicateProductFound(DuplicateProductException e)
    {
        return  new ResponseEntity<>("An error occurred: "+e.getMessage(),HttpStatus.BAD_REQUEST);
    }

}

