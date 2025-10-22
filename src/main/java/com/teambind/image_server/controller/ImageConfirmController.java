package com.teambind.image_server.controller;


import com.teambind.image_server.event.publish.ImageChangeEventPublisher;
import com.teambind.image_server.service.ImageConfirmService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageConfirmController {
	private final ImageConfirmService imageConfirmService;
	private final ImageChangeEventPublisher eventPublisher;
//
//	@PatchMapping("/{referenceId}/confirm")
//	public void confirmImage(@RequestParam(name = "imageId", required = false) List<String> imageIds,
//	                         @PathVariable(name = "referenceId") String referenceId) {
//
//		if (imageIds == null || imageIds.isEmpty()) {
//			return; // 또는 throw new CustomException(ErrorCode.INVALID_REQUEST) 등으로 처리
//		}
//
//		// 방어적 정리: 공백 제거 및 빈값 필터링
//		List<String> cleaned = imageIds.stream()
//				.filter(java.util.Objects::nonNull)
//				.map(String::trim)
//				.filter(s -> !s.isEmpty())
//				.toList();
//
//		if (cleaned.isEmpty()) return;
//
//		// optional: 최대 개수 제한
//		if (cleaned.size() > 100) { // 예: 100개 초과 시 에러
//			throw new IllegalArgumentException("Too many imageIds");
//		}
//
//		if () {
//			Image image = imageConfirmService.confirmImage(cleaned.getFirst(), referenceId);
//			eventPublisher.imageChangeEvent(image);
//		} else {
//			//TODO : IMPL
////			eventPublisher.imagesChangeEvent(imageConfirmService.confirmImages(cleaned, referenceId));
//		}
//	}

}
