package com.dimasekam.warehouse.service;

import com.dimasekam.warehouse.domain.Item;
import com.dimasekam.warehouse.domain.StockLevel;
import com.dimasekam.warehouse.domain.StockMovement;
import com.dimasekam.warehouse.domain.StockMovementReason;
import com.dimasekam.warehouse.domain.Variant;
import com.dimasekam.warehouse.dto.SellableResponse;
import com.dimasekam.warehouse.dto.StockAdjustmentRequest;
import com.dimasekam.warehouse.dto.StockAdjustmentResponse;
import com.dimasekam.warehouse.dto.StockMovementResponse;
import com.dimasekam.warehouse.exception.BadRequestException;
import com.dimasekam.warehouse.exception.ConflictException;
import com.dimasekam.warehouse.exception.NotFoundException;
import com.dimasekam.warehouse.repository.ItemRepository;
import com.dimasekam.warehouse.repository.StockLevelRepository;
import com.dimasekam.warehouse.repository.StockMovementRepository;
import com.dimasekam.warehouse.repository.VariantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class StockService {

	private final ItemRepository itemRepository;
	private final VariantRepository variantRepository;
	private final StockLevelRepository stockLevelRepository;
	private final StockMovementRepository stockMovementRepository;
	private final CatalogMapper catalogMapper;

	public StockService(ItemRepository itemRepository, VariantRepository variantRepository,
			StockLevelRepository stockLevelRepository, StockMovementRepository stockMovementRepository,
			CatalogMapper catalogMapper) {
		this.itemRepository = itemRepository;
		this.variantRepository = variantRepository;
		this.stockLevelRepository = stockLevelRepository;
		this.stockMovementRepository = stockMovementRepository;
		this.catalogMapper = catalogMapper;
	}

	@Transactional
	public StockAdjustmentResponse adjustItemStock(Long itemId, StockAdjustmentRequest request) {
		Item item = findItem(itemId);
		requireManualReason(request.getReason());

		// An item with variants is a grouping, not a sellable unit, so it holds no stock.
		if (variantRepository.countByItemId(itemId) > 0) {
			throw new ConflictException("item " + item.getSku() + " has variants; adjust the variants instead");
		}

		return apply(itemStockLevel(item), request);
	}

	@Transactional
	public StockAdjustmentResponse adjustVariantStock(Long variantId, StockAdjustmentRequest request) {
		Variant variant = findVariant(variantId);
		requireManualReason(request.getReason());

		return apply(variantStockLevel(variant), request);
	}

	@Transactional(readOnly = true)
	public List<StockMovementResponse> listMovements(Long itemId, Long variantId) {
		List<StockMovement> movements;
		if (itemId != null) {
			movements = stockMovementRepository.findByStockLevelItemId(itemId);
		} else if (variantId != null) {
			movements = stockMovementRepository.findByStockLevelVariantId(variantId);
		} else {
			movements = stockMovementRepository.findAll();
		}

		List<StockMovementResponse> responses = new ArrayList<>();
		for (StockMovement movement : movements) {
			responses.add(toMovementResponse(movement));
		}
		return responses;
	}

	private StockAdjustmentResponse apply(StockLevel level, StockAdjustmentRequest request) {
		int delta = request.getDelta();
		int newQuantity = level.getQuantity() + delta;
		SellableResponse sellable = catalogMapper.toSellableResponse(level);

		if (newQuantity < 0) {
			throw new ConflictException("not enough stock for " + sellable.getType().toLowerCase() + " "
					+ sellable.getSku() + ": " + level.getQuantity() + " available, " + (-delta) + " requested");
		}

		level.setQuantity(newQuantity);
		StockLevel savedLevel = stockLevelRepository.save(level);

		// The quantity change and its movement row live in the same transaction.
		StockMovement movement = stockMovementRepository.save(
				StockMovement.of(savedLevel, delta, newQuantity, request.getReason(), null));

		StockAdjustmentResponse response = new StockAdjustmentResponse();
		response.setStockLevelId(savedLevel.getId());
		response.setSellable(sellable);
		response.setQuantity(newQuantity);
		response.setMovementId(movement.getId());
		return response;
	}

	// The stock row is created the first time it is needed, so a fresh item reads as zero.
	private StockLevel itemStockLevel(Item item) {
		Optional<StockLevel> found = stockLevelRepository.findByItemId(item.getId());
		if (found.isPresent()) {
			return stockLevelRepository.findByIdForUpdate(found.get().getId()).orElseThrow();
		}
		return stockLevelRepository.save(StockLevel.forItem(item));
	}

	private StockLevel variantStockLevel(Variant variant) {
		Optional<StockLevel> found = stockLevelRepository.findByVariantId(variant.getId());
		if (found.isPresent()) {
			return stockLevelRepository.findByIdForUpdate(found.get().getId()).orElseThrow();
		}
		return stockLevelRepository.save(StockLevel.forVariant(variant));
	}

	private void requireManualReason(StockMovementReason reason) {
		// SALE movements are written by the sales service, never by a manual adjustment.
		if (reason == StockMovementReason.SALE) {
			throw new BadRequestException("reason must be RESTOCK or ADJUSTMENT");
		}
	}

	private StockMovementResponse toMovementResponse(StockMovement movement) {
		StockMovementResponse response = new StockMovementResponse();
		response.setId(movement.getId());
		response.setSellable(catalogMapper.toSellableResponse(movement.getStockLevel()));
		response.setDelta(movement.getDelta());
		response.setQuantityAfter(movement.getQuantityAfter());
		response.setReason(movement.getReason().name());
		response.setReferenceId(movement.getReferenceId());
		response.setCreatedAt(movement.getCreatedAt());
		return response;
	}

	private Item findItem(Long id) {
		return itemRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("item not found: " + id));
	}

	private Variant findVariant(Long id) {
		return variantRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("variant not found: " + id));
	}
}
