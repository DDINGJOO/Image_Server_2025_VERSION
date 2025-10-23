package com.teambind.image_server.util.validator;

import com.teambind.image_server.repository.ImageRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ImageIdValidator implements ConstraintValidator<ValidImageId, String> {
	
	private final ImageRepository imageRepository;
	private boolean allowEmpty;
	
	@Override
	public void initialize(ValidImageId constraintAnnotation) {
		this.allowEmpty = constraintAnnotation.allowEmpty();
	}
	
	@Override
	public boolean isValid(String imageId, ConstraintValidatorContext context) {
		if (imageId == null || imageId.isBlank()) {
			return allowEmpty;
		}
		
		// 빈 문자열("")은 confirm 로직에서 "전체 삭제"를 의미하므로 allowEmpty=true인 경우만 허용
		if (imageId.isEmpty()) {
			return allowEmpty;
		}
		
		return imageRepository.existsById(imageId);
	}
}
