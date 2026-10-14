package com.dimasekam.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One row per change to a stock count, so the count can be explained later.
 * A movement is a fact: once written it is never edited, which is why there are
 * no setters. The sum of the deltas for a stock level equals its current
 * quantity.
 */
@Entity
@Table(name = "stock_movement")
public class StockMovement {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "stock_level_id", nullable = false)
	private StockLevel stockLevel;

	@Column(nullable = false)
	private int delta;

	@Column(nullable = false)
	private int quantityAfter;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private StockMovementReason reason;

	// Only set when the reason is SALE: the sale id that caused this change.
	@Column(name = "reference_id")
	private Long referenceId;

	@Column(nullable = false)
	private Instant createdAt;

	@PrePersist
	void onCreate() {
		this.createdAt = Instant.now();
	}

	public static StockMovement of(StockLevel stockLevel, int delta, int quantityAfter,
			StockMovementReason reason, Long referenceId) {
		StockMovement movement = new StockMovement();
		movement.stockLevel = stockLevel;
		movement.delta = delta;
		movement.quantityAfter = quantityAfter;
		movement.reason = reason;
		movement.referenceId = referenceId;
		return movement;
	}

	public Long getId() {
		return id;
	}

	public StockLevel getStockLevel() {
		return stockLevel;
	}

	public int getDelta() {
		return delta;
	}

	public int getQuantityAfter() {
		return quantityAfter;
	}

	public StockMovementReason getReason() {
		return reason;
	}

	public Long getReferenceId() {
		return referenceId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
