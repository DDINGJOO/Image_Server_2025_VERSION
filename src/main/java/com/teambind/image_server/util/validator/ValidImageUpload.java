package com.teambind.image_server.util.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ImageUploadRequestValidator.class)
public @interface ValidImageUpload {
	String message() default "단일 이미지 또는 다중 이미지 중 하나만 업로드해야 합니다";
	
	Class<?>[] groups() default {};
	
	Class<? extends Payload>[] payload() default {};
}
