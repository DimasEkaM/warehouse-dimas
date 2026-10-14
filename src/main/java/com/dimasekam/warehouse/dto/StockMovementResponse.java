package com.dimasekam.warehouse.dto;

import java.time.Instant;

public class StockMovementResponse {

	private Long id;
	private SellableResponse sellable;
	private int delta;
	private int quantityAfter;
	private String reason;
	private Long referenceId;
	private Instant createdAt;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public SellableResponse getSellable() {
		return sellable;
	}

	public void setSellable(SellableResponse sellable) {
		this.sellable = sellable;
	}

	public int getDelta() {
		return delta;
	}

	public void setDelta(int delta) {
		this.delta = delta;
	}

	public int getQuantityAfter() {
		return quantityAfter;
	}

	public void setQuantityAfter(int quantityAfter) {
		this.quantityAfter = quantityAfter;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public Long getReferenceId() {
		return referenceId;
	}

	public void setReferenceId(Long referenceId) {
		this.referenceId = referenceId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
}
