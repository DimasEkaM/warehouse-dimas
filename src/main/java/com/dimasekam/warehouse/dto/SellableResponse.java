package com.dimasekam.warehouse.dto;

/**
 * Points at the thing a stock row belongs to. "ITEM" for a plain product,
 * "VARIANT" for a stocked difference. Carried in stock responses so a client
 * never has to look the id up again.
 */
public class SellableResponse {

	private String type;
	private Long id;
	private String sku;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

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
}
