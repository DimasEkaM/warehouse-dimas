package com.dimasekam.warehouse.dto;

/**
 * One field-level validation message inside an error response.
 */
public class FieldErrorResponse {

	private String field;
	private String message;

	public FieldErrorResponse(String field, String message) {
		this.field = field;
		this.message = message;
	}

	public String getField() {
		return field;
	}

	public String getMessage() {
		return message;
	}
}
