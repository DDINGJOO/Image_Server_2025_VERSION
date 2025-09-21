package com.teambind.image_server.util.validator;

import com.teambind.image_server.ImageServerApplication;
import com.teambind.image_server.config.DataInitializer;
import com.teambind.image_server.entity.Extension;
import com.teambind.image_server.entity.ReferenceType;
import com.teambind.image_server.service.ImageConfirmService;
import com.teambind.image_server.service.ImageSaveService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = ValidatorSpringBootTest.TestApp.class)
@ActiveProfiles("test")
class ValidatorSpringBootTest {

    // 최소 구성의 스프링 부트 설정: DataInitializer 및 서비스 빈을 스캔에서 제외한다
    @SpringBootConfiguration
    @ComponentScan(basePackages = "com.teambind.image_server",
            excludeFilters = {
                    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = DataInitializer.class),
                    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ImageConfirmService.class),
                    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ImageSaveService.class)
            })
    static class TestApp {}

    @Autowired
    ExtensionParser extensionParser;
    @Autowired
    ExtensionValidator extensionValidator;
    @Autowired
    ReferenceValidator referenceValidator;

    @BeforeEach
    void setUp() {
        // DB 없이 동작하도록 정적 맵을 테스트용으로 채워준다
        ImageServerApplication.extensionMap.clear();
        ImageServerApplication.referenceTypeMap.clear();

        // 예시 확장자/레퍼런스 타입을 등록
        ImageServerApplication.extensionMap.put("PNG", Extension.builder().code("PNG").name("PNG").build());
        ImageServerApplication.extensionMap.put("JPG", Extension.builder().code("JPG").name("JPG").build());

        ImageServerApplication.referenceTypeMap.put("USER", ReferenceType.builder().code("USER").name("사용자").build());
        ImageServerApplication.referenceTypeMap.put("POST", ReferenceType.builder().code("POST").name("게시글").build());
    }

    @AfterEach
    void tearDown() {
        // 각 테스트 종료 후 정적 맵을 정리하여 다음 테스트에 영향이 없도록 한다
        ImageServerApplication.extensionMap.clear();
        ImageServerApplication.referenceTypeMap.clear();
    }

    @Nested
    @DisplayName("ExtensionParser: 확장자 파싱 테스트")
    class ExtensionParserTests {
        @Test
        @DisplayName("파일명에서 확장자를 대문자로 파싱한다")
        void parsesExtensionToUpperCase() {
            assertThat(extensionParser.extensionParse("image.png")).isEqualTo("PNG");
            assertThat(extensionParser.extensionParse("photo.JPG")).isEqualTo("JPG");
            assertThat(extensionParser.extensionParse("archive.tar.Gz")).isEqualTo("GZ");
        }
    }

    @Nested
    @DisplayName("ExtensionValidator: 지원 확장자 검증")
    class ExtensionValidatorTests {
        @Test
        @DisplayName("등록된 확장자는 true, 미등록 확장자는 false")
        void validateKnownExtensions() {
            assertThat(extensionValidator.isValid("test.png")).isTrue();
            assertThat(extensionValidator.isValid("test.jpg")).isTrue();
            assertThat(extensionValidator.isValid("test.webp")).isFalse();
        }
    }

    @Nested
    @DisplayName("ReferenceValidator: 참조 타입 검증")
    class ReferenceValidatorTests {
        @Test
        @DisplayName("등록된 참조 타입은 true, 미등록 참조 타입은 false")
        void validateReferenceTypes() {
            assertThat(referenceValidator.referenceValidate("user")).isTrue();
            assertThat(referenceValidator.referenceValidate("POST")).isTrue();
            assertThat(referenceValidator.referenceValidate("comment")).isFalse();
        }
    }
}
