package com.dimasekam.warehouse.dto;

import java.math.BigDecimal;

public class SaleLineResponse {

	private Long id;
	private String type;
	private Long sellableId;
	private String sku;
	private int quantity;
	private BigDecimal unitPrice;
	private BigDecimal lineTotal;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Long getSellableId() {
		return sellableId;
	}

	public void setSellableId(Long sellableId) {
		this.sellableId = sellableId;
	}

	public String getSku() {
		return sku;
	}

	public void setSku(String sku) {
		this.sku = sku;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public BigDecimal getUnitPrice() {
		return unitPrice;
	}

	public void setUnitPrice(BigDecimal unitPrice) {
		this.unitPrice = unitPrice;
	}

	public BigDecimal getLineTotal() {
		return lineTotal;
	}

	public void setLineTotal(BigDecimal lineTotal) {
		this.lineTotal = lineTotal;
	}
}
