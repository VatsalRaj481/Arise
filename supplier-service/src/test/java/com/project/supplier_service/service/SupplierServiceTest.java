package com.project.supplier_service.service;

import com.project.supplier_service.dto.ProductDto;
import com.project.supplier_service.dto.SupplierResponseDto;
import com.project.supplier_service.exception.SupplierNotFoundException;
import com.project.supplier_service.feignclient.ProductClient;
import com.project.supplier_service.model.Supplier;
import com.project.supplier_service.repository.SupplierRepository;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the SupplierServiceImpl.
 * Uses Mockito to mock dependencies (SupplierRepository, ProductClient)
 * to test the service layer logic in isolation.
 *
 * This test does not interact with a real database or external services,
 * making it independent of the datasource and Feign client URLs in application.properties.
 */
@ExtendWith(MockitoExtension.class)
public class SupplierServiceTest {

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private ProductClient productClient; // Mock the Feign client

    @InjectMocks
    private SupplierServiceImpl supplierService;

    private Supplier sampleSupplier;
    private ProductDto sampleProductDto1;
    private ProductDto sampleProductDto2;

    @BeforeEach
    void setUp() {
        sampleSupplier = new Supplier(1L, "Test Supplier", "test@example.com", Arrays.asList(101L, 102L));
        sampleProductDto1 = new ProductDto(101L, "Product X", "Desc X", 10.0, 50, "urlX");
        sampleProductDto2 = new ProductDto(102L, "Product Y", "Desc Y", 20.0, 30, "urlY");
    }

    // --- createSupplier Tests ---
    @Test
    void createSupplier_shouldSaveSupplierSuccessfully() {
        Supplier newSupplier = new Supplier(null, "New Supplier", "new@test.com", Arrays.asList(1L));
        Supplier savedSupplier = new Supplier(1L, "New Supplier", "new@test.com", Arrays.asList(1L));

        when(supplierRepository.save(any(Supplier.class))).thenReturn(savedSupplier);

        Supplier result = supplierService.createSupplier(newSupplier);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("New Supplier");
        assertThat(result.getProvidedProductIds()).containsExactly(1L);
        verify(supplierRepository, times(1)).save(any(Supplier.class));
    }

    // --- updateSupplier Tests ---
    @Test
    void updateSupplier_shouldUpdateSupplierSuccessfully() {
        Supplier updatedDetails = new Supplier(1L, "Updated Supplier", "updated@test.com", Arrays.asList(103L));
        Supplier existingSupplier = new Supplier(1L, "Old Supplier", "old@test.com", Arrays.asList(101L));

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(existingSupplier));
        when(supplierRepository.save(any(Supplier.class))).thenReturn(updatedDetails);

        Supplier result = supplierService.updateSupplier(1L, updatedDetails);

