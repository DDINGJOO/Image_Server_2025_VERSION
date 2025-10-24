package com.teambind.image_server.performance;

import com.teambind.image_server.config.InitialSetup;
import com.teambind.image_server.entity.Extension;
import com.teambind.image_server.entity.ReferenceType;
import com.teambind.image_server.fixture.TestFixtureFactory;
import com.teambind.image_server.repository.ExtensionRepository;
import com.teambind.image_server.repository.ReferenceTypeRepository;
import com.teambind.image_server.service.ImageSaveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 이미지 처리 성능 테스트
 * <p>
 * 동기 방식 vs 비동기 방식 성능 비교
 *
 * @author Image Server Team
 * @since 3.0
 */
@SpringBootTest
@ActiveProfiles("test")
class ImageProcessingPerformanceTest {
	
	private static final Logger log = LoggerFactory.getLogger(ImageProcessingPerformanceTest.class);
	// 테스트용 이미지 파일 경로 (실제 경로로 변경 필요)
	private static final String TEST_IMAGE_PATH = "src/test/resources/test-image.jpg";
	@Autowired
	private ImageSaveService imageSaveService;
	@Autowired
	private ReferenceTypeRepository referenceTypeRepository;
	@Autowired
	private ExtensionRepository extensionRepository;
	
	@BeforeEach
	void setUp() {
		// InitialSetup 초기화
		InitialSetup.ALL_REFERENCE_TYPE_MAP.clear();
		InitialSetup.EXTENSION_MAP.clear();
		
		// 데이터베이스에 Extension 저장
		Extension jpgExt = TestFixtureFactory.createJpgExtension();
		Extension webpExt = TestFixtureFactory.createWebpExtension();
		extensionRepository.save(jpgExt);
		extensionRepository.save(webpExt);
		InitialSetup.EXTENSION_MAP.put("JPG", jpgExt);
		InitialSetup.EXTENSION_MAP.put("WEBP", webpExt);
		
		// 데이터베이스에 PRODUCT 참조 타입 저장
		ReferenceType productType = TestFixtureFactory.createMultiReferenceType("PRODUCT");
		referenceTypeRepository.save(productType);
		InitialSetup.ALL_REFERENCE_TYPE_MAP.put("PRODUCT", productType);
	}
	
	/**
	 * 테스트용 MockMultipartFile 생성
	 */
	private MultipartFile createTestImage() throws IOException {
		// 실제 테스트 이미지 파일이 없는 경우 더미 데이터 생성
		byte[] content;
		String filename = "test-image.jpg";
		
		File testFile = new File(TEST_IMAGE_PATH);
		if (testFile.exists()) {
			content = Files.readAllBytes(Paths.get(TEST_IMAGE_PATH));
		} else {
			// BufferedImage를 사용해 유효한 10x10 픽셀 JPEG 이미지 생성
			content = createValidJpegImage();
		}
		
		return new MockMultipartFile(
				"file",
				filename,
				"image/jpeg",
				content
		);
	}
	
	/**
	 * BufferedImage를 사용해 유효한 JPEG 이미지 생성
	 */
	private byte[] createValidJpegImage() throws IOException {
		// 10x10 픽셀의 간단한 이미지 생성
		java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(10, 10, java.awt.image.BufferedImage.TYPE_INT_RGB);
		
		// 이미지를 파란색으로 채우기
		java.awt.Graphics2D graphics = image.createGraphics();
		graphics.setColor(java.awt.Color.BLUE);
		graphics.fillRect(0, 0, 10, 10);
		graphics.dispose();
		
		// JPEG로 변환
		java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
		javax.imageio.ImageIO.write(image, "jpg", baos);
		
		return baos.toByteArray();
	}
	
	@Test
	@DisplayName("동기 방식: 100개 이미지 순차 처리 성능 테스트")
	void testSyncImageProcessing() throws Exception {
		int imageCount = 100;
		log.info("=".repeat(80));
		log.info("동기 방식 성능 테스트 시작: {}개 이미지", imageCount);
		log.info("=".repeat(80));
		
		long startTime = System.currentTimeMillis();
		
		for (int i = 0; i < imageCount; i++) {
			MultipartFile file = createTestImage();
			try {
				Map<String, String> result = imageSaveService.saveImage(
						file,
						"test-uploader",
						"PRODUCT"
				);
				log.debug("동기 처리 완료 [{}/{}]: imageId={}", i + 1, imageCount, result.get("id"));
			} catch (Exception e) {
				log.error("동기 처리 실패 [{}]: {}", i + 1, e.getMessage());
			}
		}
		
		long endTime = System.currentTimeMillis();
		long totalTime = endTime - startTime;
		double avgTime = (double) totalTime / imageCount;
		
		log.info("=".repeat(80));
		log.info("동기 방식 성능 테스트 결과:");
		log.info("  - 총 처리 시간: {}ms ({}초)", totalTime, totalTime / 1000.0);
		log.info("  - 평균 처리 시간: {}ms/이미지", String.format("%.2f", avgTime));
		log.info("  - 처리량: {}개/초", String.format("%.2f", 1000.0 / avgTime));
		log.info("=".repeat(80));
	}
	
