package com.dimasekam.warehouse.repository;

import com.dimasekam.warehouse.domain.SaleLine;
import org.springframework.data.jpa.repository.JpaRepository;

// Lines are written through the sale (cascade), so nothing here saves them. These two
// checks only read, and exist so a delete can be refused before the foreign key fires.
public interface SaleLineRepository extends JpaRepository<SaleLine, Long> {

	boolean existsByItemId(Long itemId);

	boolean existsByVariantId(Long variantId);
}
