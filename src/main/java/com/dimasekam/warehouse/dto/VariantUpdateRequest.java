package com.dimasekam.warehouse.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;

public class VariantUpdateRequest {

	@NotBlank(message = "sku must not be blank")
	private String sku;

	@DecimalMin(value = "0.00", message = "price must not be negative")
	@Digits(integer = 10, fraction = 2, message = "price must have at most 2 decimal places")
	private BigDecimal price;

	@Valid
	private List<AttributeRequest> attributes;

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

	public List<AttributeRequest> getAttributes() {
		return attributes;
	}

	public void setAttributes(List<AttributeRequest> attributes) {
		this.attributes = attributes;
	}
}
