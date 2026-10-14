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
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Central point for turning every unhandled situation into a predictable JSON error.
 *
 * By default Spring Boot's default ErrorController returns HTML for 4xx/5xx when
 * no @RestControllerAdvice is present. Adding this bean makes every response JSON.
 *
 * The handler order matters: more specific exceptions must be listed before broader ones.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	/** 404 – the client asked for a path that does not exist. */
	@ExceptionHandler(NoHandlerFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleNotFound(NoHandlerFoundException ex, WebRequest request) {
		return build(HttpStatus.NOT_FOUND, "not found", request, List.of());
	}

	/** 404 – a DTO with @NotFoundException was thrown explicitly. */
	@ExceptionHandler(NotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleNotFoundEx(NotFoundException ex, WebRequest request) {
		return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
	}

	/** 409 – a conflict (duplicate SKU, sale oversell, delete with dependents). */
	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<ApiErrorResponse> handleConflict(ConflictException ex, WebRequest request) {
		return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
	}

	/** 400 – a bad request (blank name/sku/price, negative quantity, malformed JSON). */
	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<ApiErrorResponse> handleBadRequest(BadRequestException ex, WebRequest request) {
		return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, List.of());
	}

	/** 400 – @Valid validation failures (field errors, method errors). */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex,
			WebRequest request) {
		List<FieldErrorResponse> fieldErrors = new ArrayList<>();
		for (org.springframework.validation.FieldError fe : ex.getBindingResult().getFieldErrors()) {
			fieldErrors.add(new FieldErrorResponse(fe.getField(), fe.getDefaultMessage()));
		}
		return build(HttpStatus.BAD_REQUEST, "validation failed", request, fieldErrors);
	}

	/** 400 – a request body that is not valid JSON or has wrong types. */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiErrorResponse> handleUnreadable(HttpMessageNotReadableException ex,
			WebRequest request) {
		return build(HttpStatus.BAD_REQUEST, "request body is missing or malformed", request, List.of());
	}

	/** 415 – the declared Content-Type is not supported by this endpoint. */
	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ApiErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
			WebRequest request) {
		return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "unsupported media type", request, List.of());
	}

	/** 405 – the HTTP method is not supported for this endpoint. */
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
			WebRequest request) {
		return build(HttpStatus.METHOD_NOT_ALLOWED, "method not allowed", request, List.of());
	}

	/** 500 – anything else. Log the detail, return a generic message. */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest http, WebRequest request) {
		String path = http.getRequestURI();
		if (path != null && path.startsWith("/api/v1")) {
			return build(HttpStatus.NOT_FOUND, "not found", request, List.of());
		}
		String desc = request.getDescription(false);
		String d = desc != null && !desc.isEmpty() ? desc : "unknown";
		log.error("Unhandled exception on {} {}", d, ex);
		return build(HttpStatus.INTERNAL_SERVER_ERROR, "unexpected error", request, List.of());
	}

	private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String message, WebRequest request,
			List<FieldErrorResponse> fieldErrors) {
		ApiErrorResponse body = new ApiErrorResponse();
		body.setTimestamp(Instant.now());
		body.setStatus(status.value());
		body.setError(status.getReasonPhrase());
		body.setMessage(message);
		String desc = request.getDescription(false);
		String path = desc != null && !desc.isEmpty() ? desc : "unknown";
		body.setPath(path);
		body.setFieldErrors(fieldErrors);
		return ResponseEntity.status(status).body(body);
	}
}