package com.teambind.image_server.controller;


import com.teambind.image_server.dto.request.ImageBatchConfirmRequest;
import com.teambind.image_server.service.ImageConfirmService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 이미지 확정 컨트롤러
 * - 단일 이미지 확정: imageId를 referenceId에 연결
 * - 다중 이미지 확정: imageIds 리스트를 referenceId에 연결
 * - 모든 검증은 Controller 레이어에서 @Valid/@Validated를 통해 수행
 */
@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
@Validated
public class ImageConfirmController {
	private final ImageConfirmService imageConfirmService;
	
	/**
	 * 단일 이미지 확정
	 * - imageId가 빈 문자열("")이면 해당 referenceId의 모든 이미지 삭제
	 * - imageId가 유효하면 해당 이미지를 referenceId에 연결
	 *
	 * @param imageId     확정할 이미지 ID (빈 문자열 허용)
	 * @param referenceId 참조 ID
	 * @return 200 OK
	 */
	@PostMapping("/confirm/{referenceId}")
	public ResponseEntity<Void> confirmImage(
			@RequestParam @NotBlank(message = "이미지 ID는 필수입니다") String imageId,
			@PathVariable(name = "referenceId") @NotBlank(message = "참조 ID는 필수입니다") String referenceId) {
		imageConfirmService.confirmImage(imageId, referenceId);
		return ResponseEntity.ok().build();
	}
	
	/**
	 * 다중 이미지 확정 (배치)
	 * - imageIds가 빈 리스트[]이면 해당 referenceId의 모든 이미지 삭제
	 * - imageIds가 유효하면 해당 이미지들을 referenceId에 연결
	 *
	 * @param request 배치 확정 요청 (imageIds, referenceId)
	 * @return 200 OK
	 */
	@PostMapping("/confirm")
	public ResponseEntity<Void> confirmImages(@Valid @RequestBody ImageBatchConfirmRequest request) {
		imageConfirmService.confirmImages(request.getImageIds(), request.getReferenceId());
		return ResponseEntity.ok().build();
	}
}
