// src/main/java/com/project/report_service/dto/ReportRequest.java
package com.project.report_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map; // <--- Import Map

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequest {
    private String reportType;
    private LocalDate startDate;
    private LocalDate endDate;
    private Map<String, Object> parameters; // <--- NEW FIELD
}