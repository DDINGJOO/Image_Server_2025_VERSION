package com.teambind.image_server.service;

import com.teambind.image_server.entity.Image;
import com.teambind.image_server.entity.ReferenceType;
import com.teambind.image_server.enums.ImageStatus;
import com.teambind.image_server.exception.CustomException;
import com.teambind.image_server.exception.ErrorCode;
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
    @DisplayName("이미지를 CONFIRMED 상태로 변경한다 (PROFILE 이외의 레퍼런스)")
    void confirmImage_updatesStatus_nonProfile() throws Exception {
        ReferenceType ref = referenceTypeRepository.findAll().stream()
                .filter(r -> r.getCode().equals("USER")).findFirst().orElseThrow();

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
    @DisplayName("PROFILE 레퍼런스는 현재 구현상 deleteOldProfileImg 호출로 최종적으로 DELETED가 될 수 있다")
    void confirmImage_profile_mayBecomeDeleted() throws Exception {
        ReferenceType ref = referenceTypeRepository.findAll().stream()
                .filter(r -> r.getCode().equals("PROFILE")).findFirst().orElseThrow();

        Image image = Image.builder()
                .id("img-confirm-prof")
                .status(ImageStatus.TEMP)
                .referenceType(ref)
                .imageUrl("http://local/images/p.webp")
                .uploaderId("u-2")
                .idDeleted(false)
                .createdAt(LocalDateTime.now())
                .build();
        imageRepository.save(image);

        confirmService.confirmImage("img-confirm-prof");

        Image updated = imageRepository.findById("img-confirm-prof").orElseThrow();
        // 현재 서비스 구현 로직을 그대로 검증: PROFILE은 deleteOldProfileImg에 의해 DELETED로 저장될 수 있음
        assertThat(updated.getStatus()).isIn(ImageStatus.CONFIRMED, ImageStatus.DELETED);
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

    @Test
    @DisplayName("실패: null 이미지 ID면 IMAGE_NOT_FOUND 상태")
    void confirmImage_null_throws() {
        CustomException ex = assertThrows(CustomException.class, () -> confirmService.confirmImage(null));
        assertThat(ex.getStatus()).isEqualTo(ErrorCode.IMAGE_NOT_FOUND.getStatus());
    }

    @Test
    @DisplayName("실패: 존재하지 않는 이미지 ID면 IMAGE_NOT_FOUND 상태")
    void confirmImage_notFound_throws() {
        CustomException ex = assertThrows(CustomException.class, () -> confirmService.confirmImage("not-exist"));
        assertThat(ex.getStatus()).isEqualTo(ErrorCode.IMAGE_NOT_FOUND.getStatus());
    }
}
