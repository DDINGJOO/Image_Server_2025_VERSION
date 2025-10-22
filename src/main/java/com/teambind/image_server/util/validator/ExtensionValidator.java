package com.teambind.image_server.util.validator;

import com.teambind.image_server.util.helper.ExtensionParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.teambind.image_server.ImageServerApplication.extensionMap;

@Component
@RequiredArgsConstructor
public class ExtensionValidator {
	private final ExtensionParser parser;
	
	public boolean isValid(String fileName) {
		String extension = parser.extensionParse(fileName);
		return extensionMap.containsKey(extension);
	}
	
	
}
