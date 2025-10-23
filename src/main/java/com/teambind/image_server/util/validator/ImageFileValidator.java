package com.teambind.image_server.util.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ImageFileValidator implements ConstraintValidator<ValidImageFile, Object> {
	
	private final ExtensionValidator extensionValidator;
	
	@Override
	public void initialize(ValidImageFile constraintAnnotation) {
		ConstraintValidator.super.initialize(constraintAnnotation);
	}
	
	@Override
	public boolean isValid(Object value, ConstraintValidatorContext context) {
		if (value == null) {
			// null은 허용 (필수 여부는 @NotNull로 별도 검증)
			return true;
		}
		
		if (value instanceof MultipartFile) {
			return validateSingleFile((MultipartFile) value, context);
		}
		
		if (value instanceof List) {
			return validateMultipleFiles((List<?>) value, context);
		}
		
		return false;
	}
	
	private boolean validateSingleFile(MultipartFile file, ConstraintValidatorContext context) {
		if (file.isEmpty()) {
			return true; // 빈 파일은 허용 (필수 여부는 별도 검증)
		}
		
		String originalFilename = file.getOriginalFilename();
		if (originalFilename == null || originalFilename.isBlank()) {
			context.disableDefaultConstraintViolation();
			context.buildConstraintViolationWithTemplate("파일명이 유효하지 않습니다")
					.addConstraintViolation();
			return false;
		}
		
		try {
			if (!extensionValidator.isValid(originalFilename)) {
				context.disableDefaultConstraintViolation();
				context.buildConstraintViolationWithTemplate("지원하지 않는 파일 확장자입니다")
						.addConstraintViolation();
				return false;
			}
		} catch (Exception e) {
			context.disableDefaultConstraintViolation();
			context.buildConstraintViolationWithTemplate("파일 검증 중 오류가 발생했습니다: " + e.getMessage())
					.addConstraintViolation();
			return false;
		}
		
		return true;
	}
	
	private boolean validateMultipleFiles(List<?> files, ConstraintValidatorContext context) {
		if (files.isEmpty()) {
			return true; // 빈 리스트는 허용
		}
		
		for (int i = 0; i < files.size(); i++) {
			Object item = files.get(i);
			if (!(item instanceof MultipartFile)) {
				context.disableDefaultConstraintViolation();
				context.buildConstraintViolationWithTemplate("유효하지 않은 파일 형식입니다")
						.addConstraintViolation();
				return false;
			}
			
			MultipartFile file = (MultipartFile) item;
			if (!file.isEmpty()) {
				String originalFilename = file.getOriginalFilename();
				if (originalFilename == null || originalFilename.isBlank()) {
					context.disableDefaultConstraintViolation();
					context.buildConstraintViolationWithTemplate(String.format("파일[%d]의 파일명이 유효하지 않습니다", i))
							.addConstraintViolation();
					return false;
				}
				
				try {
					if (!extensionValidator.isValid(originalFilename)) {
						context.disableDefaultConstraintViolation();
						context.buildConstraintViolationWithTemplate(String.format("파일[%d]의 확장자가 지원되지 않습니다", i))
								.addConstraintViolation();
						return false;
					}
				} catch (Exception e) {
					context.disableDefaultConstraintViolation();
					context.buildConstraintViolationWithTemplate(String.format("파일[%d] 검증 중 오류가 발생했습니다: %s", i, e.getMessage()))
							.addConstraintViolation();
					return false;
				}
			}
		}
		
		return true;
	}
}
