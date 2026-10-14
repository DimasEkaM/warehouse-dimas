package com.dimasekam.warehouse.controller;

import com.dimasekam.warehouse.dto.ApiErrorResponse;
import com.dimasekam.warehouse.dto.FieldErrorResponse;
import com.dimasekam.warehouse.exception.BadRequestException;
import com.dimasekam.warehouse.exception.ConflictException;
import com.dimasekam.warehouse.exception.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(NotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleNotFound(NotFoundException ex, HttpServletRequest request) {
		return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
	}

	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<ApiErrorResponse> handleConflict(ConflictException ex, HttpServletRequest request) {
		return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
	}

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<ApiErrorResponse> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, List.of());
	}

	// A request body that failed @Valid checks.
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		List<FieldErrorResponse> fieldErrors = new ArrayList<>();
		for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
			fieldErrors.add(new FieldErrorResponse(fieldError.getField(), fieldError.getDefaultMessage()));
		}
		return build(HttpStatus.BAD_REQUEST, "validation failed", request, fieldErrors);
	}

	// A path or query value that failed a constraint, such as a negative id.
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
			HttpServletRequest request) {
		List<FieldErrorResponse> fieldErrors = new ArrayList<>();
		ex.getConstraintViolations().forEach(violation ->
				fieldErrors.add(new FieldErrorResponse(violation.getPropertyPath().toString(), violation.getMessage())));
		return build(HttpStatus.BAD_REQUEST, "validation failed", request, fieldErrors);
	}

	// A body that is not valid JSON, or has the wrong types.
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiErrorResponse> handleUnreadable(HttpMessageNotReadableException ex,
			HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, "request body is missing or malformed", request, List.of());
	}

	// Anything else. Log the detail, return a generic message.
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
		log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
		return build(HttpStatus.INTERNAL_SERVER_ERROR, "unexpected error", request, List.of());
	}

	private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String message, HttpServletRequest request,
			List<FieldErrorResponse> fieldErrors) {
		ApiErrorResponse body = new ApiErrorResponse();
		body.setTimestamp(Instant.now());
		body.setStatus(status.value());
		body.setError(status.getReasonPhrase());
		body.setMessage(message);
		body.setPath(request.getRequestURI());
		body.setFieldErrors(fieldErrors);
		return ResponseEntity.status(status).body(body);
	}
}
