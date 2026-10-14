package com.dimasekam.warehouse.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class VariantResponse {

	private Long id;
	private Long itemId;
	private String sku;

	// The variant's own price, or null when it falls back to the item price.
	private BigDecimal price;

	private BigDecimal effectivePrice;

	// "VARIANT" or "ITEM".
	private String priceSource;

	private int stock;
	private List<AttributeResponse> attributes = new ArrayList<>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getItemId() {
		return itemId;
	}

	public void setItemId(Long itemId) {
		this.itemId = itemId;
	}

	public String getSku() {
		return sku;
	}

	public void setSku(String sku) {
		this.sku = sku;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public BigDecimal getEffectivePrice() {
		return effectivePrice;
	}

	public void setEffectivePrice(BigDecimal effectivePrice) {
		this.effectivePrice = effectivePrice;
	}

	public String getPriceSource() {
		return priceSource;
	}

	public void setPriceSource(String priceSource) {
		this.priceSource = priceSource;
	}

	public int getStock() {
		return stock;
	}

	public void setStock(int stock) {
		this.stock = stock;
	}

	public List<AttributeResponse> getAttributes() {
		return attributes;
	}

	public void setAttributes(List<AttributeResponse> attributes) {
		this.attributes = attributes;
	}
}
