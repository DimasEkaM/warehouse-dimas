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

import java.math.BigDecimal;

/**
 * One line of a sale: which sellable, how many, and the unit price captured at the
 * moment of the sale. That price is snapshotted so editing the catalog later does
 * not rewrite sale history.
 */
@Entity
@Table(name = "sale_line")
@Check(name = "chk_sale_line_owner",
		constraints = "((item_id is not null and variant_id is null) or (item_id is null and variant_id is not null)) and quantity > 0")
public class SaleLine {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "sale_id", nullable = false)
	private Sale sale;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "item_id")
	private Item item;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "variant_id")
	private Variant variant;

	@Column(nullable = false)
	private int quantity;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal unitPrice;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal lineTotal;

	public static SaleLine forItem(Item item, int quantity, BigDecimal unitPrice) {
		SaleLine line = new SaleLine();
		line.item = item;
		line.quantity = quantity;
		line.unitPrice = unitPrice;
		line.lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
		return line;
	}

	public static SaleLine forVariant(Variant variant, int quantity, BigDecimal unitPrice) {
		SaleLine line = new SaleLine();
		line.variant = variant;
		line.quantity = quantity;
		line.unitPrice = unitPrice;
		line.lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
		return line;
	}

	public Long getId() {
		return id;
	}

	public Sale getSale() {
		return sale;
	}

	public void setSale(Sale sale) {
		this.sale = sale;
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

	public BigDecimal getUnitPrice() {
		return unitPrice;
	}

	public BigDecimal getLineTotal() {
		return lineTotal;
	}
}
