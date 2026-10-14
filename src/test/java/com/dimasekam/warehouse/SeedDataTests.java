package com.dimasekam.warehouse;

import com.dimasekam.warehouse.domain.Item;
import com.dimasekam.warehouse.domain.StockLevel;
import com.dimasekam.warehouse.domain.Variant;
import com.dimasekam.warehouse.repository.ItemRepository;
import com.dimasekam.warehouse.repository.SaleRepository;
import com.dimasekam.warehouse.repository.StockLevelRepository;
import com.dimasekam.warehouse.repository.VariantRepository;
import com.dimasekam.warehouse.seed.SeedDataSeeder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class SeedDataTests {

	@Autowired
	private SeedDataSeeder seeder;

	@Autowired
	private ItemRepository itemRepository;

	@Autowired
	private VariantRepository variantRepository;

	@Autowired
	private SaleRepository saleRepository;

	@Autowired
	private StockLevelRepository stockLevelRepository;

	@Test
	void seedingTwiceCreatesNoDuplicates() {
		long salesBefore = saleRepository.count();

		seeder.run();

		List<String> skusFirstRun = seedSkus();
		int mugStockFirstRun = mugStock();
		long salesFirstRun = saleRepository.count();

		// the first run must have actually built something
		assertThat(skusFirstRun).hasSize(4);
		assertThat(mugStockFirstRun).isEqualTo(10); // 12 restocked, 2 sold to the seed sale
		assertThat(salesFirstRun).isEqualTo(salesBefore + 1);

		seeder.run();

		List<String> skusSecondRun = seedSkus();

		// no SKU appears twice, and no new seed rows were added
		assertThat(skusSecondRun).doesNotHaveDuplicates();
		assertThat(skusSecondRun).containsExactlyInAnyOrderElementsOf(skusFirstRun);
		assertThat(mugStock()).isEqualTo(mugStockFirstRun);
		assertThat(saleRepository.count()).isEqualTo(salesFirstRun);
	}

	// Every seed SKU, across items and variants, in whatever order the database returns.
	private List<String> seedSkus() {
		List<String> skus = new ArrayList<>();
		for (Item item : itemRepository.findAll()) {
			if (item.getSku().startsWith("SEED-")) {
				skus.add(item.getSku());
			}
		}
		for (Variant variant : variantRepository.findAll()) {
			if (variant.getSku().startsWith("SEED-")) {
				skus.add(variant.getSku());
			}
		}
		return skus;
	}

	private int mugStock() {
		for (Item item : itemRepository.findAll()) {
			if ("SEED-MUG".equals(item.getSku())) {
				return stockLevelRepository.findByItemId(item.getId())
						.map(StockLevel::getQuantity)
						.orElse(-1);
			}
		}
		return -1;
	}
}
