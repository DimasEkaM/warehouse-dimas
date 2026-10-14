package com.dimasekam.warehouse.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A recorded sale: one or more lines, drawn down together. A sale is a fact, not
 * a workflow, so there is no status, no cancellation, and no shipment.
 */
@Entity
@Table(name = "sale")
public class Sale {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal total = BigDecimal.ZERO;

	@Column(nullable = false)
	private Instant createdAt;

	@OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<SaleLine> lines = new ArrayList<>();

	@PrePersist
	void onCreate() {
		this.createdAt = Instant.now();
	}

	// Keep both sides of the relationship in step as lines are added.
	public void addLine(SaleLine line) {
		line.setSale(this);
		this.lines.add(line);
	}

	public Long getId() {
		return id;
	}

	public BigDecimal getTotal() {
		return total;
	}

	public void setTotal(BigDecimal total) {
		this.total = total;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public List<SaleLine> getLines() {
		return lines;
	}
}
