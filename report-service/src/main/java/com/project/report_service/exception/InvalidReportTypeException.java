package com.project.report_service.exception;

public class InvalidReportTypeException extends RuntimeException {
    public InvalidReportTypeException(String message) {
        super(message);
    }
}
