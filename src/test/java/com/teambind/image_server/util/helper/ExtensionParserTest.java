package com.teambind.image_server.util.helper;

import com.teambind.image_server.exception.CustomException;
import com.teambind.image_server.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExtensionParserTest {

	private ExtensionParser extensionParser;

	@BeforeEach
	void setUp() {
		extensionParser = new ExtensionParser();
	}

	@Test
	@DisplayName("정상 케이스: 일반 파일명에서 확장자 추출")
	void extensionParse_validFileName_returnsExtension() {
		// given
		String fileName = "image.png";

		// when
		String result = extensionParser.extensionParse(fileName);

		// then
		assertThat(result).isEqualTo("PNG");
	}

	@Test
	@DisplayName("정상 케이스: 대소문자 혼합 확장자")
	void extensionParse_mixedCaseExtension_returnsUpperCase() {
		// given
		String fileName = "photo.JpG";

		// when
		String result = extensionParser.extensionParse(fileName);

		// then
		assertThat(result).isEqualTo("JPG");
	}

	@Test
	@DisplayName("파일명에 여러 점이 있는 경우 INVALID_FILE_EXTENSION 예외 발생")
	void extensionParse_multipleDotsInFileName_throwsException() {
		// when & then
		assertThatThrownBy(() -> extensionParser.extensionParse("my.photo.image.jpeg"))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorcode", ErrorCode.INVALID_FILE_EXTENSION);
	}

	@Test
	@DisplayName("null 입력 시 INVALID_FILE_NAME 예외 발생")
	void extensionParse_nullInput_throwsException() {
		// when & then
		assertThatThrownBy(() -> extensionParser.extensionParse(null))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorcode", ErrorCode.INVALID_FILE_NAME);
	}

	@Test
	@DisplayName("빈 문자열 입력 시 INVALID_FILE_NAME 예외 발생")
	void extensionParse_emptyString_throwsException() {
		// when & then
		assertThatThrownBy(() -> extensionParser.extensionParse(""))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorcode", ErrorCode.INVALID_FILE_NAME);
	}

	@Test
	@DisplayName("공백만 있는 문자열 입력 시 INVALID_FILE_NAME 예외 발생")
	void extensionParse_blankString_throwsException() {
		// when & then
		assertThatThrownBy(() -> extensionParser.extensionParse("   "))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorcode", ErrorCode.INVALID_FILE_NAME);
	}

	@Test
	@DisplayName("확장자 없는 파일명 입력 시 FILE_EXTENSION_NOT_FOUND 예외 발생")
	void extensionParse_noExtension_throwsException() {
		// when & then
		assertThatThrownBy(() -> extensionParser.extensionParse("noextension"))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorcode", ErrorCode.FILE_EXTENSION_NOT_FOUND);
	}

	@Test
	@DisplayName("점으로 끝나는 파일명 입력 시 FILE_EXTENSION_NOT_FOUND 예외 발생")
	void extensionParse_endsWithDot_throwsException() {
		// when & then
		assertThatThrownBy(() -> extensionParser.extensionParse("filename."))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorcode", ErrorCode.FILE_EXTENSION_NOT_FOUND);
	}

	@Test
	@DisplayName("확장자만 있는 파일명 입력 시 정상 처리")
	void extensionParse_onlyExtension_throwsException() {
		// when & then
		assertThatThrownBy(() -> extensionParser.extensionParse(".jpg"))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorcode", ErrorCode.FILE_EXTENSION_NOT_FOUND);
	}

	@Test
	@DisplayName("이중 확장자 입력 시 INVALID_FILE_EXTENSION 예외 발생")
	void extensionParse_doubleExtension_throwsException() {
		// when & then
		assertThatThrownBy(() -> extensionParser.extensionParse("file.php.jpg"))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorcode", ErrorCode.INVALID_FILE_EXTENSION);
	}

	@Test
	@DisplayName("Path traversal 패턴 (..) 포함 시 INVALID_FILE_NAME 예외 발생")
	void extensionParse_pathTraversalWithDoubleDots_throwsException() {
		// when & then
		assertThatThrownBy(() -> extensionParser.extensionParse("../../etc/passwd.jpg"))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorcode", ErrorCode.INVALID_FILE_NAME);
	}

	@Test
	@DisplayName("Path traversal 패턴 (/) 포함 시 INVALID_FILE_NAME 예외 발생")
	void extensionParse_pathTraversalWithSlash_throwsException() {
		// when & then
		assertThatThrownBy(() -> extensionParser.extensionParse("path/to/file.jpg"))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorcode", ErrorCode.INVALID_FILE_NAME);
	}

	@Test
	@DisplayName("Path traversal 패턴 (\\) 포함 시 INVALID_FILE_NAME 예외 발생")
	void extensionParse_pathTraversalWithBackslash_throwsException() {
		// when & then
		assertThatThrownBy(() -> extensionParser.extensionParse("path\\to\\file.jpg"))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorcode", ErrorCode.INVALID_FILE_NAME);
	}

	@Test
	@DisplayName("상대 경로 (..) 입력 시 INVALID_FILE_NAME 예외 발생")
	void extensionParse_relativePath_throwsException() {
		// when & then
		assertThatThrownBy(() -> extensionParser.extensionParse("../file.jpg"))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorcode", ErrorCode.INVALID_FILE_NAME);
	}
}