        assertThat(result.getName()).isEqualTo("Updated Supplier");
        assertThat(result.getContactInfo()).isEqualTo("updated@test.com");
        assertThat(result.getProvidedProductIds()).containsExactly(103L);
        verify(supplierRepository, times(1)).findById(1L);
        verify(supplierRepository, times(1)).save(existingSupplier); // Verify save was called with the modified existingSupplier
    }

    @Test
    void updateSupplier_shouldThrowSupplierNotFoundException_whenSupplierDoesNotExist() {
        Supplier updatedDetails = new Supplier(99L, "NonExistent", "contact", Collections.emptyList());
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(SupplierNotFoundException.class, () -> supplierService.updateSupplier(99L, updatedDetails));

        verify(supplierRepository, times(1)).findById(99L);
        verify(supplierRepository, never()).save(any(Supplier.class));
    }

    // --- deleteSupplier Tests ---
    @Test
    void deleteSupplier_shouldDeleteSupplierSuccessfully() {
        when(supplierRepository.existsById(1L)).thenReturn(true);
        doNothing().when(supplierRepository).deleteById(1L);

        assertDoesNotThrow(() -> supplierService.deleteSupplier(1L));

        verify(supplierRepository, times(1)).existsById(1L);
        verify(supplierRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteSupplier_shouldThrowSupplierNotFoundException_whenSupplierDoesNotExist() {
        when(supplierRepository.existsById(99L)).thenReturn(false);

        assertThrows(SupplierNotFoundException.class, () -> supplierService.deleteSupplier(99L));

        verify(supplierRepository, times(1)).existsById(99L);
        verify(supplierRepository, never()).deleteById(anyLong());
    }

    // --- getSupplierById Tests ---
    @Test
    void getSupplierById_shouldReturnSupplierResponseDto_withProducts() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(sampleSupplier));
        // Mock product client calls for each product ID
        when(productClient.getProductById(101L)).thenReturn(Optional.of(sampleProductDto1));
        when(productClient.getProductById(102L)).thenReturn(Optional.of(sampleProductDto2));

        Optional<SupplierResponseDto> result = supplierService.getSupplierById(1L);

        assertThat(result).isPresent();
        SupplierResponseDto dto = result.get();
        assertThat(dto.getSupplierId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("Test Supplier");
        assertThat(dto.getSuppliedProducts()).hasSize(2);
        assertThat(dto.getSuppliedProducts().get(0).getName()).isEqualTo("Product X");
        assertThat(dto.getSuppliedProducts().get(1).getName()).isEqualTo("Product Y");

        verify(supplierRepository, times(1)).findById(1L);
        verify(productClient, times(1)).getProductById(101L);
        verify(productClient, times(1)).getProductById(102L);
    }

    @Test
    void getSupplierById_shouldReturnEmptyOptional_whenSupplierNotFound() {
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<SupplierResponseDto> result = supplierService.getSupplierById(99L);

        assertThat(result).isNotPresent();
        verify(supplierRepository, times(1)).findById(99L);
        verify(productClient, never()).getProductById(anyLong()); // No product client calls if supplier not found
    }

    @Test
    void getSupplierById_shouldHandleMissingProducts() {
        Supplier supplierWithMissingProduct = new Supplier(1L, "Test Supplier", "test@example.com", Arrays.asList(101L, 999L));
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplierWithMissingProduct));
        when(productClient.getProductById(101L)).thenReturn(Optional.of(sampleProductDto1));
        when(productClient.getProductById(999L)).thenReturn(Optional.empty()); // Product 999 not found

        Optional<SupplierResponseDto> result = supplierService.getSupplierById(1L);

        assertThat(result).isPresent();
        SupplierResponseDto dto = result.get();
        assertThat(dto.getSuppliedProducts()).hasSize(1); // Only Product X should be present
        assertThat(dto.getSuppliedProducts().get(0).getName()).isEqualTo("Product X");

        verify(productClient, times(1)).getProductById(101L);
        verify(productClient, times(1)).getProductById(999L);
    }

    @Test
    void getSupplierById_shouldHandleProductClientError() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(sampleSupplier));
        when(productClient.getProductById(101L)).thenReturn(Optional.of(sampleProductDto1));
        when(productClient.getProductById(102L)).thenThrow(mock(FeignException.class)); // Simulate Feign error

        Optional<SupplierResponseDto> result = supplierService.getSupplierById(1L);

        assertThat(result).isPresent();
        SupplierResponseDto dto = result.get();
        assertThat(dto.getSuppliedProducts()).hasSize(1); // Only Product X should be present
        assertThat(dto.getSuppliedProducts().get(0).getName()).isEqualTo("Product X");

        verify(productClient, times(1)).getProductById(101L);
        verify(productClient, times(1)).getProductById(102L);
    }

    // --- getAllSuppliers Tests ---
    @Test
    void getAllSuppliers_shouldReturnAllSuppliers_withProducts() {
        Supplier supplier2 = new Supplier(2L, "Supplier B", "contactB", Collections.singletonList(103L));
        ProductDto product3 = new ProductDto(103L, "Product Z", "Desc Z", 30.0, 20, "urlZ");

        List<Supplier> suppliers = Arrays.asList(sampleSupplier, supplier2);

        when(supplierRepository.findAll()).thenReturn(suppliers);
        when(productClient.getProductById(101L)).thenReturn(Optional.of(sampleProductDto1));
        when(productClient.getProductById(102L)).thenReturn(Optional.of(sampleProductDto2));
        when(productClient.getProductById(103L)).thenReturn(Optional.of(product3));

        List<SupplierResponseDto> result = supplierService.getAllSuppliers();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getSupplierId()).isEqualTo(1L);
        assertThat(result.get(0).getSuppliedProducts()).hasSize(2);
        assertThat(result.get(1).getSupplierId()).isEqualTo(2L);
        assertThat(result.get(1).getSuppliedProducts()).hasSize(1);
        assertThat(result.get(1).getSuppliedProducts().get(0).getName()).isEqualTo("Product Z");

        verify(supplierRepository, times(1)).findAll();
        verify(productClient, times(1)).getProductById(101L);
        verify(productClient, times(1)).getProductById(102L);
        verify(productClient, times(1)).getProductById(103L);
    }

    @Test
    void getAllSuppliers_shouldReturnEmptyList_whenNoSuppliersExist() {
        when(supplierRepository.findAll()).thenReturn(Collections.emptyList());

        List<SupplierResponseDto> result = supplierService.getAllSuppliers();

        assertThat(result).isEmpty();
        verify(supplierRepository, times(1)).findAll();
        verify(productClient, never()).getProductById(anyLong());
    }

    // --- getProductsSuppliedCountBySupplier Tests ---
    @Test
    void getProductsSuppliedCountBySupplier_shouldReturnCorrectCount() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(sampleSupplier)); // sampleSupplier has 2 product IDs

        Long count = supplierService.getProductsSuppliedCountBySupplier(1L);

        assertThat(count).isEqualTo(2L);
        verify(supplierRepository, times(1)).findById(1L);
    }

    @Test
    void getProductsSuppliedCountBySupplier_shouldReturnZero_whenNoProvidedProductIds() {
        Supplier supplierWithoutProducts = new Supplier(2L, "No Products Supplier", "no@test.com", Collections.emptyList());
        when(supplierRepository.findById(2L)).thenReturn(Optional.of(supplierWithoutProducts));

        Long count = supplierService.getProductsSuppliedCountBySupplier(2L);

        assertThat(count).isEqualTo(0L);
        verify(supplierRepository, times(1)).findById(2L);
    }

    @Test
    void getProductsSuppliedCountBySupplier_shouldThrowSupplierNotFoundException_whenSupplierDoesNotExist() {
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(SupplierNotFoundException.class, () -> supplierService.getProductsSuppliedCountBySupplier(99L));

        verify(supplierRepository, times(1)).findById(99L);
    }
}