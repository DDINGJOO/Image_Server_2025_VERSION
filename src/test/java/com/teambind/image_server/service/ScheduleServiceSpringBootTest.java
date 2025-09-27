package com.teambind.image_server.service;

import com.teambind.image_server.entity.Image;
import com.teambind.image_server.entity.ReferenceType;
import com.teambind.image_server.enums.ImageStatus;
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

@SpringBootTest
@ActiveProfiles("test")
class ScheduleServiceSpringBootTest {

    @TempDir
    static Path tempDir;
    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private ImageRepository imageRepository;
    @Autowired
    private ReferenceTypeRepository referenceTypeRepository;
    private ReferenceType refProfile;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("images.upload.dir", () -> tempDir.toAbsolutePath().toString());
    }

    @BeforeEach
    void setUp() {
        imageRepository.deleteAll();
        refProfile = referenceTypeRepository.findAll().stream()
                .filter(r -> r.getCode().equals("PROFILE"))
                .findFirst().orElseThrow();
    }

    @AfterEach
    void tearDown() {
        imageRepository.deleteAll();
    }

    @Test
    @DisplayName("cleanUpUnusedImages: CONFIRMED 제외, 2일 초과된 이미지만 삭제")
    void cleanUpUnusedImages_deletesOnlyOldAndUnconfirmed() {
        // given
        Image oldTemp = Image.builder()
                .id("img-old-temp")
                .status(ImageStatus.TEMP)
                .referenceType(refProfile)
                .referenceId("ref-1")
                .imageUrl("http://local/images/old.webp")
                .uploaderId("user-1")
                .idDeleted(false)
                .createdAt(LocalDateTime.now().minusDays(3))
                .build();

        Image recentTemp = Image.builder()
                .id("img-recent-temp")
                .status(ImageStatus.TEMP)
                .referenceType(refProfile)
                .referenceId("ref-2")
                .imageUrl("http://local/images/recent.webp")
                .uploaderId("user-1")
                .idDeleted(false)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        Image oldConfirmed = Image.builder()
                .id("img-old-confirmed")
                .status(ImageStatus.CONFIRMED)
                .referenceType(refProfile)
                .referenceId("ref-3")
                .imageUrl("http://local/images/conf.webp")
                .uploaderId("user-1")
                .idDeleted(false)
                .createdAt(LocalDateTime.now().minusDays(10))
                .build();

        imageRepository.save(oldTemp);
        imageRepository.save(recentTemp);
        imageRepository.save(oldConfirmed);

        // when
        scheduleService.cleanUpUnusedImages();

        // then
        assertThat(imageRepository.findById("img-old-temp")).isEmpty();
        assertThat(imageRepository.findById("img-recent-temp")).isPresent();
        assertThat(imageRepository.findById("img-old-confirmed")).isPresent();
    }
}
