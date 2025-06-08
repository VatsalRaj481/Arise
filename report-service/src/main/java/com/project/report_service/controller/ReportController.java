package com.project.report_service.controller;

import org.springframework.web.bind.annotation.*;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import com.project.report_service.exception.*;
import com.project.report_service.service.*;
import com.project.report_service.dto.ReportRequest;
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    // private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    @Autowired
    private ReportService reportService;


    @PostMapping("/generate")
    public ResponseEntity<?> generateReport(@RequestBody ReportRequest request) {
        try {
            switch (request.getReportType().toLowerCase()) {
                case "inventory":
                    return ResponseEntity.ok(reportService.generateInventoryReport(request));
                case "order":
                    return ResponseEntity.ok(reportService.generateOrderReport(request));
                case "supplier":
                    return ResponseEntity.ok(reportService.generateSupplierReport(request));
                default:
                    throw new InvalidReportTypeException("Invalid report type: " + request.getReportType());
            }
        } catch (InvalidReportTypeException | InvalidDateRangeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Unexpected error: " + e.getMessage());
        }
    }
}

