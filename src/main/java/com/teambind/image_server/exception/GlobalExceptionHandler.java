package com.teambind.image_server.exception;


import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
	
	@ExceptionHandler(CustomException.class)
	public ResponseEntity<ErrorResponse> handleProfileException(
			CustomException ex, HttpServletRequest request) {
		ErrorCode profileErrorCode = ex.getErrorcode();
		HttpStatus status = profileErrorCode.getStatus();

		log.error("CustomException occurred: code={}, message={}, path={}",
				profileErrorCode.getErrCode(),
				profileErrorCode.getMessage(),
				request.getRequestURI(),
				ex);

		ErrorResponse body =
				ErrorResponse.of(
						status.value(),
						profileErrorCode.getErrCode(),
						profileErrorCode.getMessage(),
						request.getRequestURI());
		return ResponseEntity.status(status).body(body);
	}
	
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex,
			HttpHeaders headers,
			org.springframework.http.HttpStatusCode status,
			WebRequest request) {
		String message =
				ex.getBindingResult().getFieldErrors().stream()
						.findFirst()
						.map(err -> err.getField() + ": " + err.getDefaultMessage())
						.orElse("Validation failed");

		log.error("Validation failed: {}, path={}",
				message,
				request.getDescription(false),
				ex);

		ErrorResponse body =
				ErrorResponse.of(
						HttpStatus.BAD_REQUEST.value(),
						"VALIDATION_ERROR",
						message,
						request.getDescription(false));
		return ResponseEntity.badRequest().body(body);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGeneralException(
			Exception ex, HttpServletRequest request) {
		log.error("Unexpected exception occurred: path={}",
				request.getRequestURI(),
				ex);

		ErrorResponse body =
				ErrorResponse.of(
						HttpStatus.INTERNAL_SERVER_ERROR.value(),
						"INTERNAL_SERVER_ERROR",
						"An unexpected error occurred. Please try again later.",
						request.getRequestURI());
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
	}
}
