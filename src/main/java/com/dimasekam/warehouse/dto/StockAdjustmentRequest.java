package com.dimasekam.warehouse.dto;

import com.dimasekam.warehouse.domain.StockMovementReason;
import jakarta.validation.constraints.NotNull;

public class StockAdjustmentRequest {

	@NotNull(message = "delta is required")
	private Integer delta;

	@NotNull(message = "reason is required")
	private StockMovementReason reason;

	public Integer getDelta() {
		return delta;
	}

	public void setDelta(Integer delta) {
		this.delta = delta;
	}

	public StockMovementReason getReason() {
		return reason;
	}

	public void setReason(StockMovementReason reason) {
		this.reason = reason;
	}
}
