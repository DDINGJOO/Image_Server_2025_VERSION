package com.teambind.image_server.util.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReferenceTypeConstraintValidator implements ConstraintValidator<ValidReferenceType, String> {
	
	private final ReferenceTypeValidator referenceTypeValidator;
	
	@Override
	public void initialize(ValidReferenceType constraintAnnotation) {
		ConstraintValidator.super.initialize(constraintAnnotation);
	}
	
	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if (value == null || value.isBlank()) {
			return false;
		}
		return referenceTypeValidator.referenceTypeValidate(value);
	}
}
