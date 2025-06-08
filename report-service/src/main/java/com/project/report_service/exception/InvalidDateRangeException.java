package com.project.report_service.exception; // Changed package to com.project.report_service.exception for consistency

public class InvalidDateRangeException extends RuntimeException {
    public InvalidDateRangeException(String message) {
        super(message);
    }
}
