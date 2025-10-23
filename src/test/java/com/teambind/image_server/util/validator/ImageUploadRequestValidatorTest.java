package com.teambind.image_server.util.validator;

import com.teambind.image_server.config.InitialSetup;
import com.teambind.image_server.dto.request.ImageUploadRequest;
import com.teambind.image_server.fixture.TestFixtureFactory;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageUploadRequestValidatorTest {

	private ImageUploadRequestValidator validator;
	private ReferenceTypeValidator referenceTypeValidator;

	@Mock
	private ConstraintValidatorContext context;

	@Mock
	private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

	@BeforeEach
	void setUp() {
		referenceTypeValidator = new ReferenceTypeValidator();
		validator = new ImageUploadRequestValidator(referenceTypeValidator);

		// Mock context setup
		lenient().when(context.buildConstraintViolationWithTemplate(anyString()))
				.thenReturn(violationBuilder);
		lenient().when(violationBuilder.addConstraintViolation())
				.thenReturn(context);

		// InitialSetup 초기화
		InitialSetup.ALL_REFERENCE_TYPE_MAP.clear();
		InitialSetup.MONO_IMAGE_REFERENCE_TYPE_MAP.clear();
		InitialSetup.MULTI_IMAGE_REFERENCE_TYPE_MAP.clear();

		InitialSetup.ALL_REFERENCE_TYPE_MAP.put("PROFILE", TestFixtureFactory.createProfileReferenceType());
		InitialSetup.ALL_REFERENCE_TYPE_MAP.put("GALLERY", TestFixtureFactory.createGalleryReferenceType());

		InitialSetup.MONO_IMAGE_REFERENCE_TYPE_MAP.put("PROFILE", TestFixtureFactory.createProfileReferenceType());
		InitialSetup.MULTI_IMAGE_REFERENCE_TYPE_MAP.put("GALLERY", TestFixtureFactory.createGalleryReferenceType());
	}

	@Test
	@DisplayName("null 요청은 허용")
	void isValid_nullRequest_returnsTrue() {
		// when
		boolean result = validator.isValid(null, context);

		// then
		assertThat(result).isTrue();
	}

	// ===== 단일 이미지 업로드 테스트 =====

	@Test
	@DisplayName("정상 케이스: 단일 이미지 업로드")
	void isValid_singleImageUpload_returnsTrue() {
		// given
		ImageUploadRequest request = ImageUploadRequest.builder()
				.file(TestFixtureFactory.createValidImageFile())
				.uploaderId("user123")
				.category("PROFILE")
				.build();

		// when
		boolean result = validator.isValid(request, context);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("정상 케이스: 다중 이미지 업로드")
	void isValid_multipleImageUpload_returnsTrue() {
		// given
		ImageUploadRequest request = ImageUploadRequest.builder()
				.files(TestFixtureFactory.createValidImageFiles(3))
				.uploaderId("user123")
				.category("GALLERY")
				.build();

		// when
		boolean result = validator.isValid(request, context);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("단일과 다중을 동시에 보낼 수 없음")
	void isValid_bothSingleAndMultiple_returnsFalse() {
		// given
		ImageUploadRequest request = ImageUploadRequest.builder()
				.file(TestFixtureFactory.createValidImageFile())
				.files(TestFixtureFactory.createValidImageFiles(2))
				.uploaderId("user123")
				.category("PROFILE")
				.build();

		// when
		boolean result = validator.isValid(request, context);

		// then
		assertThat(result).isFalse();
		verify(context).buildConstraintViolationWithTemplate("단일 이미지와 다중 이미지를 동시에 업로드할 수 없습니다");
	}

	@Test
	@DisplayName("파일이 없으면 실패")
	void isValid_noFiles_returnsFalse() {
		// given
		ImageUploadRequest request = ImageUploadRequest.builder()
				.uploaderId("user123")
				.category("PROFILE")
				.build();

		// when
		boolean result = validator.isValid(request, context);

		// then
		assertThat(result).isFalse();
		verify(context).buildConstraintViolationWithTemplate("업로드할 이미지 파일이 없습니다");
	}

	@Test
	@DisplayName("빈 파일만 있으면 실패")
	void isValid_onlyEmptyFile_returnsFalse() {
		// given
		ImageUploadRequest request = ImageUploadRequest.builder()
				.file(TestFixtureFactory.createEmptyFile())
				.uploaderId("user123")
				.category("PROFILE")
				.build();

		// when
		boolean result = validator.isValid(request, context);

		// then
		assertThat(result).isFalse();
		verify(context).buildConstraintViolationWithTemplate("업로드할 이미지 파일이 없습니다");
	}

	// ===== 카테고리별 제약 검증 =====

	@Test
	@DisplayName("MONO 카테고리에 다중 이미지 업로드 시 실패")
	void isValid_monoCategory_multipleImages_returnsFalse() {
		// given
		ImageUploadRequest request = ImageUploadRequest.builder()
				.files(TestFixtureFactory.createValidImageFiles(3))
				.uploaderId("user123")
				.category("PROFILE")
				.build();

		// when
		boolean result = validator.isValid(request, context);

		// then
		assertThat(result).isFalse();
		verify(context).buildConstraintViolationWithTemplate(contains("단일 이미지만 허용합니다"));
	}

	@Test
	@DisplayName("MULTI 카테고리에 단일 이미지 업로드 시 실패")
	void isValid_multiCategory_singleImage_returnsFalse() {
		// given
		ImageUploadRequest request = ImageUploadRequest.builder()
				.file(TestFixtureFactory.createValidImageFile())
				.uploaderId("user123")
				.category("GALLERY")
				.build();

		// when
		boolean result = validator.isValid(request, context);

		// then
		assertThat(result).isFalse();
		verify(context).buildConstraintViolationWithTemplate(contains("다중 이미지만 허용합니다"));
	}

	@Test
	@DisplayName("카테고리가 null이어도 단일/다중 검증은 통과")
	void isValid_nullCategory_singleImage_returnsTrue() {
		// given
		ImageUploadRequest request = ImageUploadRequest.builder()
				.file(TestFixtureFactory.createValidImageFile())
				.uploaderId("user123")
				.category(null)
				.build();

		// when
		boolean result = validator.isValid(request, context);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("대소문자 혼합 카테고리도 정상 동작")
	void isValid_mixedCaseCategory_returnsTrue() {
		// given
		ImageUploadRequest request = ImageUploadRequest.builder()
				.file(TestFixtureFactory.createValidImageFile())
				.uploaderId("user123")
				.category("profile")
				.build();

		// when
		boolean result = validator.isValid(request, context);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("여러 케이스 통합 테스트")
	void isValid_variousCases() {
		// 정상: 단일 이미지 + MONO 카테고리
		ImageUploadRequest req1 = ImageUploadRequest.builder()
				.file(TestFixtureFactory.createValidImageFile())
				.category("PROFILE")
				.build();
		assertThat(validator.isValid(req1, context)).isTrue();

		// 정상: 다중 이미지 + MULTI 카테고리
		ImageUploadRequest req2 = ImageUploadRequest.builder()
				.files(TestFixtureFactory.createValidImageFiles(2))
				.category("GALLERY")
				.build();
		assertThat(validator.isValid(req2, context)).isTrue();

		// 실패: 단일 + 다중 동시
		ImageUploadRequest req3 = ImageUploadRequest.builder()
				.file(TestFixtureFactory.createValidImageFile())
				.files(TestFixtureFactory.createValidImageFiles(1))
				.category("PROFILE")
				.build();
		assertThat(validator.isValid(req3, context)).isFalse();
	}
}
