package com.teambind.image_server.repository;

import com.teambind.image_server.entity.Image;
import com.teambind.image_server.entity.ReferenceType;
import com.teambind.image_server.enums.ImageStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ImageRepository 기본 동작 검증
 * - 각 테스트 전후로 이미지 테이블 정리를 수행하여 테스트 간 영향을 차단한다.
 */
@ActiveProfiles("test")
@SpringBootTest
class ImageRepositoryDataJpaTest {

    @Autowired
    private ImageRepository imageRepository;
    @Autowired
    private ReferenceTypeRepository referenceTypeRepository;

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {

    }

    @Test
    @DisplayName("이미지를 저장/조회할 수 있다")
    void saveAndFindById() {
        ReferenceType ref = referenceTypeRepository.findAll().stream()
                .filter(r -> r.getCode().equals("PROFILE")).findFirst().orElseThrow();

        Image image = Image.builder()
                .id("img-1")
                .status(ImageStatus.TEMP)
                .referenceType(ref)
                .imageUrl("http://local/images/x.webp")
                .uploaderId("user-1")
                .idDeleted(false)
                .createdAt(LocalDateTime.now())
                .build();

        imageRepository.save(image);
        imageRepository.flush();

        assertThat(imageRepository.findById("img-1")).isPresent();
    }

    @Test
    @DisplayName("업로더/레퍼런스 타입으로 단건 조회한다")
    void findByUploaderAndRefType() {
        ReferenceType ref = referenceTypeRepository.findAll().stream()
                .filter(r -> r.getCode().equals("PROFILE")).findFirst().orElseThrow();

        Image image = Image.builder()
                .id("img-2")
                .status(ImageStatus.TEMP)
                .referenceType(ref)
                .imageUrl("http://local/images/y.webp")
                .uploaderId("user-2")
                .idDeleted(false)
                .createdAt(LocalDateTime.now())
                .build();
        imageRepository.save(image);

        Image found = imageRepository.findByIdAndUploaderIdAndReferenceType("img-2", "user-2", ref);
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo("img-2");
    }
}
