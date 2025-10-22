package com.teambind.image_server.service;


import com.teambind.image_server.entity.Image;
import com.teambind.image_server.enums.ImageStatus;
import com.teambind.image_server.event.events.ImagesConfirmedEvent;
import com.teambind.image_server.event.publish.ImageChangeEventPublisher;
import com.teambind.image_server.exception.CustomException;
import com.teambind.image_server.exception.ErrorCode;
import com.teambind.image_server.repository.ImageRepository;
import com.teambind.image_server.service.util.StatusChanger;
import com.teambind.image_server.util.validator.ReferenceTypeValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ImageConfirmService {
	private final ImageRepository imageRepository;
	private final ImageChangeEventPublisher eventPublisher;
	private final StatusChanger statusChanger;
	private final ReferenceTypeValidator referenceTypeValidator;
	private final ApplicationEventPublisher applicationEventPublisher;
	
	public void confirmImage(String imageId, String referenceId) {
		if (imageId == null) {
			throw new CustomException(ErrorCode.IMAGE_NOT_FOUND);
		}
		
		if (imageId.isEmpty()) {
			List<Image> images = imageRepository.findAllByReferenceId(referenceId);
			images.forEach(img -> img.setStatus(ImageStatus.DELETED));
			imageRepository.saveAll(images);
			eventPublisher.imageDeletedEvent(referenceId);
			return;
		}
		
		// 이미지 조회 (1번 조회)
		Image newImage = imageRepository.findById(imageId)
				.orElseThrow(() -> new CustomException(ErrorCode.IMAGE_NOT_FOUND));
		
		
		// 다중 이미지 타입이면 confirmImages()로 위임 (조회한 이미지 재사용)
		if (referenceTypeValidator.isMultiImageReferenceType(newImage.getReferenceType().getCode())) {
			List<String> imageIds = List.of(imageId);
			confirmImages(imageIds, referenceId, newImage); // preloadedImage 전달
			return;
		}
		
		// 단일 이미지 타입 처리
		log.info("Confirming single image: imageId={}, referenceId={}", imageId, referenceId);
		List<Image> existingImages = imageRepository.findAllByReferenceId(referenceId);
		
		// 기존 이미지가 없으면 새 이미지만 확정
		if (existingImages.isEmpty()) {
			newImage.setReferenceId(referenceId);
			statusChanger.changeStatus(newImage, ImageStatus.CONFIRMED);
			imageRepository.save(newImage);
			eventPublisher.imageChangeEvent(newImage);
			log.info("New single image confirmed: imageId={}", imageId);
			return;
		}
		
		Image oldImage = existingImages.getFirst();
		
		// 같은 이미지면 아무것도 안함
		if (Objects.equals(oldImage.getId(), imageId)) {
			log.info("Image already confirmed: imageId={}", imageId);
			return;
		}
		
		// 다른 이미지면 교체
		newImage.setReferenceId(referenceId);
		statusChanger.changeStatus(oldImage, ImageStatus.DELETED);
		statusChanger.changeStatus(newImage, ImageStatus.CONFIRMED);
		imageRepository.save(newImage);
		imageRepository.save(oldImage);
		eventPublisher.imageChangeEvent(newImage);
		log.info("Single image replaced: oldImageId={}, newImageId={}", oldImage.getId(), imageId);
	}
	
	
	/**
	 * 다중 이미지 확정 (Public API)
	 */
	@Transactional
	public void confirmImages(List<String> imageIds, String referenceId) {
		confirmImages(imageIds, referenceId, null);
	}
	
	/**
	 * 다중 이미지 확정 (내부 최적화 버전)
	 *
	 * @param imageIds       확정할 이미지 ID 리스트
	 * @param referenceId    참조 ID
	 * @param preloadedImage 이미 조회된 이미지 (있으면 재사용, 없으면 null)
	 */
	@Transactional
	protected void confirmImages(List<String> imageIds, String referenceId, Image preloadedImage) {
		log.info("Confirming multiple images: referenceId={}, imageIdCount={}", referenceId, imageIds != null ? imageIds.size() : 0);
		
		if (imageIds == null) {
			throw new CustomException(ErrorCode.IMAGE_NOT_FOUND);
		}
		
		// 1단계: 기존 이미지들 먼저 조회 (1번 조회)
		List<Image> existingImages = imageRepository.findAllByReferenceId(referenceId);
		
		// 빈 리스트 = 전체 삭제
		if (imageIds.isEmpty()) {
			existingImages.forEach(img -> img.setStatus(ImageStatus.DELETED));
			imageRepository.saveAll(existingImages);
			
			// 도메인 이벤트 발행 (빈 리스트 - 전체 삭제)
			log.info("All images deleted for referenceId: {}", referenceId);
			applicationEventPublisher.publishEvent(
					new ImagesConfirmedEvent(referenceId, List.of())
			);
			return;
		}
		
		// 2단계: 기존 이미지 ID Set 생성
		Map<String, Image> existingImageMap = existingImages.stream()
				.collect(Collectors.toMap(Image::getId, Function.identity()));
		
		// 3단계: 새로운 이미지 ID 추출 (기존에 없는 것만)
		List<String> newImageIds = imageIds.stream()
				.filter(id -> !existingImageMap.containsKey(id))
				.collect(Collectors.toList());
		
		// 4단계: preloadedImage 활용 (있으면 newImageIds에서 제거)
		Map<String, Image> preloadedImageMap = new java.util.HashMap<>();
		if (preloadedImage != null && newImageIds.contains(preloadedImage.getId())) {
			preloadedImageMap.put(preloadedImage.getId(), preloadedImage);
			newImageIds.remove(preloadedImage.getId());
			log.debug("Reusing preloaded image: imageId={}", preloadedImage.getId());
		}
		
		// 5단계: 새 이미지만 추가 조회 (필요한 경우만 1번 조회)
		List<Image> newImages = newImageIds.isEmpty()
				? List.of()
				: imageRepository.findAllByIdIn(newImageIds);
		
		// 6단계: 확정할 이미지 리스트 구성 (기존 + 새로운 + preloaded)
		List<Image> confirmedImages = new ArrayList<>();
		for (String imageId : imageIds) {
			if (existingImageMap.containsKey(imageId)) {
				confirmedImages.add(existingImageMap.get(imageId));
			} else if (preloadedImageMap.containsKey(imageId)) {
				confirmedImages.add(preloadedImageMap.get(imageId));
			} else {
				// newImages에서 찾기
				newImages.stream()
						.filter(img -> img.getId().equals(imageId))
						.findFirst()
						.ifPresent(confirmedImages::add);
			}
		}
		
		// 7단계: referenceType 검증 (첫 번째 이미지로)
		if (!confirmedImages.isEmpty()) {
			String referenceTypeCode = confirmedImages.get(0).getReferenceType().getCode();
			
			// 단일 이미지 타입이면 에러
			if (referenceTypeValidator.isMonoImageReferenceType(referenceTypeCode)) {
				throw new CustomException(ErrorCode.NOT_ALLOWED_MULTIPLE_IMAGES);
			}
		}
		
		// 8단계: 확정 이미지 상태 변경 및 referenceId 설정
		confirmedImages.forEach(img -> {
			img.setReferenceId(referenceId);
			img.setStatus(ImageStatus.CONFIRMED);
		});
		
		// 9단계: 확정 이미지 ID Set 생성
		Map<String, Image> confirmedImageMap = confirmedImages.stream()
				.collect(Collectors.toMap(Image::getId, Function.identity()));
		
		// 10단계: 기존 이미지 중 확정 목록에 없는 것들은 DELETED로 변경
		List<Image> imagesToDelete = existingImages.stream()
				.filter(img -> !confirmedImageMap.containsKey(img.getId()))
				.peek(img -> img.setStatus(ImageStatus.DELETED))
				.collect(Collectors.toList());
		
		// 11단계: 모든 이미지 저장 (확정 + 삭제)
		imageRepository.saveAll(confirmedImages);
		if (!imagesToDelete.isEmpty()) {
			imageRepository.saveAll(imagesToDelete);
		}
		
		// 12단계: 도메인 이벤트 발행 (ImageSequence 생성 및 외부 이벤트 발행을 트리거)
		log.info("Publishing ImagesConfirmedEvent for referenceId: {}, imageCount: {}",
				referenceId, confirmedImages.size());
		applicationEventPublisher.publishEvent(
				new ImagesConfirmedEvent(referenceId, confirmedImages)
		);
	}
}
