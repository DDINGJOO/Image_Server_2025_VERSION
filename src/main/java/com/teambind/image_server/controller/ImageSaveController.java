package com.teambind.image_server.controller;


import com.teambind.image_server.dto.request.ImageUploadRequest;
import com.teambind.image_server.service.ImageSaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/images")
public class ImageSaveController {
	private final ImageSaveService imageSaveService;
	
	/**
	 * 이미지 업로드 API
	 * - 단일 이미지: file 파라미터 사용
	 * - 다중 이미지: files 파라미터 사용
	 * - 카테고리별 단일/다중 제약사항은 자동으로 검증됨
	 *
	 * @param request 이미지 업로드 요청 (단일 또는 다중)
	 * @return 업로드된 이미지 정보 (URL 등)
	 */
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Map<String, String>> saveImage(@Valid @ModelAttribute ImageUploadRequest request) {
		Map<String, String> result;
		
		if (request.isSingleUpload()) {
			result = imageSaveService.saveImage(
					request.getFile(),
					request.getUploaderId(),
					request.getCategory()
			);
		} else {
			result = imageSaveService.saveImages(
					request.getFiles(),
					request.getUploaderId(),
					request.getCategory()
			);
		}
		
		return ResponseEntity.ok(result);
	}
}
