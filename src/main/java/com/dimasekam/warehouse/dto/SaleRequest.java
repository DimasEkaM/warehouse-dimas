package com.dimasekam.warehouse.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SaleRequest {

	@Valid
	@NotEmpty(message = "at least one line is required")
	private List<SaleLineRequest> lines = new ArrayList<>();

	public List<SaleLineRequest> getLines() {
		return lines;
	}

	public void setLines(List<SaleLineRequest> lines) {
		this.lines = lines;
	}
}
