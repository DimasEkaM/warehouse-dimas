package com.dimasekam.warehouse.repository;

import com.dimasekam.warehouse.domain.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

	List<StockMovement> findByStockLevelIdOrderByCreatedAtAsc(Long stockLevelId);

	List<StockMovement> findByStockLevelItemId(Long itemId);

	List<StockMovement> findByStockLevelVariantId(Long variantId);
}
