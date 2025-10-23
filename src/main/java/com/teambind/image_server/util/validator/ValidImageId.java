package com.teambind.image_server.util.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ImageIdValidator.class)
public @interface ValidImageId {
	String message() default "존재하지 않는 이미지 ID입니다";
	
	Class<?>[] groups() default {};
	
	Class<? extends Payload>[] payload() default {};
	
	boolean allowEmpty() default false; // 빈 문자열 허용 여부
}
