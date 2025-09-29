package com.teambind.image_server.service;

import com.teambind.image_server.entity.Extension;
import com.teambind.image_server.entity.Image;
import com.teambind.image_server.entity.ReferenceType;
import com.teambind.image_server.exception.CustomException;
import com.teambind.image_server.exception.ErrorCode;
import com.teambind.image_server.repository.ExtensionRepository;
import com.teambind.image_server.repository.ImageRepository;
import com.teambind.image_server.repository.ReferenceTypeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 서비스 통합 테스트: ImageSaveService
 * - 정상 저장 및 다양한 실패 케이스를 검증한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ImageSaveServiceSpringBootTest {

    @TempDir
    static Path tempDir;
    @Autowired
    private ImageSaveService saveService;
    @Autowired
    private ImageRepository imageRepository;
    @Autowired
    private ReferenceTypeRepository referenceTypeRepository;
    @Autowired
    private ExtensionRepository extensionRepository;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("images.upload.dir", () -> tempDir.toAbsolutePath().toString());
        r.add("images.base-url", () -> "http://localhost/images/");
    }

    @BeforeEach
    void beforeEach() {
        imageRepository.deleteAll();
    }

    @AfterEach
    void afterEach() {
        imageRepository.deleteAll();
    }

    private byte[] createPngBytes(int w, int h, Color color) throws Exception {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setColor(color);
            g.fillRect(0, 0, w, h);
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }

    @Test
    @DisplayName("정상: PNG 이미지를 PROFILE 카테고리로 저장한다")
    void saveImage_success() throws Exception {
        // given
        byte[] png = createPngBytes(40, 40, Color.GREEN);
        MockMultipartFile file = new MockMultipartFile("file", "ok.png", "image/png", png);

        // sanity check: 코드 테이블이 로드되었는지 확인
        assertThat(extensionRepository.findAll()).extracting(Extension::getCode).contains("WEBP", "PNG", "JPG");
        ReferenceType profile = referenceTypeRepository.findAll().stream().filter(r -> r.getCode().equals("PROFILE")).findFirst().orElseThrow();
        assertThat(profile).isNotNull();

        // when
        java.util.Map<String, String> result = saveService.saveImage(file, "user-100", "PROFILE");

        // then: 응답 포맷 검증 (현재 서비스는 id(UUID), fileName을 반환)
        assertThat(result).containsKeys("id", "fileName");
        assertThat(result.get("fileName")).isEqualTo("ok.png");
        assertThat(result.get("id")).isNotBlank();

        // and: DB 적재 - id로 조회하여 검증
        Image found = imageRepository.findById(result.get("id")).orElseThrow();
        assertThat(found.getImageUrl()).isNotBlank();
        assertThat(found.getStorageObject()).isNotNull();
        assertThat(found.getStorageObject().getConvertedSize()).isNotNull();
        assertThat(found.getReferenceType().getCode()).isEqualTo("PROFILE");

        // and: 파일이 실제로 저장됨
        String relativePath = found.getStorageObject().getStorageLocation();
        Path expected = tempDir.resolve(relativePath).normalize();
        assertThat(Files.exists(expected)).isTrue();
    }

    @Test
    @DisplayName("실패: 지원하지 않는 확장자면 FILE_EXTENSION_NOT_FOUND")
    void saveImage_invalidExtension_throws() {
        MockMultipartFile file = new MockMultipartFile("file", "not-allowed.txt", "text/plain", new byte[]{1, 2, 3});
        CustomException ex = assertThrows(CustomException.class, () -> saveService.saveImage(file, "u-1", "PROFILE"));
        assertThat(ex.getStatus()).isEqualTo(ErrorCode.FILE_EXTENSION_NOT_FOUND.getStatus());
    }

    @Test
    @DisplayName("실패: 존재하지 않는 레퍼런스면 REFERENCE_TYPE_NOT_FOUND")
    void saveImage_invalidReference_throws() throws Exception {
        byte[] png = createPngBytes(30, 30, Color.BLACK);
        MockMultipartFile file = new MockMultipartFile("file", "ok.png", "image/png", png);
        CustomException ex = assertThrows(CustomException.class, () -> saveService.saveImage(file, "u-1", "unknown"));
        assertThat(ex.getStatus()).isEqualTo(ErrorCode.REFERENCE_TYPE_NOT_FOUND.getStatus());
    }

    @Test
    @DisplayName("실패: 손상된 이미지 바이트는 IMAGE_SAVE_FAILED")
    void saveImage_corruptedImage_throws() {
        // Scrimage가 해석할 수 없는 바이트 전달
        byte[] bad = new byte[]{0, 1, 2, 3, 4, 5};
        MockMultipartFile file = new MockMultipartFile("file", "bad.png", "image/png", bad);
        CustomException ex = assertThrows(CustomException.class, () -> saveService.saveImage(file, "u-1", "PROFILE"));
        assertThat(ex.getStatus()).isEqualTo(ErrorCode.IMAGE_SAVE_FAILED.getStatus());
    }

    @Test
    @DisplayName("로제타류 오류 발생 시 원본 포맷으로 폴백 저장한다")
    void saveImage_rosettaFallback_storesOriginal() throws Exception {
        // given
        byte[] png = createPngBytes(20, 20, Color.MAGENTA);
        MockMultipartFile file = new MockMultipartFile("file", "orig.png", "image/png", png);

        // ImageUtil.toWebp 가 로제타 관련 에러를 던지도록 정적 모킹
        try (org.mockito.MockedStatic<com.teambind.image_server.util.convertor.ImageUtil> mocked =
                     org.mockito.Mockito.mockStatic(com.teambind.image_server.util.convertor.ImageUtil.class)) {
            mocked.when(() -> com.teambind.image_server.util.convertor.ImageUtil.toWebp(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyFloat()))
                    .thenThrow(new RuntimeException("rosetta error: cannot load libwebp"));

            // when
            java.util.Map<String, String> result = saveService.saveImage(file, "user-200", "PROFILE");

            // then
            Image found = imageRepository.findById(result.get("id")).orElseThrow();
            assertThat(found.getStorageObject()).isNotNull();
            // 폴백이므로 저장 포맷은 원본(PNG)이어야 한다
            assertThat(found.getStorageObject().getConvertedFormat().getCode()).isEqualTo("PNG");
            // 저장 파일 확장자도 .png 여야 한다
            assertThat(found.getStorageObject().getStorageLocation()).endsWith(".png");

            java.nio.file.Path stored = tempDir.resolve(found.getStorageObject().getStorageLocation()).normalize();
            assertThat(java.nio.file.Files.exists(stored)).isTrue();
        }
    }
}
