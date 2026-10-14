package com.dimasekam.warehouse.exception;

/**
 * Thrown when a requested id does not exist. The handler turns it into a 404.
 */
public class NotFoundException extends RuntimeException {

	public NotFoundException(String message) {
		super(message);
	}
}
