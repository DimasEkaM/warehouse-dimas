package com.dimasekam.warehouse.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ItemResponse {

	private Long id;
	private String sku;
	private String name;
	private String description;
	private BigDecimal price;

	private int stock;
	private boolean stockDerived;
	private int variantCount;

	// Filled on a single-item read; left empty on list reads.
	private List<VariantResponse> variants = new ArrayList<>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getSku() {
		return sku;
	}

	public void setSku(String sku) {
		this.sku = sku;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public int getStock() {
		return stock;
	}

	public void setStock(int stock) {
		this.stock = stock;
	}

	public boolean isStockDerived() {
		return stockDerived;
	}

	public void setStockDerived(boolean stockDerived) {
		this.stockDerived = stockDerived;
	}

	public int getVariantCount() {
		return variantCount;
	}

	public void setVariantCount(int variantCount) {
		this.variantCount = variantCount;
	}

	public List<VariantResponse> getVariants() {
		return variants;
	}

	public void setVariants(List<VariantResponse> variants) {
		this.variants = variants;
	}
}
