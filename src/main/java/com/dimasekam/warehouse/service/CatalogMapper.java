package com.dimasekam.warehouse.service;

import com.dimasekam.warehouse.domain.Item;
import com.dimasekam.warehouse.domain.StockLevel;
import com.dimasekam.warehouse.domain.Variant;
import com.dimasekam.warehouse.domain.VariantAttribute;
import com.dimasekam.warehouse.dto.AttributeResponse;
import com.dimasekam.warehouse.dto.ItemResponse;
import com.dimasekam.warehouse.dto.SellableResponse;
import com.dimasekam.warehouse.dto.VariantResponse;
import com.dimasekam.warehouse.repository.StockLevelRepository;
import com.dimasekam.warehouse.repository.VariantRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CatalogMapper {

	private final VariantRepository variantRepository;
	private final StockLevelRepository stockLevelRepository;

	public CatalogMapper(VariantRepository variantRepository, StockLevelRepository stockLevelRepository) {
		this.variantRepository = variantRepository;
		this.stockLevelRepository = stockLevelRepository;
	}

	public ItemResponse toItemResponse(Item item, boolean includeVariants) {
		List<Variant> variants = variantRepository.findByItemId(item.getId());

		ItemResponse response = new ItemResponse();
		response.setId(item.getId());
		response.setSku(item.getSku());
		response.setName(item.getName());
		response.setDescription(item.getDescription());
		response.setPrice(item.getPrice());
		response.setVariantCount(variants.size());

		if (variants.isEmpty()) {
			response.setStock(currentStockOfItem(item.getId()));
			response.setStockDerived(false);
		} else {
			int total = 0;
			for (Variant variant : variants) {
				total += currentStockOfVariant(variant.getId());
			}
			response.setStock(total);
			response.setStockDerived(true);
		}

		if (includeVariants) {
			List<VariantResponse> variantResponses = new ArrayList<>();
			for (Variant variant : variants) {
				variantResponses.add(toVariantResponse(variant));
			}
			response.setVariants(variantResponses);
		}
		return response;
	}

	public VariantResponse toVariantResponse(Variant variant) {
		VariantResponse response = new VariantResponse();
		response.setId(variant.getId());
		response.setItemId(variant.getItem().getId());
		response.setSku(variant.getSku());
		response.setPrice(variant.getPrice());

		if (variant.getPrice() != null) {
			response.setEffectivePrice(variant.getPrice());
			response.setPriceSource("VARIANT");
		} else {
			response.setEffectivePrice(variant.getItem().getPrice());
			response.setPriceSource("ITEM");
		}

		response.setStock(currentStockOfVariant(variant.getId()));

		List<AttributeResponse> attributes = new ArrayList<>();
		for (VariantAttribute attribute : variant.getAttributes()) {
			AttributeResponse attributeResponse = new AttributeResponse();
			attributeResponse.setName(attribute.getName());
			attributeResponse.setValue(attribute.getValue());
			attributes.add(attributeResponse);
		}
		response.setAttributes(attributes);
		return response;
	}

	public SellableResponse toSellableResponse(StockLevel level) {
		SellableResponse response = new SellableResponse();
		if (level.getItem() != null) {
			response.setType("ITEM");
			response.setId(level.getItem().getId());
			response.setSku(level.getItem().getSku());
		} else {
			response.setType("VARIANT");
			response.setId(level.getVariant().getId());
			response.setSku(level.getVariant().getSku());
		}
		return response;
	}

	private int currentStockOfItem(Long itemId) {
		return stockLevelRepository.findByItemId(itemId).map(StockLevel::getQuantity).orElse(0);
	}

	private int currentStockOfVariant(Long variantId) {
		return stockLevelRepository.findByVariantId(variantId).map(StockLevel::getQuantity).orElse(0);
	}
}
