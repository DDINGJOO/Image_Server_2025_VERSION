package com.teambind.image_server.util.helper;


import com.teambind.image_server.exception.CustomException;
import com.teambind.image_server.exception.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class ExtensionParser {
	
	public String extensionParse(String fileName) {
		if (fileName == null || fileName.isBlank()) {
			throw new CustomException(ErrorCode.INVALID_FILE_NAME);
		}
		
		// Path traversal 검증
		if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
			throw new CustomException(ErrorCode.INVALID_FILE_NAME);
		}
		
		int dotIndex = fileName.lastIndexOf(".");
		if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
			throw new CustomException(ErrorCode.FILE_EXTENSION_NOT_FOUND);
		}
		
		String extension = fileName.substring(dotIndex + 1);
		
		// 이중 확장자 검증
		if (extension.contains(".")) {
			throw new CustomException(ErrorCode.INVALID_FILE_EXTENSION);
		}
		return extension.toUpperCase();
	}
	
}
