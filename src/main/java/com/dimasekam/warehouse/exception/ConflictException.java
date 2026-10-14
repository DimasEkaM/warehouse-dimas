package com.dimasekam.warehouse.exception;

/**
 * Thrown when a request is well formed but conflicts with current state, such as
 * a duplicate SKU, a delete with dependents, or an oversell. The handler turns it
 * into a 409.
 */
public class ConflictException extends RuntimeException {

	public ConflictException(String message) {
		super(message);
	}
}
