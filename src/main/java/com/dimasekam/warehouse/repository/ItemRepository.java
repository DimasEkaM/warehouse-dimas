package com.dimasekam.warehouse.repository;

import com.dimasekam.warehouse.domain.Item;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {

	// Used to answer 409 on a duplicate SKU before the database constraint fires.
	boolean existsBySku(String sku);
}
