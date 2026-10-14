package com.dimasekam.warehouse.repository;

import com.dimasekam.warehouse.domain.Variant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VariantRepository extends JpaRepository<Variant, Long> {

	boolean existsBySku(String sku);

	List<Variant> findByItemId(Long itemId);

	long countByItemId(Long itemId);
}
