package com.dimasekam.warehouse.domain;

/**
 * Why a stock count changed. RESTOCK is goods arriving, ADJUSTMENT is a manual
 * correction, and SALE is a sale drawing the count down.
 */
public enum StockMovementReason {
	SALE,
	RESTOCK,
	ADJUSTMENT
}
