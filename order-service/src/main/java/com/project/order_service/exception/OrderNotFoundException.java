package com.project.order_service.exception;

public class OrderNotFoundException extends RuntimeException{

    private static final long serialVersionUID = 3000469681218165763L;

    public OrderNotFoundException(String msg) {
        super(msg);
    }
}