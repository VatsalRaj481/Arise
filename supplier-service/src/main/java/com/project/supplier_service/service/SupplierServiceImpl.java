package com.project.supplier_service.service;

import com.project.supplier_service.dto.ProductDto;
import com.project.supplier_service.dto.SupplierResponseDto;
import com.project.supplier_service.exception.SupplierNotFoundException;
import com.project.supplier_service.feignclient.ProductClient;
import com.project.supplier_service.model.Supplier;
import com.project.supplier_service.repository.SupplierRepository;
//import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
//@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {

    private static final Logger log = LoggerFactory.getLogger(SupplierServiceImpl.class);

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private ProductClient productClient; // Inject the Feign client

    /**
     * Helper method to convert a Supplier entity to a SupplierResponseDto,
     * fetching associated product details from the Product Service.
     */
    private SupplierResponseDto convertToDtoAndFetchProducts(Supplier supplier) {
        SupplierResponseDto dto = new SupplierResponseDto();
        dto.setSupplierId(supplier.getSupplierId());
        dto.setName(supplier.getName());
        dto.setContactInfo(supplier.getContactInfo());

        List<ProductDto> fetchedProducts = new ArrayList<>();
        if (supplier.getProvidedProductIds() != null && !supplier.getProvidedProductIds().isEmpty()) {
            fetchedProducts = supplier.getProvidedProductIds().stream()
                    .map(productId -> {
                        try {
                            // Call Product Service via Feign Client
                            Optional<ProductDto> productDtoOptional = productClient.getProductById(productId);
                            if (productDtoOptional.isPresent()) {
                                log.debug("Fetched product details for ID {}: {}", productId, productDtoOptional.get().getName());
                                return productDtoOptional.get();
                            } else {
                                log.warn("Product with ID {} not found in Product Service for supplier {}. Skipping.", productId, supplier.getSupplierId());
                                return null; // Or return a "Product not found" DTO if preferred
                            }
                        } catch (Exception e) {
                            log.error("Error fetching product ID {} from Product Service for supplier {}: {}", productId, supplier.getSupplierId(), e.getMessage());
                            // Handle cases where Product Service is down or returns an error
                            return null; // Return null or a fallback DTO
                        }
                    })
                    .filter(java.util.Objects::nonNull) // Filter out any nulls if products weren't found or errors occurred
                    .collect(Collectors.toList());
        }

        dto.setSuppliedProducts(fetchedProducts);
        return dto;
    }


    @Override
    public Supplier createSupplier(Supplier supplier) {
        log.info("Creating new supplier: {}", supplier.getName());
        // Optional: Add validation here to check if providedProductIds actually exist in Product Service
        // For example, call productClient.getProductById(id) for each ID to ensure validity.
        Supplier savedSupplier = supplierRepository.save(supplier);
        log.info("Supplier created successfully with ID: {}", savedSupplier.getSupplierId());
        return savedSupplier;
    }

    @Override
    public Supplier updateSupplier(Long supplierId, Supplier supplier) {
        log.info("Updating supplier with ID: {}", supplierId);

        return supplierRepository.findById(supplierId)
                .map(existingSupplier -> {
                    log.info("Supplier found: {} - Updating details", existingSupplier.getName());

                    existingSupplier.setName(supplier.getName());
                    existingSupplier.setContactInfo(supplier.getContactInfo());
                    // Update the list of product IDs provided by this supplier
                    existingSupplier.setProvidedProductIds(supplier.getProvidedProductIds());

                    Supplier updatedSupplier = supplierRepository.save(existingSupplier);
                    log.info("Successfully updated supplier: {}", updatedSupplier.getName());

                    return updatedSupplier;
                })
                .orElseThrow(() -> {
                    log.error("Supplier with ID {} not found - Update failed", supplierId);
                    return new SupplierNotFoundException(supplierId);
                });
    }

    @Override
    public void deleteSupplier(Long supplierId) {
        log.warn("Deleting supplier with ID: {}", supplierId);

        if (!supplierRepository.existsById(supplierId)) {
            log.error("Supplier with ID {} not found - Deletion failed", supplierId);
            throw new SupplierNotFoundException(supplierId);
        }

        supplierRepository.deleteById(supplierId);
        log.info("Supplier with ID {} deleted successfully", supplierId);
    }

    @Override
    public Optional<SupplierResponseDto> getSupplierById(Long supplierId) {
        log.info("Fetching supplier with ID: {}", supplierId);

        Optional<Supplier> supplierOptional = supplierRepository.findById(supplierId);
        if (supplierOptional.isPresent()) {
            // Convert the entity to DTO and fetch product details
            return supplierOptional.map(this::convertToDtoAndFetchProducts);
        } else {
            log.warn("Supplier with ID {} not found", supplierId);
            return Optional.empty(); // Let the controller handle 404
        }
    }


    @Override
    public List<SupplierResponseDto> getAllSuppliers() {
        log.info("Fetching all suppliers");
        List<Supplier> suppliers = supplierRepository.findAll();
        log.info("Found {} suppliers in local database.", suppliers.size());

        // Convert each Supplier entity to SupplierResponseDto and fetch products
        return suppliers.stream()
                .map(this::convertToDtoAndFetchProducts)
                .collect(Collectors.toList());
    }
    @Override
    public Long getProductsSuppliedCountBySupplier(Long supplierId) {
        log.info("Calculating products supplied count for supplier ID: {}", supplierId);
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> {
                    log.warn("Supplier with ID {} not found while counting supplied products.", supplierId);
                    // Use the constructor that takes a String message
                    return new SupplierNotFoundException("Supplier with ID " + supplierId + " not found.");
                });
        
        // --- FIX IS HERE ---
        // Access the list of product IDs stored in the Supplier entity
        if (supplier.getProvidedProductIds() != null) {
            long count = supplier.getProvidedProductIds().size(); // Use getProvidedProductIds()
            log.info("Supplier ID {} provides {} products.", supplierId, count);
            return count;
        }
        log.info("Supplier ID {} has no provided product IDs.", supplierId);
        return 0L;
    }
}