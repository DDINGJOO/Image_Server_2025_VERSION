package com.teambind.image_server.util.validator;


import org.springframework.stereotype.Component;

import static com.teambind.image_server.ImageServerApplication.referenceTypeMap;

@Component
public class ReferenceValidator {
	public boolean referenceValidate(String reference) {
		return referenceTypeMap.containsKey(reference.toUpperCase());
	}
}
