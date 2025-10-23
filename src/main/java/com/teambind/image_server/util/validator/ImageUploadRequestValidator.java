package com.teambind.image_server.util.validator;

import com.teambind.image_server.dto.request.ImageUploadRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ImageUploadRequestValidator implements ConstraintValidator<ValidImageUpload, ImageUploadRequest> {
	
	private final ReferenceTypeValidator referenceTypeValidator;
	
	@Override
	public void initialize(ValidImageUpload constraintAnnotation) {
		ConstraintValidator.super.initialize(constraintAnnotation);
	}
	
	@Override
	public boolean isValid(ImageUploadRequest request, ConstraintValidatorContext context) {
		if (request == null) {
			return true;
		}
		
		boolean hasSingleFile = request.isSingleUpload();
		boolean hasMultipleFiles = request.isMultiUpload();
		
		// 1. 단일과 다중을 동시에 보낼 수 없음
		if (hasSingleFile && hasMultipleFiles) {
			context.disableDefaultConstraintViolation();
			context.buildConstraintViolationWithTemplate("단일 이미지와 다중 이미지를 동시에 업로드할 수 없습니다")
					.addConstraintViolation();
			return false;
		}
		
		// 2. 둘 다 없으면 안됨
		if (!hasSingleFile && !hasMultipleFiles) {
			context.disableDefaultConstraintViolation();
			context.buildConstraintViolationWithTemplate("업로드할 이미지 파일이 없습니다")
					.addConstraintViolation();
			return false;
		}
		
		// 3. 카테고리가 단일 이미지만 허용하는데 다중 이미지를 보낸 경우
		if (hasMultipleFiles && request.getCategory() != null) {
			if (referenceTypeValidator.isMonoImageReferenceType(request.getCategory())) {
				context.disableDefaultConstraintViolation();
				context.buildConstraintViolationWithTemplate(
								String.format("카테고리 '%s'는 단일 이미지만 허용합니다", request.getCategory()))
						.addConstraintViolation();
				return false;
			}
		}
		
		// 4. 카테고리가 다중 이미지만 허용하는데 단일 이미지를 보낸 경우
		if (hasSingleFile && request.getCategory() != null) {
			if (referenceTypeValidator.isMultiImageReferenceType(request.getCategory())) {
				context.disableDefaultConstraintViolation();
				context.buildConstraintViolationWithTemplate(
								String.format("카테고리 '%s'는 다중 이미지만 허용합니다", request.getCategory()))
						.addConstraintViolation();
				return false;
			}
		}
		
		return true;
	}
}
