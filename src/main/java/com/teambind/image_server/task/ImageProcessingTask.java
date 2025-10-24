package com.teambind.image_server.task;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 이미지 처리 작업 DTO
 * <p>
 * Task Queue에 등록되는 이미지 변환/저장 작업 정보
 *
 * @author Image Server Team
 * @since 3.0
 */
@Data
@AllArgsConstructor
public class ImageProcessingTask {
	
	/**
	 * 이미지 ID (UUID)
	 */
	private String imageId;
	
	/**
	 * 업로드된 파일 (MultipartFile)
	 */
	private MultipartFile file;
	
	/**
	 * 저장 경로 (category/date/filename)
	 * 예: PRODUCT/2025/01/24/abc-123.webp
	 */
	private String storedPath;
	
	/**
	 * 원본 확장자 (대문자)
	 */
	private String originalExtension;
}
