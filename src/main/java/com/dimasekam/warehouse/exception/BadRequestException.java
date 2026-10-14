package com.dimasekam.warehouse.exception;

/**
 * Thrown when a request is rejected for a reason the field checks do not cover.
 * The handler turns it into a 400.
 */
public class BadRequestException extends RuntimeException {

	public BadRequestException(String message) {
		super(message);
	}
}
