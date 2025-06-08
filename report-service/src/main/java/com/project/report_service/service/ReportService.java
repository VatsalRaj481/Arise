package com.project.report_service.service;

import com.project.report_service.dto.*;
import java.util.List;

public interface ReportService {
    List<InventoryReportDto> generateInventoryReport(ReportRequest request);
    OrderReportDto generateOrderReport(ReportRequest request);
    List<SupplierReportDto> generateSupplierReport(ReportRequest request);
}
