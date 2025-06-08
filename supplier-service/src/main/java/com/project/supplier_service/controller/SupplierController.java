package com.project.supplier_service.controller;

import com.project.supplier_service.dto.SupplierResponseDto; // Import the DTO for responses
import com.project.supplier_service.exception.SupplierNotFoundException;
import com.project.supplier_service.model.Supplier; // Still use Supplier entity for request bodies
import com.project.supplier_service.service.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
// Optional: @CrossOrigin(origins = "*") if you need CORS, consider restricting in production
public class SupplierController {

    @Autowired
    private SupplierService supplierService;

    @GetMapping
    public List<SupplierResponseDto> getAllSuppliers() { // Return List of DTOs
        return supplierService.getAllSuppliers();
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplierResponseDto> getSupplier(@PathVariable Long id) { // Return ResponseEntity of DTO
        return supplierService.getSupplierById(id)
                .map(ResponseEntity::ok) // If Optional has value, return 200 OK
                .orElse(ResponseEntity.notFound().build()); // If Optional is empty, return 404 Not Found
    }

    // http://localhost:8080/api/suppliers
    // {
    //     "name": "Global Gadgets Inc.",
    //     "contactInfo": "info@globalgadgets.com",
    //     "providedProductIds": [2] // Array of product IDs from product-service
    // }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // Return 201 Created for new resources
    public Supplier createSupplier(@RequestBody Supplier supplier) {
        return supplierService.createSupplier(supplier);
    }

    // http://localhost:8080/api/suppliers/2
    // {
    //     "name": "Global Gadgets Corporation",
    //     "contactInfo": "sales@globalgadgets.com",
    //     "providedProductIds": [3, 4, 5] // Updated array of product IDs
    // }
    @PutMapping("/{id}")
    public Supplier updateSupplier(@PathVariable Long id, @RequestBody Supplier supplier) {
        return supplierService.updateSupplier(id, supplier);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // Return 204 No Content for successful deletion
    public void deleteSupplier(@PathVariable Long id) {
        supplierService.deleteSupplier(id);
    }

    @GetMapping("/products-supplied-count/{supplierId}") // <-- IMPORTANT: Ensure this path is correct
    public ResponseEntity<Long> getProductsSuppliedCountBySupplier(@PathVariable Long supplierId) {
        // ... (your service call logic here)
        // You can add a log here to confirm it's reached:
        // log.info("Received request for products supplied count for supplier ID: {}", supplierId);
        try {
            Long count = supplierService.getProductsSuppliedCountBySupplier(supplierId);
            return ResponseEntity.ok(count);
        } catch (SupplierNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            // Log the actual exception here for more details
            return ResponseEntity.internalServerError().body(0L);
        }
    }
}