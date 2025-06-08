package com.project.product_service.exception;

public class DuplicateProductException extends  RuntimeException{
    DuplicateProductException(String msg)
    {
        super(msg);
    }
}
