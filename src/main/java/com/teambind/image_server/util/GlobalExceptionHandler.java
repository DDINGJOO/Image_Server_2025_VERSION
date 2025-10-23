package com.teambind.image_server.util;


import com.teambind.image_server.exception.CustomException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {
	
	@ExceptionHandler(CustomException.class)
	public ResponseEntity<?> handleCustomException(CustomException ex) {
		
		return ResponseEntity.status(ex.getStatus()).body(ex.getMessage());
	}
	
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidationExceptions(
			MethodArgumentNotValidException ex) {
		Map<String, Object> response = new HashMap<>();
		Map<String, String> errors = new HashMap<>();
		
		ex.getBindingResult()
				.getAllErrors()
				.forEach(
						(error) -> {
							String fieldName = ((FieldError) error).getField();
							String errorMessage = error.getDefaultMessage();
							errors.put(fieldName, errorMessage);
						});
		
		response.put("status", HttpStatus.BAD_REQUEST.value());
		response.put("error", "Validation Failed");
		response.put("message", "입력값 검증에 실패했습니다");
		response.put("fieldErrors", errors);
		
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}
}
