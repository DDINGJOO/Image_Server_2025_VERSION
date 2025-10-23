package com.teambind.image_server.util.validator;

import com.teambind.image_server.config.InitialSetup;
import com.teambind.image_server.fixture.TestFixtureFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReferenceTypeValidatorTest {

	private ReferenceTypeValidator validator;

	@BeforeEach
	void setUp() {
		validator = new ReferenceTypeValidator();

		// InitialSetup 초기화
		InitialSetup.ALL_REFERENCE_TYPE_MAP.clear();
		InitialSetup.MONO_IMAGE_REFERENCE_TYPE_MAP.clear();
		InitialSetup.MULTI_IMAGE_REFERENCE_TYPE_MAP.clear();

		InitialSetup.ALL_REFERENCE_TYPE_MAP.put("PROFILE", TestFixtureFactory.createProfileReferenceType());
		InitialSetup.ALL_REFERENCE_TYPE_MAP.put("GALLERY", TestFixtureFactory.createGalleryReferenceType());
		InitialSetup.ALL_REFERENCE_TYPE_MAP.put("PRODUCT", TestFixtureFactory.createMultiReferenceType("PRODUCT"));
		InitialSetup.ALL_REFERENCE_TYPE_MAP.put("BANNER", TestFixtureFactory.createMonoReferenceType("BANNER"));

		InitialSetup.MONO_IMAGE_REFERENCE_TYPE_MAP.put("PROFILE", TestFixtureFactory.createProfileReferenceType());
		InitialSetup.MONO_IMAGE_REFERENCE_TYPE_MAP.put("BANNER", TestFixtureFactory.createMonoReferenceType("BANNER"));

		InitialSetup.MULTI_IMAGE_REFERENCE_TYPE_MAP.put("GALLERY", TestFixtureFactory.createGalleryReferenceType());
		InitialSetup.MULTI_IMAGE_REFERENCE_TYPE_MAP.put("PRODUCT", TestFixtureFactory.createMultiReferenceType("PRODUCT"));
	}

	// ===== referenceTypeValidate 테스트 =====

	@Test
	@DisplayName("정상 케이스: 존재하는 ReferenceType (대문자)")
	void referenceTypeValidate_existingType_returnsTrue() {
		// when
		boolean result = validator.referenceTypeValidate("PROFILE");

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("정상 케이스: 존재하는 ReferenceType (소문자)")
	void referenceTypeValidate_existingTypeLowerCase_returnsTrue() {
		// when
		boolean result = validator.referenceTypeValidate("profile");

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("정상 케이스: 존재하는 ReferenceType (혼합)")
	void referenceTypeValidate_existingTypeMixedCase_returnsTrue() {
		// when
		boolean result = validator.referenceTypeValidate("ProFile");

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("존재하지 않는 ReferenceType은 실패")
	void referenceTypeValidate_nonExistingType_returnsFalse() {
		// when
		boolean result = validator.referenceTypeValidate("NON_EXISTENT");

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("여러 존재하는 ReferenceType 검증")
	void referenceTypeValidate_multipleExistingTypes() {
		assertThat(validator.referenceTypeValidate("PROFILE")).isTrue();
		assertThat(validator.referenceTypeValidate("GALLERY")).isTrue();
		assertThat(validator.referenceTypeValidate("PRODUCT")).isTrue();
		assertThat(validator.referenceTypeValidate("BANNER")).isTrue();
	}

	// ===== isMonoImageReferenceType 테스트 =====

	@Test
	@DisplayName("PROFILE은 단일 이미지 타입")
	void isMonoImageReferenceType_profile_returnsTrue() {
		// when
		boolean result = validator.isMonoImageReferenceType("PROFILE");

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("BANNER는 단일 이미지 타입")
	void isMonoImageReferenceType_banner_returnsTrue() {
		// when
		boolean result = validator.isMonoImageReferenceType("BANNER");

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("GALLERY는 단일 이미지 타입이 아님")
	void isMonoImageReferenceType_gallery_returnsFalse() {
		// when
		boolean result = validator.isMonoImageReferenceType("GALLERY");

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("소문자로 검증해도 정상 동작")
	void isMonoImageReferenceType_lowerCase_returnsTrue() {
		// when
		boolean result = validator.isMonoImageReferenceType("profile");

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("존재하지 않는 타입은 false")
	void isMonoImageReferenceType_nonExistent_returnsFalse() {
		// when
		boolean result = validator.isMonoImageReferenceType("NON_EXISTENT");

		// then
		assertThat(result).isFalse();
	}

	// ===== isMultiImageReferenceType 테스트 =====

	@Test
	@DisplayName("GALLERY는 다중 이미지 타입")
	void isMultiImageReferenceType_gallery_returnsTrue() {
		// when
		boolean result = validator.isMultiImageReferenceType("GALLERY");

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("PRODUCT는 다중 이미지 타입")
	void isMultiImageReferenceType_product_returnsTrue() {
		// when
		boolean result = validator.isMultiImageReferenceType("PRODUCT");

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("PROFILE은 다중 이미지 타입이 아님")
	void isMultiImageReferenceType_profile_returnsFalse() {
		// when
		boolean result = validator.isMultiImageReferenceType("PROFILE");

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("소문자로 검증해도 정상 동작")
	void isMultiImageReferenceType_lowerCase_returnsTrue() {
		// when
		boolean result = validator.isMultiImageReferenceType("gallery");

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("존재하지 않는 타입은 false")
	void isMultiImageReferenceType_nonExistent_returnsFalse() {
		// when
		boolean result = validator.isMultiImageReferenceType("NON_EXISTENT");

		// then
		assertThat(result).isFalse();
	}

	// ===== 통합 테스트 =====

	@Test
	@DisplayName("모든 ReferenceType의 단일/다중 구분 테스트")
	void validateAllReferenceTypes() {
		// MONO types
		assertThat(validator.isMonoImageReferenceType("PROFILE")).isTrue();
		assertThat(validator.isMonoImageReferenceType("BANNER")).isTrue();
		assertThat(validator.isMultiImageReferenceType("PROFILE")).isFalse();
		assertThat(validator.isMultiImageReferenceType("BANNER")).isFalse();

		// MULTI types
		assertThat(validator.isMultiImageReferenceType("GALLERY")).isTrue();
		assertThat(validator.isMultiImageReferenceType("PRODUCT")).isTrue();
		assertThat(validator.isMonoImageReferenceType("GALLERY")).isFalse();
		assertThat(validator.isMonoImageReferenceType("PRODUCT")).isFalse();
	}

	@Test
	@DisplayName("대소문자 혼합 케이스 통합 테스트")
	void validateMixedCaseCombinations() {
		assertThat(validator.referenceTypeValidate("PrOfIlE")).isTrue();
		assertThat(validator.isMonoImageReferenceType("PrOfIlE")).isTrue();

		assertThat(validator.referenceTypeValidate("GaLlErY")).isTrue();
		assertThat(validator.isMultiImageReferenceType("GaLlErY")).isTrue();
	}
}
