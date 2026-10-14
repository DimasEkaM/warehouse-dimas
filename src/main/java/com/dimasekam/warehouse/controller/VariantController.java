package com.dimasekam.warehouse.controller;

import com.dimasekam.warehouse.dto.VariantCreateRequest;
import com.dimasekam.warehouse.dto.VariantResponse;
import com.dimasekam.warehouse.dto.VariantUpdateRequest;
import com.dimasekam.warehouse.service.VariantService;
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
@RequestMapping("/api/v1")
public class VariantController {

	private final VariantService variantService;

	public VariantController(VariantService variantService) {
		this.variantService = variantService;
	}

	@PostMapping("/items/{itemId}/variants")
	@ResponseStatus(HttpStatus.CREATED)
	public VariantResponse create(@PathVariable Long itemId, @Valid @RequestBody VariantCreateRequest request) {
		return variantService.create(itemId, request);
	}

	@GetMapping("/items/{itemId}/variants")
	public List<VariantResponse> list(@PathVariable Long itemId) {
		return variantService.list(itemId);
	}

	@PutMapping("/variants/{id}")
	public VariantResponse update(@PathVariable Long id, @Valid @RequestBody VariantUpdateRequest request) {
		return variantService.update(id, request);
	}

	@DeleteMapping("/variants/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		variantService.delete(id);
	}
}
