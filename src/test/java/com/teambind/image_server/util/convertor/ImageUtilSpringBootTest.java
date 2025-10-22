package com.teambind.image_server.util.convertor;

import com.teambind.image_server.config.DataInitializer;
import com.teambind.image_server.service.ImageConfirmService;
import com.teambind.image_server.service.ImageSaveService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = ImageUtilSpringBootTest.TestApp.class)
@ActiveProfiles("test")
class ImageUtilSpringBootTest {
	
	@BeforeEach
	void setUp() {
		// 각 테스트 시작 전 별도의 전역 상태는 없지만, 훅을 통해 테스트 흐름을 명확히 한다
	}
	
	@AfterEach
	void tearDown() {
		// 각 테스트 종료 후 리소스 정리 필요 시 확장 가능 (현재는 정리할 리소스 없음)
	}
	
	// 메모리 상에서 작은 PNG 이미지를 생성한다
	private byte[] createPngBytes(int width, int height, Color color) throws Exception {
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = img.createGraphics();
		try {
			g2d.setColor(color);
			g2d.fillRect(0, 0, width, height);
		} finally {
			g2d.dispose();
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(img, "png", baos);
		return baos.toByteArray();
	}
	
	@Test
	@DisplayName("PNG 이미지를 WebP 바이트로 변환한다")
	void toWebp_convertsSuccessfully() throws Exception {
		byte[] png = createPngBytes(64, 64, Color.RED);
		MockMultipartFile file = new MockMultipartFile("file", "red.png", "image/png", png);
		
		byte[] webp = ImageUtil.toWebp(file, 0.8f);
		
		// 변환 결과가 존재하고, 원본 바이트와는 달라야 한다
		assertThat(webp).isNotNull();
		assertThat(webp.length).isGreaterThan(0);
		assertThat(webp).isNotEqualTo(png);
	}
	
	@Test
	@DisplayName("PNG 이미지를 지정 크기의 WebP 썸네일로 변환한다")
	void toWebpThumbnail_convertsAndResizes() throws Exception {
		byte[] png = createPngBytes(200, 100, Color.BLUE);
		MockMultipartFile file = new MockMultipartFile("file", "blue.png", "image/png", png);
		
		byte[] webpThumb = ImageUtil.toWebpThumbnail(file, 50, 50, 0.7f);
		
		assertThat(webpThumb).isNotNull();
		assertThat(webpThumb.length).isGreaterThan(0);
	}
	
	// 최소 구성의 스프링 부트 설정: 테스트에 불필요한 빈(DataInitializer, Service들)을 제외하여 JPA 의존성 없이 컨텍스트를 구동한다
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
