package com.dimasekam.warehouse.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SaleLineRequest {

	@NotBlank(message = "type is required")
	private String type;

	@NotNull(message = "id is required")
	private Long id;

	@NotNull(message = "quantity is required")
	@Min(value = 1, message = "quantity must be greater than zero")
	private Integer quantity;

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

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}
}
