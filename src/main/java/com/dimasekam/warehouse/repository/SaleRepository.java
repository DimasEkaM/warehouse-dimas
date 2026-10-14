package com.dimasekam.warehouse.repository;

import com.dimasekam.warehouse.domain.Sale;
import org.springframework.data.jpa.repository.JpaRepository;

// Lines are persisted through the sale (cascade); read-only line lookups live in
// SaleLineRepository.
public interface SaleRepository extends JpaRepository<Sale, Long> {
}
