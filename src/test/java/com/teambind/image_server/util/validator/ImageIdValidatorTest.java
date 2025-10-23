package com.teambind.image_server.util.validator;

import com.teambind.image_server.repository.ImageRepository;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageIdValidatorTest {

	private ImageIdValidator validator;

	@Mock
	private ImageRepository imageRepository;

	@Mock
	private ConstraintValidatorContext context;

	@Mock
	private ValidImageId annotation;

	@BeforeEach
	void setUp() {
		validator = new ImageIdValidator(imageRepository);
	}

	@Test
	@DisplayName("정상 케이스: 존재하는 이미지 ID")
	void isValid_existingImageId_returnsTrue() {
		// given
		when(annotation.allowEmpty()).thenReturn(false);
		validator.initialize(annotation);
		when(imageRepository.existsById("image-123")).thenReturn(true);

		// when
		boolean result = validator.isValid("image-123", context);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("존재하지 않는 이미지 ID는 실패")
	void isValid_nonExistingImageId_returnsFalse() {
		// given
		when(annotation.allowEmpty()).thenReturn(false);
		validator.initialize(annotation);
		when(imageRepository.existsById("non-existing")).thenReturn(false);

		// when
		boolean result = validator.isValid("non-existing", context);

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("null 값은 allowEmpty=false일 때 실패")
	void isValid_null_allowEmptyFalse_returnsFalse() {
		// given
		when(annotation.allowEmpty()).thenReturn(false);
		validator.initialize(annotation);

		// when
		boolean result = validator.isValid(null, context);

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("null 값은 allowEmpty=true일 때 허용")
	void isValid_null_allowEmptyTrue_returnsTrue() {
		// given
		when(annotation.allowEmpty()).thenReturn(true);
		validator.initialize(annotation);

		// when
		boolean result = validator.isValid(null, context);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("빈 문자열은 allowEmpty=false일 때 실패")
	void isValid_empty_allowEmptyFalse_returnsFalse() {
		// given
		when(annotation.allowEmpty()).thenReturn(false);
		validator.initialize(annotation);

		// when
		boolean result = validator.isValid("", context);

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("빈 문자열은 allowEmpty=true일 때 허용")
	void isValid_empty_allowEmptyTrue_returnsTrue() {
		// given
		when(annotation.allowEmpty()).thenReturn(true);
		validator.initialize(annotation);

		// when
		boolean result = validator.isValid("", context);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("공백 문자열은 allowEmpty=false일 때 실패")
	void isValid_blank_allowEmptyFalse_returnsFalse() {
		// given
		when(annotation.allowEmpty()).thenReturn(false);
		validator.initialize(annotation);

		// when
		boolean result = validator.isValid("   ", context);

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("공백 문자열은 allowEmpty=true일 때 허용")
	void isValid_blank_allowEmptyTrue_returnsTrue() {
		// given
		when(annotation.allowEmpty()).thenReturn(true);
		validator.initialize(annotation);

		// when
		boolean result = validator.isValid("   ", context);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("여러 존재하는 이미지 ID 검증")
	void isValid_multipleExistingIds() {
		// given
		when(annotation.allowEmpty()).thenReturn(false);
		validator.initialize(annotation);
		when(imageRepository.existsById("id-1")).thenReturn(true);
		when(imageRepository.existsById("id-2")).thenReturn(true);
		when(imageRepository.existsById("id-3")).thenReturn(true);

		// when & then
		assertThat(validator.isValid("id-1", context)).isTrue();
		assertThat(validator.isValid("id-2", context)).isTrue();
		assertThat(validator.isValid("id-3", context)).isTrue();
	}

	@Test
	@DisplayName("여러 존재하지 않는 이미지 ID 검증")
	void isValid_multipleNonExistingIds() {
		// given
		when(annotation.allowEmpty()).thenReturn(false);
		validator.initialize(annotation);
		when(imageRepository.existsById("fake-1")).thenReturn(false);
		when(imageRepository.existsById("fake-2")).thenReturn(false);

		// when & then
		assertThat(validator.isValid("fake-1", context)).isFalse();
		assertThat(validator.isValid("fake-2", context)).isFalse();
	}
}
