package com.teambind.image_server.util.validator;


import com.teambind.image_server.util.InitialSetup;
import org.springframework.stereotype.Component;


@Component
public class ReferenceValidator {
	public boolean referenceValidate(String reference) {
		return InitialSetup.ALL_REFERENCE_TYPE_MAP.containsKey(reference.toUpperCase());
	}
}
