package com.teambind.image_server.util.validator;

import com.teambind.image_server.config.InitialSetup;
import com.teambind.image_server.fixture.TestFixtureFactory;
import com.teambind.image_server.util.helper.ExtensionParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExtensionValidatorTest {

	private ExtensionValidator validator;
	private ExtensionParser parser;

	@BeforeEach
	void setUp() {
		parser = new ExtensionParser();
		validator = new ExtensionValidator(parser);

		// InitialSetup 초기화
		InitialSetup.EXTENSION_MAP.clear();
		InitialSetup.EXTENSION_MAP.put("JPG", TestFixtureFactory.createJpgExtension());
		InitialSetup.EXTENSION_MAP.put("PNG", TestFixtureFactory.createPngExtension());
		InitialSetup.EXTENSION_MAP.put("WEBP", TestFixtureFactory.createWebpExtension());
		InitialSetup.EXTENSION_MAP.put("JPEG", TestFixtureFactory.createExtension("JPEG"));
	}

	@Test
	@DisplayName("정상 케이스: JPG 확장자")
	void isValid_jpgExtension_returnsTrue() {
		// when
		boolean result = validator.isValid("test-image.jpg");

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("정상 케이스: PNG 확장자")
	void isValid_pngExtension_returnsTrue() {
		// when
		boolean result = validator.isValid("test-image.png");

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("정상 케이스: WEBP 확장자")
	void isValid_webpExtension_returnsTrue() {
		// when
		boolean result = validator.isValid("test-image.webp");

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("정상 케이스: JPEG 확장자")
	void isValid_jpegExtension_returnsTrue() {
		// when
		boolean result = validator.isValid("test-image.jpeg");

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("대문자 확장자도 정상 동작")
	void isValid_upperCaseExtension_returnsTrue() {
		// when
		boolean result = validator.isValid("test-image.JPG");

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("소문자 확장자도 정상 동작")
	void isValid_lowerCaseExtension_returnsTrue() {
		// when
		boolean result = validator.isValid("test-image.jpg");

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("혼합 케이스 확장자도 정상 동작")
	void isValid_mixedCaseExtension_returnsTrue() {
		// when
		boolean result = validator.isValid("test-image.JpG");

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("지원하지 않는 확장자는 실패")
	void isValid_unsupportedExtension_returnsFalse() {
		// when
		boolean result = validator.isValid("test-file.exe");

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("확장자 없는 파일은 예외 발생")
	void isValid_noExtension_throwsException() {
		// when & then
		assertThatThrownBy(() -> validator.isValid("noextension"))
				.isInstanceOf(Exception.class);
	}

	@Test
	@DisplayName("이중 확장자는 예외 발생")
	void isValid_doubleExtension_throwsException() {
		// when & then
		assertThatThrownBy(() -> validator.isValid("file.php.jpg"))
				.isInstanceOf(Exception.class);
	}

	@Test
	@DisplayName("Path traversal 패턴은 예외 발생")
	void isValid_pathTraversal_throwsException() {
		// when & then
		assertThatThrownBy(() -> validator.isValid("../../file.jpg"))
				.isInstanceOf(Exception.class);
	}

	@Test
	@DisplayName("null 파일명은 예외 발생")
	void isValid_nullFilename_throwsException() {
		// when & then
		assertThatThrownBy(() -> validator.isValid(null))
				.isInstanceOf(Exception.class);
	}

	@Test
	@DisplayName("빈 문자열 파일명은 예외 발생")
	void isValid_emptyFilename_throwsException() {
		// when & then
		assertThatThrownBy(() -> validator.isValid(""))
				.isInstanceOf(Exception.class);
	}

	@Test
	@DisplayName("여러 지원되는 확장자 테스트")
	void isValid_multipleSupportedExtensions() {
		assertThat(validator.isValid("image1.jpg")).isTrue();
		assertThat(validator.isValid("image2.png")).isTrue();
		assertThat(validator.isValid("image3.webp")).isTrue();
		assertThat(validator.isValid("image4.jpeg")).isTrue();
	}

	@Test
	@DisplayName("여러 지원되지 않는 확장자 테스트")
	void isValid_multipleUnsupportedExtensions() {
		assertThat(validator.isValid("file.exe")).isFalse();
		assertThat(validator.isValid("file.pdf")).isFalse();
		assertThat(validator.isValid("file.txt")).isFalse();
		assertThat(validator.isValid("file.zip")).isFalse();
	}
}
