package com.teambind.image_server.util.validator;

import com.teambind.image_server.config.InitialSetup;
import com.teambind.image_server.fixture.TestFixtureFactory;
import com.teambind.image_server.util.helper.ExtensionParser;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ImageFileValidatorTest {

	private ImageFileValidator validator;

	@Mock
	private ExtensionValidator extensionValidator;

	@Mock
	private ConstraintValidatorContext context;

	@Mock
	private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

	@BeforeEach
	void setUp() {
		validator = new ImageFileValidator(extensionValidator);

		// Mock context setup (lenient to avoid unnecessary stubbing errors)
		lenient().when(context.buildConstraintViolationWithTemplate(anyString()))
				.thenReturn(violationBuilder);
		lenient().when(violationBuilder.addConstraintViolation())
				.thenReturn(context);

		// InitialSetup 초기화 (Extension Map)
		InitialSetup.EXTENSION_MAP.clear();
		InitialSetup.EXTENSION_MAP.put("JPG", TestFixtureFactory.createJpgExtension());
		InitialSetup.EXTENSION_MAP.put("PNG", TestFixtureFactory.createPngExtension());
		InitialSetup.EXTENSION_MAP.put("WEBP", TestFixtureFactory.createWebpExtension());
	}

	// ===== 단일 파일 테스트 =====

	@Test
	@DisplayName("정상 케이스: 유효한 JPG 파일")
	void isValid_validJpgFile_returnsTrue() {
		// given
		MultipartFile file = TestFixtureFactory.createValidImageFile();
		when(extensionValidator.isValid("test-image.jpg")).thenReturn(true);

		// when
		boolean result = validator.isValid(file, context);

		// then
		assertThat(result).isTrue();
		verify(extensionValidator).isValid("test-image.jpg");
	}

	@Test
	@DisplayName("정상 케이스: 유효한 PNG 파일")
	void isValid_validPngFile_returnsTrue() {
		// given
		MultipartFile file = TestFixtureFactory.createValidPngFile();
		when(extensionValidator.isValid("test-image.png")).thenReturn(true);

		// when
		boolean result = validator.isValid(file, context);

		// then
		assertThat(result).isTrue();
		verify(extensionValidator).isValid("test-image.png");
	}

	@Test
	@DisplayName("null 파일은 허용 (필수 여부는 별도 검증)")
	void isValid_nullFile_returnsTrue() {
		// when
		boolean result = validator.isValid(null, context);

		// then
		assertThat(result).isTrue();
		verifyNoInteractions(extensionValidator);
	}

	@Test
	@DisplayName("빈 파일은 허용 (필수 여부는 별도 검증)")
	void isValid_emptyFile_returnsTrue() {
		// given
		MultipartFile file = TestFixtureFactory.createEmptyFile();

		// when
		boolean result = validator.isValid(file, context);

		// then
		assertThat(result).isTrue();
		verifyNoInteractions(extensionValidator);
	}

	@Test
	@DisplayName("파일명이 null인 경우 실패")
	void isValid_nullFilename_returnsFalse() {
		// given
		MultipartFile file = TestFixtureFactory.createImageFile(null, "image/jpeg", "content".getBytes());

		// when
		boolean result = validator.isValid(file, context);

		// then
		assertThat(result).isFalse();
		verify(context).disableDefaultConstraintViolation();
		verify(context).buildConstraintViolationWithTemplate("파일명이 유효하지 않습니다");
	}

	@Test
	@DisplayName("파일명이 빈 문자열인 경우 실패")
	void isValid_blankFilename_returnsFalse() {
		// given
		MultipartFile file = TestFixtureFactory.createImageFile("  ", "image/jpeg", "content".getBytes());

		// when
		boolean result = validator.isValid(file, context);

		// then
		assertThat(result).isFalse();
		verify(context).disableDefaultConstraintViolation();
		verify(context).buildConstraintViolationWithTemplate("파일명이 유효하지 않습니다");
	}

	@Test
	@DisplayName("지원하지 않는 확장자인 경우 실패")
	void isValid_unsupportedExtension_returnsFalse() {
		// given
		MultipartFile file = TestFixtureFactory.createFileWithInvalidExtension();
		when(extensionValidator.isValid("test.exe")).thenReturn(false);

		// when
		boolean result = validator.isValid(file, context);

		// then
		assertThat(result).isFalse();
		verify(context).disableDefaultConstraintViolation();
		verify(context).buildConstraintViolationWithTemplate("지원하지 않는 파일 확장자입니다");
	}

	@Test
	@DisplayName("확장자 검증 중 예외 발생 시 실패")
	void isValid_extensionValidationException_returnsFalse() {
		// given
		MultipartFile file = TestFixtureFactory.createValidImageFile();
		when(extensionValidator.isValid(anyString())).thenThrow(new RuntimeException("Validation error"));

		// when
		boolean result = validator.isValid(file, context);

		// then
		assertThat(result).isFalse();
		verify(context).disableDefaultConstraintViolation();
		verify(context).buildConstraintViolationWithTemplate(contains("파일 검증 중 오류가 발생했습니다"));
	}

	// ===== 다중 파일 테스트 =====

	@Test
	@DisplayName("정상 케이스: 유효한 다중 파일")
	void isValid_validMultipleFiles_returnsTrue() {
		// given
		List<MultipartFile> files = TestFixtureFactory.createValidImageFiles(3);
		when(extensionValidator.isValid(anyString())).thenReturn(true);

		// when
		boolean result = validator.isValid(files, context);

		// then
		assertThat(result).isTrue();
		verify(extensionValidator, times(3)).isValid(anyString());
	}

	@Test
	@DisplayName("빈 리스트는 허용")
	void isValid_emptyList_returnsTrue() {
		// given
		List<MultipartFile> files = new ArrayList<>();

		// when
		boolean result = validator.isValid(files, context);

		// then
		assertThat(result).isTrue();
		verifyNoInteractions(extensionValidator);
	}

	@Test
	@DisplayName("리스트에 MultipartFile이 아닌 객체가 있으면 실패")
	void isValid_invalidItemInList_returnsFalse() {
		// given
		List<Object> files = new ArrayList<>();
		files.add("not a file");

		// when
		boolean result = validator.isValid(files, context);

		// then
		assertThat(result).isFalse();
		verify(context).buildConstraintViolationWithTemplate("유효하지 않은 파일 형식입니다");
	}

	@Test
	@DisplayName("다중 파일 중 하나의 파일명이 null인 경우 실패")
	void isValid_multipleFiles_oneHasNullFilename_returnsFalse() {
		// given
		List<MultipartFile> files = new ArrayList<>();
		files.add(TestFixtureFactory.createValidImageFile());
		files.add(TestFixtureFactory.createImageFile(null, "image/jpeg", "content".getBytes()));

		when(extensionValidator.isValid(anyString())).thenReturn(true);

		// when
		boolean result = validator.isValid(files, context);

		// then
		assertThat(result).isFalse();
		verify(context).buildConstraintViolationWithTemplate(contains("파일[1]의 파일명이 유효하지 않습니다"));
	}

	@Test
	@DisplayName("다중 파일 중 하나의 확장자가 지원되지 않는 경우 실패")
	void isValid_multipleFiles_oneHasUnsupportedExtension_returnsFalse() {
		// given
		List<MultipartFile> files = new ArrayList<>();
		files.add(TestFixtureFactory.createValidImageFile());
		files.add(TestFixtureFactory.createFileWithInvalidExtension());

		when(extensionValidator.isValid("test-image.jpg")).thenReturn(true);
		when(extensionValidator.isValid("test.exe")).thenReturn(false);

		// when
		boolean result = validator.isValid(files, context);

		// then
		assertThat(result).isFalse();
		verify(context).buildConstraintViolationWithTemplate(contains("파일[1]의 확장자가 지원되지 않습니다"));
	}

	@Test
	@DisplayName("다중 파일 중 하나의 검증에서 예외 발생 시 실패")
	void isValid_multipleFiles_oneThrowsException_returnsFalse() {
		// given
		List<MultipartFile> files = new ArrayList<>();
		files.add(TestFixtureFactory.createValidImageFile());
		files.add(TestFixtureFactory.createValidPngFile());

		when(extensionValidator.isValid("test-image.jpg")).thenReturn(true);
		when(extensionValidator.isValid("test-image.png")).thenThrow(new RuntimeException("Error"));

		// when
		boolean result = validator.isValid(files, context);

		// then
		assertThat(result).isFalse();
		verify(context).buildConstraintViolationWithTemplate(contains("파일[1] 검증 중 오류가 발생했습니다"));
	}

	@Test
	@DisplayName("다중 파일에 빈 파일이 포함되어 있어도 허용")
	void isValid_multipleFiles_withEmptyFile_returnsTrue() {
		// given
		List<MultipartFile> files = new ArrayList<>();
		files.add(TestFixtureFactory.createValidImageFile());
		files.add(TestFixtureFactory.createEmptyFile());
		files.add(TestFixtureFactory.createValidPngFile());

		when(extensionValidator.isValid(anyString())).thenReturn(true);

		// when
		boolean result = validator.isValid(files, context);

		// then
		assertThat(result).isTrue();
		// 빈 파일은 검증하지 않으므로 2번만 호출
		verify(extensionValidator, times(2)).isValid(anyString());
	}

	@Test
	@DisplayName("MultipartFile도 List도 아닌 객체는 실패")
	void isValid_invalidObjectType_returnsFalse() {
		// when
		boolean result = validator.isValid("not a file", context);

		// then
		assertThat(result).isFalse();
		verifyNoInteractions(extensionValidator);
	}
}
