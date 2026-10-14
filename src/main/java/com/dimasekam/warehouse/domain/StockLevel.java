package com.dimasekam.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Check;

/**
 * The stock count for one sellable unit. A row points at either an item (a simple
 * product) or a variant (a stocked difference), never both. Items that carry
 * variants do not get a stock row: their variants hold the counts, and the item
 * quantity is only ever derived by summing them.
 *
 * The count never goes below zero. The service enforces that before writing, and
 * the database check constraint refuses it a second time.
 */
@Entity
@Table(name = "stock_level")
@Check(name = "chk_stock_level_owner",
		constraints = "((item_id is not null and variant_id is null) or (item_id is null and variant_id is not null)) and quantity >= 0")
public class StockLevel {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "item_id")
	private Item item;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "variant_id")
	private Variant variant;

	@Column(nullable = false)
	private int quantity;

	// A stock row starts at zero and is moved by restocks and sales.
	public static StockLevel forItem(Item item) {
		StockLevel level = new StockLevel();
		level.item = item;
		return level;
	}

	public static StockLevel forVariant(Variant variant) {
		StockLevel level = new StockLevel();
		level.variant = variant;
		return level;
	}

	public Long getId() {
		return id;
	}

	public Item getItem() {
		return item;
	}

	public Variant getVariant() {
		return variant;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}
}
