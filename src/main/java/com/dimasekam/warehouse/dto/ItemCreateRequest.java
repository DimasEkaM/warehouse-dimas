package com.dimasekam.warehouse.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class ItemCreateRequest {

	@NotBlank(message = "sku is required")
	private String sku;

	@NotBlank(message = "name is required")
	private String name;

	private String description;

	@NotNull(message = "price is required")
	@DecimalMin(value = "0.00", message = "price must not be negative")
	@Digits(integer = 10, fraction = 2, message = "price must have at most 2 decimal places")
	private BigDecimal price;

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
}
