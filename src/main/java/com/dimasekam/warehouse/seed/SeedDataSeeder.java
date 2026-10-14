package com.dimasekam.warehouse.seed;

import com.dimasekam.warehouse.domain.StockMovementReason;
import com.dimasekam.warehouse.dto.AttributeRequest;
import com.dimasekam.warehouse.dto.ItemCreateRequest;
import com.dimasekam.warehouse.dto.SaleLineRequest;
import com.dimasekam.warehouse.dto.SaleRequest;
import com.dimasekam.warehouse.dto.StockAdjustmentRequest;
import com.dimasekam.warehouse.dto.VariantCreateRequest;
import com.dimasekam.warehouse.repository.ItemRepository;
import com.dimasekam.warehouse.repository.VariantRepository;
import com.dimasekam.warehouse.service.ItemService;
import com.dimasekam.warehouse.service.SaleService;
import com.dimasekam.warehouse.service.StockService;
import com.dimasekam.warehouse.service.VariantService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Sample data for a demo or a first look at the API. The logic lives here as a plain
 * bean so tests can call it directly; {@link SeedRunner} decides whether the
 * application runs it at startup (the `seed` profile).
 *
 * Every step checks what already exists before writing, so running it a second time
 * changes nothing: no duplicate SKUs, stock is not counted twice, and the sale is
 * only recorded once.
 */
@Component
public class SeedDataSeeder {

	private final ItemService itemService;
	private final VariantService variantService;
	private final StockService stockService;
	private final SaleService saleService;
	private final ItemRepository itemRepository;
	private final VariantRepository variantRepository;

	public SeedDataSeeder(ItemService itemService, VariantService variantService, StockService stockService,
			SaleService saleService, ItemRepository itemRepository, VariantRepository variantRepository) {
		this.itemService = itemService;
		this.variantService = variantService;
		this.stockService = stockService;
		this.saleService = saleService;
		this.itemRepository = itemRepository;
		this.variantRepository = variantRepository;
	}

	public void run() {
		// A simple item with its own stock.
		Long mugId = seedItem("SEED-MUG", "Ceramic mug", "Plain white ceramic mug", new BigDecimal("9.99"));
		if (mugId != null) {
			restockItem(mugId, 12);
		}

		// An item whose variants are the sellable units; the item itself holds no stock.
		Long teeId = seedItem("SEED-TEE", "Cotton tee", "T-shirt in two sizes", new BigDecimal("19.90"));
		Long largeId = null;
		if (teeId != null) {
			Long smallId = seedVariant(teeId, "SEED-TEE-S", null, "Size", "S");
			largeId = seedVariant(teeId, "SEED-TEE-L", new BigDecimal("21.90"), "Size", "L");
			if (smallId != null) {
				restockVariant(smallId, 5);
			}
			if (largeId != null) {
				restockVariant(largeId, 8);
			}
		}

		// One sale, only when this run built the two sellables it asks for. That keeps
		// a second run from recording the same sale again.
		if (mugId != null && largeId != null) {
			recordSale(mugId, largeId);
		}
	}

	// Returns the new item's id, or null when the SKU already exists (nothing written).
	private Long seedItem(String sku, String name, String description, BigDecimal price) {
		if (itemRepository.existsBySku(sku)) {
			return null;
		}
		ItemCreateRequest request = new ItemCreateRequest();
		request.setSku(sku);
		request.setName(name);
		request.setDescription(description);
		request.setPrice(price);
		return itemService.create(request).getId();
	}

	// Returns the new variant's id, or null when the SKU already exists (nothing written).
	private Long seedVariant(Long itemId, String sku, BigDecimal price, String attributeName, String attributeValue) {
		if (variantRepository.existsBySku(sku)) {
			return null;
		}
		AttributeRequest attribute = new AttributeRequest();
		attribute.setName(attributeName);
		attribute.setValue(attributeValue);

		VariantCreateRequest request = new VariantCreateRequest();
		request.setSku(sku);
		request.setPrice(price);
		request.getAttributes().add(attribute);
		return variantService.create(itemId, request).getId();
	}

	// Goes through the stock service so the seed leaves movement rows behind, exactly
	// like a restock typed into the API.
	private void restockItem(Long itemId, int quantity) {
		StockAdjustmentRequest request = new StockAdjustmentRequest();
		request.setDelta(quantity);
		request.setReason(StockMovementReason.RESTOCK);
		stockService.adjustItemStock(itemId, request);
	}

	private void restockVariant(Long variantId, int quantity) {
		StockAdjustmentRequest request = new StockAdjustmentRequest();
		request.setDelta(quantity);
		request.setReason(StockMovementReason.RESTOCK);
		stockService.adjustVariantStock(variantId, request);
	}

	private void recordSale(Long mugId, Long largeVariantId) {
		SaleLineRequest mugLine = new SaleLineRequest();
		mugLine.setType("ITEM");
		mugLine.setId(mugId);
		mugLine.setQuantity(2);

		SaleLineRequest teeLine = new SaleLineRequest();
		teeLine.setType("VARIANT");
		teeLine.setId(largeVariantId);
		teeLine.setQuantity(1);

		SaleRequest request = new SaleRequest();
		request.getLines().add(mugLine);
		request.getLines().add(teeLine);
		saleService.createSale(request);
	}
}
