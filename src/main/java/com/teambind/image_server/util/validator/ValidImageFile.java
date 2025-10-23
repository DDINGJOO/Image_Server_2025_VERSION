package com.teambind.image_server.util.validator;


import jakarta.validation.Constraint;
import org.springframework.messaging.handler.annotation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ImageFileValidator.class)
public @interface ValidImageFile {
	String message() default "유효하지 않은 이미지 파일입니다";
	
	Class<?>[] groups() default {};
	
	Class<? extends Payload>[] payload() default {};
}
