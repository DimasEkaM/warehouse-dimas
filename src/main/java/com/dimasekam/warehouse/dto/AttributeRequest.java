package com.dimasekam.warehouse.dto;

import jakarta.validation.constraints.NotBlank;

public class AttributeRequest {

	@NotBlank(message = "attribute name is required")
	private String name;

	@NotBlank(message = "attribute value is required")
	private String value;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
