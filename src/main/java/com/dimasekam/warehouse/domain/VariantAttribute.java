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

/**
 * One name/value pair on a variant, for example name = "size", value = "L".
 * Stored one row per pair rather than as a JSON column, so it can be queried
 * with plain SQL and does not need a custom converter.
 */
@Entity
@Table(name = "variant_attribute")
public class VariantAttribute {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "variant_id", nullable = false)
	private Variant variant;

	@Column(nullable = false)
	private String name;

	// VALUE is a reserved word in H2, so the column gets a different name.
	@Column(name = "attribute_value", nullable = false)
	private String value;

	public Long getId() {
		return id;
	}

	public Variant getVariant() {
		return variant;
	}

	public void setVariant(Variant variant) {
		this.variant = variant;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
