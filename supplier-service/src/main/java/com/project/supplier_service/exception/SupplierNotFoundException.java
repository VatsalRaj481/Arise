// src/main/java/com/project/supplier_service/exception/SupplierNotFoundException.java
package com.project.supplier_service.exception;

public class SupplierNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    // Constructor that takes a supplier ID (for consistency with your existing code)
    public SupplierNotFoundException(Long supplierId) {
        super("Supplier not found with ID: " + supplierId);
    }

    // --- ADD THIS NEW CONSTRUCTOR ---
    // Constructor that takes a custom message (to resolve the current error)
    public SupplierNotFoundException(String message) {
        super(message);
    }
}