	@Test
	@DisplayName("비동기 방식: 100개 이미지 처리 성능 테스트")
	void testAsyncImageProcessing() throws Exception {
		int imageCount = 100;
		log.info("=".repeat(80));
		log.info("비동기 방식 성능 테스트 시작: {}개 이미지", imageCount);
		log.info("=".repeat(80));
		
		long startTime = System.currentTimeMillis();
		
		List<String> imageIds = new ArrayList<>();
		for (int i = 0; i < imageCount; i++) {
			MultipartFile file = createTestImage();
			try {
				Map<String, String> result = imageSaveService.saveImageAsync(
						file,
						"test-uploader",
						"PRODUCT"
				);
				imageIds.add(result.get("id"));
				log.debug("비동기 제출 완료 [{}/{}]: imageId={}, status={}",
						i + 1, imageCount, result.get("id"), result.get("status"));
			} catch (Exception e) {
				log.error("비동기 제출 실패 [{}]: {}", i + 1, e.getMessage());
			}
		}
		
		long submitEndTime = System.currentTimeMillis();
		long submitTime = submitEndTime - startTime;
		
		log.info("모든 작업 제출 완료: {}ms", submitTime);
		log.info("백그라운드 처리 대기 중...");
		
		// 백그라운드 처리 완료 대기 (최대 60초)
		Thread.sleep(60000);
		
		long endTime = System.currentTimeMillis();
		long totalTime = endTime - startTime;
		double avgSubmitTime = (double) submitTime / imageCount;
		
		log.info("=".repeat(80));
		log.info("비동기 방식 성능 테스트 결과:");
		log.info("  - 작업 제출 시간: {}ms ({}초)", submitTime, submitTime / 1000.0);
		log.info("  - 총 경과 시간: {}ms ({}초)", totalTime, totalTime / 1000.0);
		log.info("  - 평균 제출 시간: {}ms/이미지", String.format("%.2f", avgSubmitTime));
		log.info("  - 제출 처리량: {}개/초", String.format("%.2f", 1000.0 / avgSubmitTime));
		log.info("=".repeat(80));
	}
	
	@Test
	@DisplayName("동시성 테스트: 동기 방식 50개 동시 요청")
	void testSyncConcurrentProcessing() throws Exception {
		int threadCount = 50;
		ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);
		AtomicInteger successCount = new AtomicInteger(0);
		AtomicInteger failCount = new AtomicInteger(0);
		
		log.info("=".repeat(80));
		log.info("동기 방식 동시성 테스트 시작: {} 스레드", threadCount);
		log.info("=".repeat(80));
		
		long startTime = System.currentTimeMillis();
		
		for (int i = 0; i < threadCount; i++) {
			final int index = i;
			executorService.submit(() -> {
				try {
					MultipartFile file = createTestImage();
					Map<String, String> result = imageSaveService.saveImage(
							file,
							"test-uploader-" + index,
							"PRODUCT"
					);
					successCount.incrementAndGet();
					log.debug("동시 처리 성공 [{}]: imageId={}", index, result.get("id"));
				} catch (Exception e) {
					failCount.incrementAndGet();
					log.error("동시 처리 실패 [{}]: {}", index, e.getMessage());
				} finally {
					latch.countDown();
				}
			});
		}
		
		latch.await(120, TimeUnit.SECONDS);
		executorService.shutdown();
		
		long endTime = System.currentTimeMillis();
		long totalTime = endTime - startTime;
		
