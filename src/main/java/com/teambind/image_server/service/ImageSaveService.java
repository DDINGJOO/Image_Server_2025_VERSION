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
import com.teambind.image_server.util.helper.ExtensionParser;
import com.teambind.image_server.util.helper.UrlHelper;
import com.teambind.image_server.util.store.LocalImageStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 이미지 저장 서비스
 * - Controller 레이어에서 @Valid를 통해 모든 검증이 완료됨
 * - 서비스 레이어는 비즈니스 로직에 집중 (변환, 저장, 엔티티 생성)
 * - 최소한의 방어 코드만 유지 (null 체크, IO 예외 처리)
 */

@Service
@RequiredArgsConstructor
@Transactional
public class ImageSaveService {
	private final UrlHelper urlHelper;
	private final ImageRepository imageRepository;
	private final LocalImageStorage imageStorage;
	private final ExtensionParser extensionParser;
	private final ImageProcessingTaskQueue taskQueue;
	
	/**
	 * 단일 이미지 저장
	 * - Controller에서 @Valid를 통해 검증 완료된 데이터가 전달됨
	 * - 파일 확장자, 카테고리 유효성은 이미 검증됨
	 */
	public Map<String, String> saveImage(MultipartFile file, String uploaderId, String category) {
		return saveImage(file, file.getOriginalFilename(), uploaderId, category);
	}
	
	/**
	 * 실제 이미지 저장 로직
	 * - WebP 변환 시도 → 실패 시 원본 저장
	 * - 최소한의 방어 코드만 유지 (null 체크, IO 예외 처리)
	 */
	private Map<String, String> saveImage(MultipartFile file, String fileName, String uploaderId, String category) {
		// 방어 코드: fileName null 체크 (극히 예외적인 경우 대비)
		if (fileName == null || fileName.isBlank()) {
			throw new CustomException(ErrorCode.INVALID_FILE_NAME);
		}
		
		String uuid = UUID.randomUUID().toString();
		String datePath = LocalDateTime.now().toLocalDate().toString().replace("-", "/");

		String originExtUpper = extensionParser.extensionParse(fileName).toUpperCase();
		String originExtLower = originExtUpper.toLowerCase();
		
		String storedPath; // 실제 저장 경로 (성공/폴백에 따라 달라짐)
		byte[] savedBytes; // 실제로 저장된 바이트
		String convertedFormatCode; // 저장된 파일의 포맷 코드
		
		try {
			// 1) WebP 변환 시도
			byte[] webpBytes = ImageUtil.toWebp(file, 0.8f);
			String webpFileName = uuid + ".webp";
			storedPath = category.toUpperCase() + "/" + datePath + "/" + webpFileName;
			imageStorage.store(webpBytes, storedPath);
			
			savedBytes = webpBytes;
			convertedFormatCode = "WEBP";
		} catch (Exception e) {
			// 2) 예외 유형 판단: Rosetta/네이티브 로더 관련 문제라면 원본 그대로 저장 (운영 환경 안정성 우선)
			String msg = e.getMessage() == null ? "" : e.getMessage();
			boolean rosettaLike = msg.contains("rosetta error") || msg.contains("/lib64/ld-linux-x86-64.so.2");
			
			if (rosettaLike) {
				try {
					String fallbackName = uuid + "." + originExtLower;
					storedPath = category.toUpperCase() + "/" + datePath + "/" + fallbackName;
					byte[] originalBytes = file.getBytes();
					imageStorage.store(originalBytes, storedPath);
					
					savedBytes = originalBytes;
					convertedFormatCode = originExtUpper; // WebP가 아닌 원본 포맷으로 저장됨
				} catch (IOException ioEx) {
					System.err.println("[IO_EXCEPTION_FALLBACK] cause=" + ioEx.getClass().getName() + ", message=" + ioEx.getMessage());
					throw new CustomException(ErrorCode.IOException);
				}
			} else {
				// 손상된 이미지 등 일반적 변환 실패는 IMAGE_SAVE_FAILED로 매핑하여 기존 기대 동작 유지
				System.err.println("[IMAGE_CONVERT_FAIL] cause=" + e.getClass().getName() + ", message=" + msg);
				throw new CustomException(ErrorCode.IMAGE_SAVE_FAILED);
			}
		}
		
		// 3) 엔티티 빌드 및 저장
		Image image = Image.builder()
				.id(uuid)
				.createdAt(LocalDateTime.now())
				.isDeleted(false)
				.status(ImageStatus.READY)  // 동기 메서드는 처리 완료 후 즉시 READY
				.referenceType(InitialSetup.ALL_REFERENCE_TYPE_MAP.get(category.toUpperCase()))
				.imageUrl(urlHelper.getUrl(storedPath))
				.uploaderId(uploaderId)
				.build();
		
		StorageObject storageObject = StorageObject.builder()
				.image(image)
				.convertedFormat(InitialSetup.EXTENSION_MAP.get(convertedFormatCode))
				.originFormat(InitialSetup.EXTENSION_MAP.get(originExtUpper))
				.originSize(file.getSize())
				.convertedSize((long) savedBytes.length)
				.storageLocation(storedPath)
				.build();
		
		image.setStorageObject(storageObject);
		imageRepository.save(image);
		return Map.of("id", image.getId(), "fileName", Objects.requireNonNull(file.getOriginalFilename()));
	}
	
