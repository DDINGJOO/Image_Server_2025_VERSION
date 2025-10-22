package com.teambind.image_server.util.validator;


import com.teambind.image_server.util.InitialSetup;
import org.springframework.stereotype.Component;


@Component
public class ReferenceTypeValidator {
	public boolean referenceTypeValidate(String reference) {
		return InitialSetup.ALL_REFERENCE_TYPE_MAP.containsKey(reference.toUpperCase());
	}
	
	public boolean isMonoImageReferenceType(String reference) {
		return InitialSetup.MONO_IMAGE_REFERENCE_TYPE_MAP.containsKey(reference.toUpperCase());
	}
	
	public boolean isMultiImageReferenceType(String reference) {
		return InitialSetup.MULTI_IMAGE_REFERENCE_TYPE_MAP.containsKey(reference.toUpperCase());
	}
}
