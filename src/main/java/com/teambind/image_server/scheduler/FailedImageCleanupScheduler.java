package com.teambind.image_server.scheduler;

import com.teambind.image_server.entity.Image;
import com.teambind.image_server.enums.ImageStatus;
import com.teambind.image_server.repository.ImageRepository;
import com.teambind.image_server.util.store.LocalImageStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 실패한 이미지 정리 스케줄러
 * <p>
 * 비동기 처리 중 실패한 이미지(FAILED 상태)를 주기적으로 정리합니다.
 *
 * @author Image Server Team
 * @since 3.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FailedImageCleanupScheduler {
	
	private final ImageRepository imageRepository;
	private final LocalImageStorage imageStorage;
	
	/**
	 * 매일 새벽 3시에 실패한 이미지 정리
	 * <p>
	 * - 생성된 지 24시간 이상 지난 FAILED 이미지만 삭제
	 * - 파일 시스템에서 파일 삭제
	 * - DB에서 레코드 삭제
	 */
	@Scheduled(cron = "0 0 3 * * *")  // 매일 새벽 3시
	@Transactional
	public void cleanupFailedImages() {
		LocalDateTime threshold = LocalDateTime.now().minusDays(1);
		
		List<Image> failedImages = imageRepository.findByStatusAndCreatedAtBefore(
				ImageStatus.FAILED,
				threshold
		);
		
		if (failedImages.isEmpty()) {
			log.info("No failed images to cleanup");
			return;
		}
		
		log.info("Starting cleanup of {} failed images", failedImages.size());
		
		int successCount = 0;
		int failCount = 0;
		
		for (Image image : failedImages) {
			try {
				// 파일 삭제 (있다면)
				if (image.getStorageObject() != null && image.getStorageObject().getStorageLocation() != null) {
					try {
						imageStorage.delete(image.getStorageObject().getStorageLocation());
						log.debug("Deleted file: {}", image.getStorageObject().getStorageLocation());
					} catch (Exception e) {
						log.warn("Failed to delete file: {}, error: {}",
								image.getStorageObject().getStorageLocation(), e.getMessage());
					}
				}
				
				// DB에서 삭제
				imageRepository.delete(image);
				successCount++;
				
				log.debug("Cleaned up failed image: imageId={}", image.getId());
				
			} catch (Exception e) {
				failCount++;
				log.error("Failed to cleanup image: imageId={}", image.getId(), e);
			}
		}
		
		log.info("Cleanup completed: success={}, failed={}, total={}",
				successCount, failCount, failedImages.size());
	}
	
	/**
	 * 매일 오전 10시에 오래된 TEMP 이미지 정리
	 * <p>
	 * - 24시간 이상 TEMP 상태로 남아있는 이미지 삭제
	 * - 변환이 시작되었지만 완료되지 않은 이미지
	 */
	@Scheduled(cron = "0 0 10 * * *")  // 매일 오전 10시
	@Transactional
	public void cleanupStaleTempImages() {
		LocalDateTime threshold = LocalDateTime.now().minusDays(1);
		
		List<Image> staleImages = imageRepository.findByStatusAndCreatedAtBefore(
				ImageStatus.TEMP,
				threshold
		);
		
		if (staleImages.isEmpty()) {
			log.info("No stale TEMP images to cleanup");
			return;
		}
		
		log.info("Starting cleanup of {} stale TEMP images", staleImages.size());
		
		for (Image image : staleImages) {
			try {
				// 파일 삭제 (있다면)
				if (image.getStorageObject() != null && image.getStorageObject().getStorageLocation() != null) {
					try {
						imageStorage.delete(image.getStorageObject().getStorageLocation());
					} catch (Exception e) {
						log.warn("Failed to delete file: {}", image.getStorageObject().getStorageLocation());
					}
				}
				
				// DB에서 삭제
				imageRepository.delete(image);
				
				log.debug("Cleaned up stale TEMP image: imageId={}", image.getId());
				
			} catch (Exception e) {
				log.error("Failed to cleanup stale image: imageId={}", image.getId(), e);
			}
		}
		
		log.info("Stale TEMP cleanup completed: total={}", staleImages.size());
	}
}
