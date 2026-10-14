package com.dimasekam.warehouse.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A stocked difference of an item, such as size or color. A variant is its own
 * sellable unit: it has its own SKU, its own optional price, and its own stock.
 */
@Entity
@Table(name = "variant")
public class Variant {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "item_id", nullable = false)
	private Item item;

	@Column(nullable = false, unique = true)
	private String sku;

	// Null means "use the item price". A value here overrides it.
	@Column(precision = 12, scale = 2)
	private BigDecimal price;

	@OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<VariantAttribute> attributes = new ArrayList<>();

	// Convenience for callers: keep both sides of the relationship in step.
	public void addAttribute(String name, String value) {
		VariantAttribute attribute = new VariantAttribute();
		attribute.setName(name);
		attribute.setValue(value);
		attribute.setVariant(this);
		this.attributes.add(attribute);
	}

	public Long getId() {
		return id;
	}

	public Item getItem() {
		return item;
	}

	public void setItem(Item item) {
		this.item = item;
	}

	public String getSku() {
		return sku;
	}

	public void setSku(String sku) {
		this.sku = sku;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public List<VariantAttribute> getAttributes() {
		return attributes;
	}
}
