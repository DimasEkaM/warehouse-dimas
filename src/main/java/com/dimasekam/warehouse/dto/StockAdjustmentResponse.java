package com.dimasekam.warehouse.dto;

public class StockAdjustmentResponse {

	private Long stockLevelId;
	private SellableResponse sellable;
	private int quantity;
	private Long movementId;

	public Long getStockLevelId() {
		return stockLevelId;
	}

	public void setStockLevelId(Long stockLevelId) {
		this.stockLevelId = stockLevelId;
	}

	public SellableResponse getSellable() {
		return sellable;
	}

	public void setSellable(SellableResponse sellable) {
		this.sellable = sellable;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public Long getMovementId() {
		return movementId;
	}

	public void setMovementId(Long movementId) {
		this.movementId = movementId;
	}
}
