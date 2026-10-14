package com.dimasekam.warehouse.controller;

import com.dimasekam.warehouse.dto.StockAdjustmentRequest;
import com.dimasekam.warehouse.dto.StockAdjustmentResponse;
import com.dimasekam.warehouse.dto.StockMovementResponse;
import com.dimasekam.warehouse.service.StockService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class StockController {

	private final StockService stockService;

	public StockController(StockService stockService) {
		this.stockService = stockService;
	}

	@PostMapping("/items/{itemId}/stock")
	public StockAdjustmentResponse adjustItemStock(@PathVariable Long itemId,
			@Valid @RequestBody StockAdjustmentRequest request) {
		return stockService.adjustItemStock(itemId, request);
	}

	@PostMapping("/variants/{variantId}/stock")
	public StockAdjustmentResponse adjustVariantStock(@PathVariable Long variantId,
			@Valid @RequestBody StockAdjustmentRequest request) {
		return stockService.adjustVariantStock(variantId, request);
	}

	@GetMapping("/stock-movements")
	public List<StockMovementResponse> listMovements(
			@RequestParam(value = "itemId", required = false) Long itemId,
			@RequestParam(value = "variantId", required = false) Long variantId) {
		return stockService.listMovements(itemId, variantId);
	}
}
