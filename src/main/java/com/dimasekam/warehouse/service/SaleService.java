package com.dimasekam.warehouse.service;

import com.dimasekam.warehouse.domain.Item;
import com.dimasekam.warehouse.domain.Sale;
import com.dimasekam.warehouse.domain.SaleLine;
import com.dimasekam.warehouse.domain.StockLevel;
import com.dimasekam.warehouse.domain.StockMovement;
import com.dimasekam.warehouse.domain.StockMovementReason;
import com.dimasekam.warehouse.domain.Variant;
import com.dimasekam.warehouse.dto.SaleLineRequest;
import com.dimasekam.warehouse.dto.SaleLineResponse;
import com.dimasekam.warehouse.dto.SaleRequest;
import com.dimasekam.warehouse.dto.SaleResponse;
import com.dimasekam.warehouse.exception.BadRequestException;
import com.dimasekam.warehouse.exception.ConflictException;
import com.dimasekam.warehouse.exception.NotFoundException;
import com.dimasekam.warehouse.repository.ItemRepository;
import com.dimasekam.warehouse.repository.SaleRepository;
import com.dimasekam.warehouse.repository.StockLevelRepository;
import com.dimasekam.warehouse.repository.StockMovementRepository;
import com.dimasekam.warehouse.repository.VariantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SaleService {

	private final SaleRepository saleRepository;
	private final ItemRepository itemRepository;
	private final VariantRepository variantRepository;
	private final StockLevelRepository stockLevelRepository;
	private final StockMovementRepository stockMovementRepository;

	public SaleService(SaleRepository saleRepository, ItemRepository itemRepository,
			VariantRepository variantRepository, StockLevelRepository stockLevelRepository,
			StockMovementRepository stockMovementRepository) {
		this.saleRepository = saleRepository;
		this.itemRepository = itemRepository;
		this.variantRepository = variantRepository;
		this.stockLevelRepository = stockLevelRepository;
		this.stockMovementRepository = stockMovementRepository;
	}

	@Transactional
	public SaleResponse createSale(SaleRequest request) {
		if (request.getLines() == null || request.getLines().isEmpty()) {
			throw new BadRequestException("at least one line is required");
		}

		// All lines must be valid; also prevent selling same sellable twice in one sale (simple rule)
		Set<String> keys = new HashSet<>();
		List<SaleLineRequest> lines = request.getLines();
		for (SaleLineRequest line : lines) {
			String key = (line.getType() == null ? "?" : line.getType().toUpperCase()) + ":" + line.getId();
			if (!keys.add(key)) {
				throw new BadRequestException("duplicate line for " + key);
			}
		}

		List<SaleLineData> data = new ArrayList<>();
		for (SaleLineRequest line : lines) {
			data.add(toLineData(line));
		}

		// Lock all stock rows for affected sellables
		List<StockLevel> locked = new ArrayList<>();
		try {
			for (SaleLineData d : data) {
				locked.add(lockStockLevel(d));
			}

			// Check quantities (all or nothing)
			for (int i = 0; i < data.size(); i++) {
				checkAvailable(data.get(i), locked.get(i));
			}

			// Apply: create sale, create lines, update quantities, create movements
			Sale sale = new Sale();
			BigDecimal total = BigDecimal.ZERO;
			for (int i = 0; i < data.size(); i++) {
				SaleLineData d = data.get(i);
				StockLevel sl = locked.get(i);

				BigDecimal lineTotal = d.unitPrice.multiply(BigDecimal.valueOf(d.quantity));
				total = total.add(lineTotal);

				SaleLine saleLine = d.createSaleLine(lineTotal);
				sale.addLine(saleLine);

				int newQty = sl.getQuantity() - d.quantity;
				sl.setQuantity(newQty);
				stockLevelRepository.save(sl);

				stockMovementRepository.save(StockMovement.of(sl, -d.quantity, newQty, StockMovementReason.SALE, sale.getId()));
			}
			sale.setTotal(total);

			Sale saved = saleRepository.save(sale);

			return toSaleResponse(saved);
		} finally {
			// no-op; transaction ends
		}
	}

	private SaleLineData toLineData(SaleLineRequest request) {
		String type = request.getType();
		if (type == null) {
			throw new BadRequestException("type is required");
		}
		type = type.toUpperCase();
		if ("ITEM".equals(type)) {
			Long id = request.getId();
			Item item = itemRepository.findById(id)
					.orElseThrow(() -> new NotFoundException("item not found: " + id));
			if (variantRepository.countByItemId(id) > 0) {
				throw new ConflictException("item " + item.getSku() + " has variants and cannot be sold as an item");
			}
			BigDecimal unitPrice = item.getPrice();
			return new SaleLineData(type, item, null, id, item.getSku(), request.getQuantity(), unitPrice);
		} else if ("VARIANT".equals(type)) {
			Long id = request.getId();
			Variant variant = variantRepository.findById(id)
					.orElseThrow(() -> new NotFoundException("variant not found: " + id));
			BigDecimal unitPrice = variant.getPrice() != null ? variant.getPrice() : variant.getItem().getPrice();
			return new SaleLineData(type, null, variant, id, variant.getSku(), request.getQuantity(), unitPrice);
		} else {
			throw new BadRequestException("type must be ITEM or VARIANT");
		}
	}

	private StockLevel lockStockLevel(SaleLineData d) {
		if ("ITEM".equals(d.type)) {
			// find stock level by item
			return stockLevelRepository.findByItemId(d.sellableId)
					.map(sl -> stockLevelRepository.findByIdForUpdate(sl.getId())
							.orElseThrow(() -> new NotFoundException("stock level not found: " + sl.getId())))
					.orElseThrow(() -> new NotFoundException("stock level not found for item: " + d.sellableId));
		} else { // VARIANT
			return stockLevelRepository.findByVariantId(d.sellableId)
					.map(sl -> stockLevelRepository.findByIdForUpdate(sl.getId())
							.orElseThrow(() -> new NotFoundException("stock level not found: " + sl.getId())))
					.orElseThrow(() -> new NotFoundException("stock level not found for variant: " + d.sellableId));
		}
	}

	private void checkAvailable(SaleLineData d, StockLevel sl) {
		if (d.quantity > sl.getQuantity()) {
			throw new ConflictException("not enough stock for " + d.type.toLowerCase() + " " + d.sku
					+ ": " + sl.getQuantity() + " available, " + d.quantity + " requested");
		}
	}

	private SaleResponse toSaleResponse(Sale sale) {
		SaleResponse res = new SaleResponse();
		res.setId(sale.getId());
		res.setCreatedAt(sale.getCreatedAt());
		res.setTotal(sale.getTotal());
		for (SaleLine sl : sale.getLines()) {
			SaleLineResponse lr = new SaleLineResponse();
			lr.setId(sl.getId());
			if (sl.getItem() != null) {
				lr.setType("ITEM");
				lr.setSellableId(sl.getItem().getId());
				lr.setSku(sl.getItem().getSku());
			} else {
				lr.setType("VARIANT");
				lr.setSellableId(sl.getVariant().getId());
				lr.setSku(sl.getVariant().getSku());
			}
			lr.setQuantity(sl.getQuantity());
			lr.setUnitPrice(sl.getUnitPrice());
			lr.setLineTotal(sl.getLineTotal());
			res.getLines().add(lr);
		}
		return res;
	}

	static class SaleLineData {
		String type;
		Item item;
		Variant variant;
		Long sellableId;
		String sku;
		int quantity;
		BigDecimal unitPrice;

		SaleLineData(String type, Item item, Variant variant, Long sellableId, String sku, int quantity,
				BigDecimal unitPrice) {
			this.type = type;
			this.item = item;
			this.variant = variant;
			this.sellableId = sellableId;
			this.sku = sku;
			this.quantity = quantity;
			this.unitPrice = unitPrice;
		}

		SaleLine createSaleLine(BigDecimal lineTotal) {
			if ("ITEM".equals(type)) {
				SaleLine line = SaleLine.forItem(item, quantity, unitPrice);
				line.setLineTotal(lineTotal);
				return line;
			} else {
				SaleLine line = SaleLine.forVariant(variant, quantity, unitPrice);
				line.setLineTotal(lineTotal);
				return line;
			}
		}
	}
}
