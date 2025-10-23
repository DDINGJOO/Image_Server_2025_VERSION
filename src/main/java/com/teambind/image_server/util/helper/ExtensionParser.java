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

		// 확장자만 있는 파일명 검증 (숨겨진 파일)
		if (dotIndex == 0) {
			throw new CustomException(ErrorCode.FILE_EXTENSION_NOT_FOUND);
		}

		// 이중 확장자 검증 - 파일명 부분에 . 이 여러 개 있는지 확인
		String fileNameWithoutExtension = fileName.substring(0, dotIndex);
		if (fileNameWithoutExtension.contains(".")) {
			throw new CustomException(ErrorCode.INVALID_FILE_EXTENSION);
		}

		String extension = fileName.substring(dotIndex + 1);

		// 확장자 자체에 . 이 포함된 경우 검증
		if (extension.contains(".")) {
			throw new CustomException(ErrorCode.INVALID_FILE_EXTENSION);
		}
		return extension.toUpperCase();
	}
	
}
