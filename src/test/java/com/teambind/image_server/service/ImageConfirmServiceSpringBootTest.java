package com.teambind.image_server.service;

import com.teambind.image_server.entity.Image;
import com.teambind.image_server.entity.ReferenceType;
import com.teambind.image_server.enums.ImageStatus;
import com.teambind.image_server.exception.CustomException;
import com.teambind.image_server.repository.ImageRepository;
import com.teambind.image_server.repository.ReferenceTypeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 서비스 통합 테스트: ImageConfirmService
 * - 이미지 상태를 CONFIRMED로 변경하는 로직을 검증한다.
 * - 각 테스트 전후로 DB 정리를 수행하여 테스트 간 독립성을 보장한다.
 */
@SpringBootTest
@ActiveProfiles("test")
class ImageConfirmServiceSpringBootTest {

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        // 파일 시스템 접근을 안전한 임시 디렉터리로 고정
        r.add("images.upload.dir", () -> tempDir.toAbsolutePath().toString());
    }

    @Autowired
    private ImageConfirmService confirmService;
    @Autowired
    private ImageRepository imageRepository;
    @Autowired
    private ReferenceTypeRepository referenceTypeRepository;

    @BeforeEach
    void beforeEach() {
        // 각 테스트 시작 전 이미지 테이블 정리
        imageRepository.deleteAll();
    }

    @AfterEach
    void afterEach() {
        // 각 테스트 종료 후 이미지 테이블 정리
        imageRepository.deleteAll();
    }

    @Test
    @DisplayName("이미지를 CONFIRMED 상태로 변경한다")
    void confirmImage_updatesStatus() throws Exception {
        ReferenceType ref = referenceTypeRepository.findAll().stream()
                .filter(r -> r.getCode().equals("PROFILE")).findFirst().orElseThrow();

        Image image = Image.builder()
                .id("img-confirm-1")
                .status(ImageStatus.TEMP)
                .referenceType(ref)
                .imageUrl("http://local/images/a.webp")
                .uploaderId("u-1")
                .idDeleted(false)
                .createdAt(LocalDateTime.now())
                .build();
        imageRepository.save(image);

        // when
        confirmService.confirmImage("img-confirm-1");

        // then
        Image updated = imageRepository.findById("img-confirm-1").orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ImageStatus.CONFIRMED);
    }

    @Test
    @DisplayName("이미 CONFIRMED 상태면 예외를 던진다")
    void confirmImage_alreadyConfirmed_throws() throws Exception {
        ReferenceType ref = referenceTypeRepository.findAll().stream()
                .filter(r -> r.getCode().equals("PROFILE")).findFirst().orElseThrow();

        Image image = Image.builder()
                .id("img-confirm-2")
                .status(ImageStatus.CONFIRMED)
                .referenceType(ref)
                .imageUrl("http://local/images/b.webp")
                .uploaderId("u-1")
                .idDeleted(false)
                .createdAt(LocalDateTime.now())
                .build();
        imageRepository.save(image);

        assertThrows(CustomException.class, () -> confirmService.confirmImage("img-confirm-2"));
    }
}
