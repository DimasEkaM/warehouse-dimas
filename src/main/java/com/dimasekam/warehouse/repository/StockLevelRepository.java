package com.dimasekam.warehouse.repository;

import com.dimasekam.warehouse.domain.StockLevel;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StockLevelRepository extends JpaRepository<StockLevel, Long> {

	Optional<StockLevel> findByItemId(Long itemId);

	Optional<StockLevel> findByVariantId(Long variantId);

	// Locked lookups for the moment a quantity is about to change. One query, locked
	// from the start: loading the row first without a lock would put it in Hibernate's
	// cache, and the follow-up FOR UPDATE could then read the stale quantity.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select s from StockLevel s where s.item.id = :itemId")
	Optional<StockLevel> findByItemIdForUpdate(@Param("itemId") Long itemId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select s from StockLevel s where s.variant.id = :variantId")
	Optional<StockLevel> findByVariantIdForUpdate(@Param("variantId") Long variantId);

	// SELECT ... FOR UPDATE. The row stays locked until the transaction ends, so two
	// sales racing for the last unit cannot both read the same quantity.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select s from StockLevel s where s.id = :id")
	Optional<StockLevel> findByIdForUpdate(@Param("id") Long id);
}
