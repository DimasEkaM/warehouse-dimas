package com.dimasekam.warehouse.service;

import com.dimasekam.warehouse.domain.Item;
import com.dimasekam.warehouse.domain.Variant;
import com.dimasekam.warehouse.dto.AttributeRequest;
import com.dimasekam.warehouse.dto.VariantCreateRequest;
import com.dimasekam.warehouse.dto.VariantResponse;
import com.dimasekam.warehouse.dto.VariantUpdateRequest;
import com.dimasekam.warehouse.exception.ConflictException;
import com.dimasekam.warehouse.exception.NotFoundException;
import com.dimasekam.warehouse.repository.ItemRepository;
import com.dimasekam.warehouse.repository.VariantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class VariantService {

	private final ItemRepository itemRepository;
	private final VariantRepository variantRepository;
	private final CatalogMapper catalogMapper;

	public VariantService(ItemRepository itemRepository, VariantRepository variantRepository, CatalogMapper catalogMapper) {
		this.itemRepository = itemRepository;
		this.variantRepository = variantRepository;
		this.catalogMapper = catalogMapper;
	}

	@Transactional
	public VariantResponse create(Long itemId, VariantCreateRequest request) {
		Item item = findItem(itemId);
		requireSkuFree(request.getSku());

		Variant variant = new Variant();
		variant.setItem(item);
		variant.setSku(request.getSku());
		variant.setPrice(request.getPrice());
		addAttributes(variant, request.getAttributes());

		return catalogMapper.toVariantResponse(variantRepository.save(variant));
	}

	@Transactional(readOnly = true)
	public List<VariantResponse> list(Long itemId) {
		// An unknown item is a 404 even when it has no variants to show.
		findItem(itemId);

		List<VariantResponse> responses = new ArrayList<>();
		for (Variant variant : variantRepository.findByItemId(itemId)) {
			responses.add(catalogMapper.toVariantResponse(variant));
		}
		return responses;
	}

	@Transactional
	public VariantResponse update(Long id, VariantUpdateRequest request) {
		Variant variant = findVariant(id);

		// Only the fields the caller supplied are changed.
		if (request.getSku() != null && !request.getSku().equals(variant.getSku())) {
			requireSkuFree(request.getSku());
			variant.setSku(request.getSku());
		}
		if (request.getPrice() != null) {
			variant.setPrice(request.getPrice());
		}
		if (request.getAttributes() != null) {
			// Sending attributes replaces the whole set; omitting them keeps the old set.
			variant.getAttributes().clear();
			addAttributes(variant, request.getAttributes());
		}

		return catalogMapper.toVariantResponse(variantRepository.save(variant));
	}

	@Transactional
	public void delete(Long id) {
		variantRepository.delete(findVariant(id));
	}

	private void addAttributes(Variant variant, List<AttributeRequest> attributes) {
		if (attributes == null) {
			return;
		}
		for (AttributeRequest attribute : attributes) {
			variant.addAttribute(attribute.getName(), attribute.getValue());
		}
	}

	private Item findItem(Long id) {
		return itemRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("item not found: " + id));
	}

	private Variant findVariant(Long id) {
		return variantRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("variant not found: " + id));
	}

	private void requireSkuFree(String sku) {
		if (itemRepository.existsBySku(sku) || variantRepository.existsBySku(sku)) {
			throw new ConflictException("sku already in use: " + sku);
		}
	}
}