	/**
	 * 다중 이미지 저장
	 * - Controller에서 @Valid를 통해 검증 완료된 데이터가 전달됨
	 * - 파일 리스트, 확장자, 카테고리 유효성은 이미 검증됨
	 */
	public Map<String, String> saveImages(List<MultipartFile> files, String uploaderId, String category) {
		Map<String, String> responses = new HashMap<>();
		for (MultipartFile file : files) {
			Map<String, String> result = saveImage(file, uploaderId, category);
			responses.put(result.get("id"), result.get("fileName"));
		}
		return responses;
	}
	
	// ==================== 비동기 처리 메서드 ====================
	
	/**
	 * 단일 이미지 저장 (비동기)
	 * <p>
	 * 처리 흐름:
	 * 1. DB에 메타데이터 먼저 저장 (status: TEMP)
	 * 2. imageId와 예상 URL 즉시 반환
	 * 3. 백그라운드에서 변환/저장 처리 (Task Queue)
	 *
	 * @param file       업로드된 파일
	 * @param uploaderId 업로더 ID
	 * @param category   카테고리
	 * @return imageId, imageUrl, status
	 */
	@Transactional
	public Map<String, String> saveImageAsync(MultipartFile file, String uploaderId, String category) {
		String fileName = file.getOriginalFilename();
		if (fileName == null || fileName.isBlank()) {
			throw new CustomException(ErrorCode.INVALID_FILE_NAME);
		}
		
		// 1. 메타데이터 미리 생성
		String uuid = UUID.randomUUID().toString();
		String datePath = LocalDateTime.now().toLocalDate().toString().replace("-", "/");
		String originExtUpper = extensionParser.extensionParse(fileName).toUpperCase();
		String webpFileName = uuid + ".webp";
		String storedPath = category.toUpperCase() + "/" + datePath + "/" + webpFileName;
		String imageUrl = urlHelper.getUrl(storedPath);
		
		// 2. DB에 먼저 저장 (status: TEMP - 변환 대기 중)
		Image image = Image.builder()
				.id(uuid)
				.uploaderId(uploaderId)
				.referenceType(InitialSetup.ALL_REFERENCE_TYPE_MAP.get(category))
				.status(ImageStatus.TEMP)  // ⭐ 변환 대기 중
				.imageUrl(imageUrl)
				.build();
		
		imageRepository.save(image);
		
		// 3. 백그라운드 Task Queue에 등록
		ImageProcessingTask task = new ImageProcessingTask(
				uuid,
				file,
				storedPath,
				originExtUpper
		);
		taskQueue.submit(task);
		
		// 4. 즉시 응답 (클라이언트는 폼 작성 계속)
		return Map.of(
				"id", uuid,
				"imageUrl", imageUrl,
				"status", "PROCESSING"
		);
	}
	
	/**
	 * 다중 이미지 저장 (비동기)
	 *
	 * @param files      업로드된 파일 리스트
	 * @param uploaderId 업로더 ID
	 * @param category   카테고리
	 * @return imageId -> status 맵
	 */
	@Transactional
	public Map<String, String> saveImagesAsync(List<MultipartFile> files, String uploaderId, String category) {
		Map<String, String> responses = new HashMap<>();
		for (MultipartFile file : files) {
			Map<String, String> result = saveImageAsync(file, uploaderId, category);
			responses.put(result.get("id"), result.get("status"));
		}
		return responses;
	}


}
