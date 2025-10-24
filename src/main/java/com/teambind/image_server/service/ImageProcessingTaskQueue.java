package com.teambind.image_server.service;

import com.teambind.image_server.config.InitialSetup;
import com.teambind.image_server.entity.Image;
import com.teambind.image_server.entity.StorageObject;
import com.teambind.image_server.enums.ImageStatus;
import com.teambind.image_server.exception.CustomException;
import com.teambind.image_server.exception.ErrorCode;
import com.teambind.image_server.repository.ImageRepository;
import com.teambind.image_server.task.ImageProcessingTask;
import com.teambind.image_server.util.convertor.ImageUtil;
import com.teambind.image_server.util.store.LocalImageStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이미지 처리 Task Queue 서비스
 * <p>
 * 백그라운드에서 이미지 변환 및 저장을 비동기로 처리합니다.
 *
 * @author Image Server Team
 * @since 3.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImageProcessingTaskQueue {
	
	private final ImageRepository imageRepository;
	private final LocalImageStorage imageStorage;
	
	/**
	 * 이미지 처리 작업을 비동기로 실행
	 * <p>
	 * 처리 흐름:
	 * 1. WebP 변환 시도
	 * 2. 파일 저장
	 * 3. DB 상태 업데이트 (TEMP → READY)
	 * 4. 실패 시 원본 저장 또는 FAILED 상태로 변경
	 *
	 * @param task 이미지 처리 작업
	 */
	@Async("imageProcessingExecutor")
	@Transactional
	public void submit(ImageProcessingTask task) {
		String imageId = task.getImageId();
		
		try {
			log.info("Starting image processing: imageId={}", imageId);
			
			byte[] convertedBytes;
			String finalStoredPath;
			String convertedFormatCode;
			
			try {
				// 1. WebP 변환 시도
				convertedBytes = ImageUtil.toWebp(task.getFile(), 0.8f);
				finalStoredPath = task.getStoredPath(); // .webp 경로
				convertedFormatCode = "WEBP";
				
				log.debug("Image converted to WebP: imageId={}, size={}bytes", imageId, convertedBytes.length);
				
			} catch (Exception e) {
				// 2. 변환 실패 시 원본 저장 (폴백)
				log.warn("WebP conversion failed, saving as original: imageId={}, error={}", imageId, e.getMessage());
				
				convertedBytes = task.getFile().getBytes();
				String ext = task.getOriginalExtension().toLowerCase();
				finalStoredPath = task.getStoredPath().replace(".webp", "." + ext);
				convertedFormatCode = task.getOriginalExtension();
				
				log.debug("Image saved as original: imageId={}, format={}", imageId, convertedFormatCode);
			}
			
			// 3. 파일 저장
			imageStorage.store(convertedBytes, finalStoredPath);
			
			// 4. DB 상태 업데이트 (TEMP → READY)
			Image image = imageRepository.findById(imageId)
					.orElseThrow(() -> new CustomException(ErrorCode.IMAGE_NOT_FOUND));
			
			// StorageObject 생성
			StorageObject storageObject = StorageObject.builder()
					.image(image)
					.convertedFormat(InitialSetup.EXTENSION_MAP.get(convertedFormatCode))
					.originFormat(InitialSetup.EXTENSION_MAP.get(task.getOriginalExtension()))
					.originSize(task.getFile().getSize())
					.convertedSize((long) convertedBytes.length)
					.storageLocation(finalStoredPath)
					.build();
			
			image.setStatus(ImageStatus.READY);
			image.setStorageObject(storageObject);
			
			imageRepository.save(image);
			
			log.info("Image processing completed successfully: imageId={}, format={}, size={}bytes",
					imageId, convertedFormatCode, convertedBytes.length);
			
		} catch (Exception e) {
			log.error("Image processing failed: imageId={}", imageId, e);
			
			// 5. 완전 실패 시 FAILED 상태로 변경
			handleProcessingFailure(imageId, e);
		}
	}
	
	/**
	 * 이미지 처리 완전 실패 시 처리
	 * <p>
	 * DB 상태를 FAILED로 변경합니다.
	 * 실패한 이미지는 나중에 스케줄러가 정리합니다.
	 *
	 * @param imageId 이미지 ID
	 * @param e       발생한 예외
	 */
	@Transactional
	protected void handleProcessingFailure(String imageId, Exception e) {
		try {
			Image image = imageRepository.findById(imageId)
					.orElseThrow(() -> new CustomException(ErrorCode.IMAGE_NOT_FOUND));
			
			image.setStatus(ImageStatus.FAILED);
			imageRepository.save(image);
			
			log.error("Image status changed to FAILED: imageId={}, reason={}", imageId, e.getMessage());
			
		} catch (Exception dbException) {
			log.error("Failed to update image status to FAILED: imageId={}", imageId, dbException);
		}
	}
}
