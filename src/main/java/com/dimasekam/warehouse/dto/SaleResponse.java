package com.dimasekam.warehouse.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class SaleResponse {

	private Long id;
	private Instant createdAt;
	private List<SaleLineResponse> lines = new ArrayList<>();
	private BigDecimal total;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public List<SaleLineResponse> getLines() {
		return lines;
	}

	public void setLines(List<SaleLineResponse> lines) {
		this.lines = lines;
	}

	public BigDecimal getTotal() {
		return total;
	}

	public void setTotal(BigDecimal total) {
		this.total = total;
	}
}
