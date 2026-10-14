package com.dimasekam.warehouse.repository;

import com.dimasekam.warehouse.domain.Sale;
import org.springframework.data.jpa.repository.JpaRepository;

// Lines are persisted through the sale (cascade), so they have no repository.
public interface SaleRepository extends JpaRepository<Sale, Long> {
}