		log.info("=".repeat(80));
		log.info("동기 방식 동시성 테스트 결과:");
		log.info("  - 총 처리 시간: {}ms ({}초)", totalTime, totalTime / 1000.0);
		log.info("  - 성공: {}개", successCount.get());
		log.info("  - 실패: {}개", failCount.get());
		log.info("  - 처리량: {}개/초", String.format("%.2f", (double) successCount.get() / (totalTime / 1000.0)));
		log.info("=".repeat(80));
	}
	
	@Test
	@DisplayName("동시성 테스트: 비동기 방식 50개 동시 요청")
	void testAsyncConcurrentProcessing() throws Exception {
		int threadCount = 50;
		ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);
		AtomicInteger successCount = new AtomicInteger(0);
		AtomicInteger failCount = new AtomicInteger(0);
		
		log.info("=".repeat(80));
		log.info("비동기 방식 동시성 테스트 시작: {} 스레드", threadCount);
		log.info("=".repeat(80));
		
		long startTime = System.currentTimeMillis();
		
		for (int i = 0; i < threadCount; i++) {
			final int index = i;
			executorService.submit(() -> {
				try {
					MultipartFile file = createTestImage();
					Map<String, String> result = imageSaveService.saveImageAsync(
							file,
							"test-uploader-" + index,
							"PRODUCT"
					);
					successCount.incrementAndGet();
					log.debug("동시 제출 성공 [{}]: imageId={}, status={}", index, result.get("id"), result.get("status"));
				} catch (Exception e) {
					failCount.incrementAndGet();
					log.error("동시 제출 실패 [{}]: {}", index, e.getMessage());
				} finally {
					latch.countDown();
				}
			});
		}
		
		latch.await(120, TimeUnit.SECONDS);
		executorService.shutdown();
		
		long submitEndTime = System.currentTimeMillis();
		long submitTime = submitEndTime - startTime;
		
		log.info("모든 작업 제출 완료: {}ms", submitTime);
		log.info("백그라운드 처리 대기 중...");
		
		// 백그라운드 처리 완료 대기
		Thread.sleep(60000);
		
		long endTime = System.currentTimeMillis();
		long totalTime = endTime - startTime;
		
		log.info("=".repeat(80));
		log.info("비동기 방식 동시성 테스트 결과:");
		log.info("  - 작업 제출 시간: {}ms ({}초)", submitTime, submitTime / 1000.0);
		log.info("  - 총 경과 시간: {}ms ({}초)", totalTime, totalTime / 1000.0);
		log.info("  - 성공: {}개", successCount.get());
		log.info("  - 실패: {}개", failCount.get());
		log.info("  - 제출 처리량: {}개/초", String.format("%.2f", (double) successCount.get() / (submitTime / 1000.0)));
		log.info("=".repeat(80));
	}
	
	@Test
	@DisplayName("부하 테스트: 동기 방식 200개 이미지")
	void testSyncLoadTest() throws Exception {
		int imageCount = 200;
		log.info("=".repeat(80));
		log.info("동기 방식 부하 테스트: {}개 이미지", imageCount);
		log.info("=".repeat(80));
		
		long startTime = System.currentTimeMillis();
		int successCount = 0;
		int failCount = 0;
		
		for (int i = 0; i < imageCount; i++) {
			try {
				MultipartFile file = createTestImage();
				imageSaveService.saveImage(file, "load-test-user", "PRODUCT");
				successCount++;
			} catch (Exception e) {
				failCount++;
				log.error("부하 테스트 실패 [{}]: {}", i, e.getMessage());
			}
			
			if ((i + 1) % 50 == 0) {
				log.info("진행 상황: {}/{}개 완료", i + 1, imageCount);
			}
		}
		
		long endTime = System.currentTimeMillis();
		long totalTime = endTime - startTime;
		
		log.info("=".repeat(80));
		log.info("동기 방식 부하 테스트 결과:");
		log.info("  - 총 처리 시간: {}ms ({}초)", totalTime, totalTime / 1000.0);
		log.info("  - 성공: {}개", successCount);
		log.info("  - 실패: {}개", failCount);
		log.info("  - 성공률: {}%", String.format("%.2f", (double) successCount / imageCount * 100));
		log.info("  - 처리량: {}개/초", String.format("%.2f", (double) successCount / (totalTime / 1000.0)));
		log.info("=".repeat(80));
	}
	
	@Test
	@DisplayName("부하 테스트: 비동기 방식 200개 이미지")
	void testAsyncLoadTest() throws Exception {
		int imageCount = 200;
		log.info("=".repeat(80));
		log.info("비동기 방식 부하 테스트: {}개 이미지", imageCount);
		log.info("=".repeat(80));
		
		long startTime = System.currentTimeMillis();
		int successCount = 0;
		int failCount = 0;
		
		for (int i = 0; i < imageCount; i++) {
			try {
				MultipartFile file = createTestImage();
				imageSaveService.saveImageAsync(file, "load-test-user", "PRODUCT");
				successCount++;
			} catch (Exception e) {
				failCount++;
				log.error("부하 테스트 제출 실패 [{}]: {}", i, e.getMessage());
			}
			
			if ((i + 1) % 50 == 0) {
				log.info("제출 진행: {}/{}개 완료", i + 1, imageCount);
			}
		}
		
		long submitEndTime = System.currentTimeMillis();
		long submitTime = submitEndTime - startTime;
		
		log.info("모든 작업 제출 완료: {}ms", submitTime);
		log.info("백그라운드 처리 대기 중...");
		
		Thread.sleep(120000); // 2분 대기
		
		long endTime = System.currentTimeMillis();
		long totalTime = endTime - startTime;
		
		log.info("=".repeat(80));
		log.info("비동기 방식 부하 테스트 결과:");
		log.info("  - 작업 제출 시간: {}ms ({}초)", submitTime, submitTime / 1000.0);
		log.info("  - 총 경과 시간: {}ms ({}초)", totalTime, totalTime / 1000.0);
		log.info("  - 성공: {}개", successCount);
		log.info("  - 실패: {}개", failCount);
		log.info("  - 성공률: {}%", String.format("%.2f", (double) successCount / imageCount * 100));
		log.info("  - 제출 처리량: {}개/초", String.format("%.2f", (double) successCount / (submitTime / 1000.0)));
		log.info("=".repeat(80));
	}
}
