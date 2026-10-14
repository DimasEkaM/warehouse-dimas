package com.dimasekam.warehouse.controller;

import com.dimasekam.warehouse.dto.SaleRequest;
import com.dimasekam.warehouse.dto.SaleResponse;
import com.dimasekam.warehouse.service.SaleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sales")
public class SaleController {

	private final SaleService saleService;

	public SaleController(SaleService saleService) {
		this.saleService = saleService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public SaleResponse create(@Valid @RequestBody SaleRequest request) {
		return saleService.createSale(request);
	}

	@GetMapping("/{id}")
	public SaleResponse get(@PathVariable Long id) {
		return saleService.getSale(id);
	}
}
