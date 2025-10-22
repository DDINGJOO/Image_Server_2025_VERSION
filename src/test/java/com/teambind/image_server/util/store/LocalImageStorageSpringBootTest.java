package com.teambind.image_server.util.store;

import com.teambind.image_server.config.DataInitializer;
import com.teambind.image_server.exception.CustomException;
import com.teambind.image_server.service.ImageConfirmService;
import com.teambind.image_server.service.ImageSaveService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = LocalImageStorageSpringBootTest.TestApp.class)
@ActiveProfiles("test")
class LocalImageStorageSpringBootTest {
	
	@TempDir
	static Path tempDir;
	@Autowired
	private LocalImageStorage storage;
	
	@DynamicPropertySource
	static void registerProps(DynamicPropertyRegistry registry) {
		// 테스트용 업로드 디렉터리를 동적으로 주입한다
		registry.add("images.upload.dir", () -> tempDir.toAbsolutePath().toString());
	}
	
	@BeforeEach
	void setUp() throws IOException {
		// 각 테스트 시작 전 임시 디렉터리를 깨끗한 상태로 유지
		if (Files.exists(tempDir)) {
			Files.walk(tempDir)
					.filter(p -> !p.equals(tempDir))
					.sorted(Comparator.reverseOrder())
					.forEach(p -> {
						try {
							Files.deleteIfExists(p);
						} catch (IOException ignored) {
						}
					});
		}
		Files.createDirectories(tempDir);
	}
	
	@AfterEach
	void tearDown() throws IOException {
		// 각 테스트 종료 후 생성된 파일 정리
		if (Files.exists(tempDir)) {
			Files.walk(tempDir)
					.filter(p -> !p.equals(tempDir))
					.sorted(Comparator.reverseOrder())
					.forEach(p -> {
						try {
							Files.deleteIfExists(p);
						} catch (IOException ignored) {
						}
					});
		}
	}
	
	@Test
	@DisplayName("정상 경로에 이미지를 저장하고 삭제에 성공한다")
	void storeAndDelete_success() throws Exception {
		// given: 더미 이미지 바이트와 상대 경로
		byte[] bytes = new byte[]{1, 2, 3, 4, 5};
		String relativePath = "user/%s/test.bin".formatted(UUID.randomUUID());
		
		// when: 저장 수행
		String savedPath = storage.store(bytes, relativePath);
		
		// then: 파일이 물리적으로 존재하는지 확인
		Path expected = tempDir.resolve(relativePath).normalize();
		assertThat(savedPath).isEqualTo(relativePath);
		assertThat(Files.exists(expected)).isTrue();
		
		// and when: 삭제 수행
		boolean deleted = storage.delete(relativePath);
		
		// then: 삭제 성공 및 파일 존재하지 않음
		assertThat(deleted).isTrue();
		assertThat(Files.exists(expected)).isFalse();
	}
	
	@Test
	@DisplayName("상대 경로에 '..' 이 포함되면 저장에 실패한다")
	void store_rejects_path_traversal() {
		// given
		byte[] bytes = new byte[]{1, 2, 3};
		String badPath = "../outside.bin";
		
		// when & then
		assertThrows(CustomException.class, () -> storage.store(bytes, badPath));
	}
	
	@Test
	@DisplayName("삭제: 잘못된 입력(널/빈/경로조작)일 경우 false를 반환한다")
	void delete_invalid_inputs() {
		assertThat(storage.delete(null)).isFalse();
		assertThat(storage.delete(" ")).isFalse();
		assertThat(storage.delete("../oops.bin")).isFalse();
	}
	
	// 최소 구성의 스프링 부트 설정: 테스트에 불필요한 빈(DataInitializer, Service들)을 제외하고 필요한 컴포넌트만 스캔한다
	@SpringBootConfiguration
	@ComponentScan(basePackages = "com.teambind.image_server",
			excludeFilters = {
					@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = DataInitializer.class),
					@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ImageConfirmService.class),
					@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ImageSaveService.class)
			})
	static class TestApp {
	}
}
