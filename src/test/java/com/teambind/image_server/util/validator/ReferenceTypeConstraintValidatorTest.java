package com.teambind.image_server.util.validator;

import com.teambind.image_server.config.InitialSetup;
import com.teambind.image_server.fixture.TestFixtureFactory;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ReferenceTypeConstraintValidatorTest {

	private ReferenceTypeConstraintValidator validator;
	private ReferenceTypeValidator referenceTypeValidator;

	@Mock
	private ConstraintValidatorContext context;

	@BeforeEach
	void setUp() {
		referenceTypeValidator = new ReferenceTypeValidator();
		validator = new ReferenceTypeConstraintValidator(referenceTypeValidator);

		// InitialSetup 초기화
		InitialSetup.ALL_REFERENCE_TYPE_MAP.clear();
		InitialSetup.MONO_IMAGE_REFERENCE_TYPE_MAP.clear();
		InitialSetup.MULTI_IMAGE_REFERENCE_TYPE_MAP.clear();

		InitialSetup.ALL_REFERENCE_TYPE_MAP.put("PROFILE", TestFixtureFactory.createProfileReferenceType());
		InitialSetup.ALL_REFERENCE_TYPE_MAP.put("GALLERY", TestFixtureFactory.createGalleryReferenceType());
		InitialSetup.ALL_REFERENCE_TYPE_MAP.put("PRODUCT", TestFixtureFactory.createMultiReferenceType("PRODUCT"));

		InitialSetup.MONO_IMAGE_REFERENCE_TYPE_MAP.put("PROFILE", TestFixtureFactory.createProfileReferenceType());
		InitialSetup.MULTI_IMAGE_REFERENCE_TYPE_MAP.put("GALLERY", TestFixtureFactory.createGalleryReferenceType());
		InitialSetup.MULTI_IMAGE_REFERENCE_TYPE_MAP.put("PRODUCT", TestFixtureFactory.createMultiReferenceType("PRODUCT"));
	}

	@Test
	@DisplayName("정상 케이스: 유효한 카테고리 (대문자)")
	void isValid_validCategoryUpperCase_returnsTrue() {
		// when
		boolean result = validator.isValid("PROFILE", context);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("정상 케이스: 유효한 카테고리 (소문자 - 대문자로 변환됨)")
	void isValid_validCategoryLowerCase_returnsTrue() {
		// when
		boolean result = validator.isValid("profile", context);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("정상 케이스: 유효한 카테고리 (혼합)")
	void isValid_validCategoryMixedCase_returnsTrue() {
		// when
		boolean result = validator.isValid("ProFile", context);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("null 값은 실패")
	void isValid_nullValue_returnsFalse() {
		// when
		boolean result = validator.isValid(null, context);

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("빈 문자열은 실패")
	void isValid_emptyString_returnsFalse() {
		// when
		boolean result = validator.isValid("", context);

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("공백 문자열은 실패")
	void isValid_blankString_returnsFalse() {
		// when
		boolean result = validator.isValid("   ", context);

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("존재하지 않는 카테고리는 실패")
	void isValid_nonExistentCategory_returnsFalse() {
		// when
		boolean result = validator.isValid("NON_EXISTENT", context);

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("유효한 다중 이미지 카테고리")
	void isValid_multiImageCategory_returnsTrue() {
		// when
		boolean result = validator.isValid("GALLERY", context);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("유효한 단일 이미지 카테고리")
	void isValid_monoImageCategory_returnsTrue() {
		// when
		boolean result = validator.isValid("PROFILE", context);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("여러 유효한 카테고리 테스트")
	void isValid_multipleValidCategories() {
		assertThat(validator.isValid("PROFILE", context)).isTrue();
		assertThat(validator.isValid("GALLERY", context)).isTrue();
		assertThat(validator.isValid("PRODUCT", context)).isTrue();
	}

	@Test
	@DisplayName("여러 무효한 카테고리 테스트")
	void isValid_multipleInvalidCategories() {
		assertThat(validator.isValid("INVALID", context)).isFalse();
		assertThat(validator.isValid("TEST", context)).isFalse();
		assertThat(validator.isValid("UNKNOWN", context)).isFalse();
	}
}
