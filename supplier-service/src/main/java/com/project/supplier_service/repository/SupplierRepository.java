package com.project.supplier_service.repository;

import com.project.supplier_service.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
	
}