package com.dimasekam.warehouse.controller;

import com.dimasekam.warehouse.dto.ItemCreateRequest;
import com.dimasekam.warehouse.dto.ItemResponse;
import com.dimasekam.warehouse.dto.ItemUpdateRequest;
import com.dimasekam.warehouse.service.ItemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/items")
public class ItemController {

	private final ItemService itemService;

	public ItemController(ItemService itemService) {
		this.itemService = itemService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ItemResponse create(@Valid @RequestBody ItemCreateRequest request) {
		return itemService.create(request);
	}

	@GetMapping
	public List<ItemResponse> list() {
		return itemService.list();
	}

	@GetMapping("/{id}")
	public ItemResponse get(@PathVariable Long id) {
		return itemService.get(id);
	}

	@PutMapping("/{id}")
	public ItemResponse update(@PathVariable Long id, @Valid @RequestBody ItemUpdateRequest request) {
		return itemService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		itemService.delete(id);
	}
}
