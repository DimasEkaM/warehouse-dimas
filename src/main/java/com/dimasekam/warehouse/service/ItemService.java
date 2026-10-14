package com.dimasekam.warehouse.service;

import com.dimasekam.warehouse.domain.Item;
import com.dimasekam.warehouse.dto.ItemCreateRequest;
import com.dimasekam.warehouse.dto.ItemResponse;
import com.dimasekam.warehouse.dto.ItemUpdateRequest;
import com.dimasekam.warehouse.exception.ConflictException;
import com.dimasekam.warehouse.exception.NotFoundException;
import com.dimasekam.warehouse.repository.ItemRepository;
import com.dimasekam.warehouse.repository.VariantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ItemService {

	private final ItemRepository itemRepository;
	private final VariantRepository variantRepository;
	private final CatalogMapper catalogMapper;

	public ItemService(ItemRepository itemRepository, VariantRepository variantRepository, CatalogMapper catalogMapper) {
		this.itemRepository = itemRepository;
		this.variantRepository = variantRepository;
		this.catalogMapper = catalogMapper;
	}

	@Transactional
	public ItemResponse create(ItemCreateRequest request) {
		requireSkuFree(request.getSku());

		Item item = new Item();
		item.setSku(request.getSku());
		item.setName(request.getName());
		item.setDescription(request.getDescription());
		item.setPrice(request.getPrice());

		return catalogMapper.toItemResponse(itemRepository.save(item), false);
	}

	@Transactional(readOnly = true)
	public List<ItemResponse> list() {
		List<ItemResponse> responses = new ArrayList<>();
		for (Item item : itemRepository.findAll()) {
			responses.add(catalogMapper.toItemResponse(item, false));
		}
		return responses;
	}

	@Transactional(readOnly = true)
	public ItemResponse get(Long id) {
		return catalogMapper.toItemResponse(findItem(id), true);
	}

	@Transactional
	public ItemResponse update(Long id, ItemUpdateRequest request) {
		Item item = findItem(id);

		// Only the fields the caller supplied are changed (FR-01).
		if (request.getSku() != null && !request.getSku().equals(item.getSku())) {
			requireSkuFree(request.getSku());
			item.setSku(request.getSku());
		}
		if (request.getName() != null) {
			item.setName(request.getName());
		}
		if (request.getDescription() != null) {
			item.setDescription(request.getDescription());
		}
		if (request.getPrice() != null) {
			item.setPrice(request.getPrice());
		}

		return catalogMapper.toItemResponse(itemRepository.save(item), true);
	}

	@Transactional
	public void delete(Long id) {
		Item item = findItem(id);

		if (variantRepository.countByItemId(id) > 0) {
			throw new ConflictException("item " + item.getSku() + " has variants and cannot be deleted");
		}
		itemRepository.delete(item);
	}

	private Item findItem(Long id) {
		return itemRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("item not found: " + id));
	}

	private void requireSkuFree(String sku) {
		if (itemRepository.existsBySku(sku) || variantRepository.existsBySku(sku)) {
			throw new ConflictException("sku already in use: " + sku);
		}
	}
}
