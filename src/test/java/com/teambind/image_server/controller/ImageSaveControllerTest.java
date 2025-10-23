package com.teambind.image_server.controller;

import com.teambind.image_server.config.InitialSetup;
import com.teambind.image_server.fixture.TestFixtureFactory;
import com.teambind.image_server.repository.ImageRepository;
import com.teambind.image_server.service.ImageSaveService;
import com.teambind.image_server.util.helper.ExtensionParser;
import com.teambind.image_server.util.validator.ExtensionValidator;
import com.teambind.image_server.util.validator.ReferenceTypeValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ImageSaveController.class)
@Import({ExtensionValidator.class, ReferenceTypeValidator.class, ExtensionParser.class})
class ImageSaveControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private ImageSaveService imageSaveService;

	@MockBean
	private ImageRepository imageRepository;

	@BeforeEach
	void setUp() {
		// InitialSetup 초기화
		InitialSetup.ALL_REFERENCE_TYPE_MAP.clear();
		InitialSetup.EXTENSION_MAP.clear();

		InitialSetup.ALL_REFERENCE_TYPE_MAP.put("PROFILE", TestFixtureFactory.createProfileReferenceType());
		InitialSetup.ALL_REFERENCE_TYPE_MAP.put("GALLERY", TestFixtureFactory.createGalleryReferenceType());
		InitialSetup.EXTENSION_MAP.put("JPG", TestFixtureFactory.createJpgExtension());
		InitialSetup.EXTENSION_MAP.put("PNG", TestFixtureFactory.createPngExtension());
		InitialSetup.EXTENSION_MAP.put("WEBP", TestFixtureFactory.createWebpExtension());
	}

	// ===== 단일 이미지 업로드 테스트 =====

	@Test
	@DisplayName("정상 케이스: 단일 이미지 업로드 성공")
	void saveImage_validSingleFile_success() throws Exception {
		// given
		MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "content".getBytes());
		Map<String, String> result = Map.of("id", "image-123", "fileName", "test.jpg");

		when(imageSaveService.saveImage(any(MultipartFile.class), anyString(), anyString())).thenReturn(result);

		// when & then
		mockMvc.perform(multipart("/api/images")
						.file(file)
						.param("uploaderId", "user123")
						.param("category", "PROFILE")
						.contentType(MediaType.MULTIPART_FORM_DATA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value("image-123"))
				.andExpect(jsonPath("$.fileName").value("test.jpg"));

		verify(imageSaveService).saveImage(any(MultipartFile.class), eq("user123"), eq("PROFILE"));
	}

	@Test
	@DisplayName("uploaderId가 없으면 400 에러")
	void saveImage_missingUploaderId_badRequest() throws Exception {
		// given
		MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "content".getBytes());

		// when & then
		mockMvc.perform(multipart("/api/images")
						.file(file)
						.param("category", "PROFILE")
						.contentType(MediaType.MULTIPART_FORM_DATA))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("category가 없으면 400 에러")
	void saveImage_missingCategory_badRequest() throws Exception {
		// given
		MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "content".getBytes());

		// when & then
		mockMvc.perform(multipart("/api/images")
						.file(file)
						.param("uploaderId", "user123")
						.contentType(MediaType.MULTIPART_FORM_DATA))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("존재하지 않는 category는 400 에러")
	void saveImage_invalidCategory_badRequest() throws Exception {
		// given
		MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "content".getBytes());

		// when & then
		mockMvc.perform(multipart("/api/images")
						.file(file)
						.param("uploaderId", "user123")
						.param("category", "INVALID_CATEGORY")
						.contentType(MediaType.MULTIPART_FORM_DATA))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("파일이 비어있으면 400 에러")
	void saveImage_emptyFile_badRequest() throws Exception {
		// given
		MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[0]);

		// when & then
		mockMvc.perform(multipart("/api/images")
						.file(file)
						.param("uploaderId", "user123")
						.param("category", "PROFILE")
						.contentType(MediaType.MULTIPART_FORM_DATA))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("지원하지 않는 파일 확장자는 400 에러")
	void saveImage_unsupportedExtension_badRequest() throws Exception {
		// given
		MockMultipartFile file = new MockMultipartFile("file", "test.exe", "application/exe", "content".getBytes());

		// when & then
		mockMvc.perform(multipart("/api/images")
						.file(file)
						.param("uploaderId", "user123")
						.param("category", "PROFILE")
						.contentType(MediaType.MULTIPART_FORM_DATA))
				.andExpect(status().isBadRequest());
	}

	// ===== 다중 이미지 업로드 테스트 =====

	@Test
	@DisplayName("정상 케이스: 다중 이미지 업로드 성공")
	void saveImages_validMultipleFiles_success() throws Exception {
		// given
		MockMultipartFile file1 = new MockMultipartFile("files", "test1.jpg", "image/jpeg", "content1".getBytes());
		MockMultipartFile file2 = new MockMultipartFile("files", "test2.jpg", "image/jpeg", "content2".getBytes());
		Map<String, String> result = Map.of("id1", "test1.jpg", "id2", "test2.jpg");

		when(imageSaveService.saveImages(anyList(), anyString(), anyString())).thenReturn(result);

		// when & then
		mockMvc.perform(multipart("/api/images")
						.file(file1)
						.file(file2)
						.param("uploaderId", "user123")
						.param("category", "GALLERY")
						.contentType(MediaType.MULTIPART_FORM_DATA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id1").value("test1.jpg"))
				.andExpect(jsonPath("$.id2").value("test2.jpg"));

		verify(imageSaveService).saveImages(anyList(), eq("user123"), eq("GALLERY"));
	}

	// Note: MONO 타입 다중 이미지 검증은 ImageUploadRequestValidatorTest에서 테스트됨

	// Note: 단일/다중 동시 업로드 검증은 ImageUploadRequestValidatorTest에서 테스트됨

	// Note: 빈 파일 검증은 ImageFileValidatorTest에서 테스트됨

	@Test
	@DisplayName("다중 파일 중 하나라도 지원하지 않는 확장자면 400 에러")
	void saveImages_oneInvalidExtension_badRequest() throws Exception {
		// given
		MockMultipartFile file1 = new MockMultipartFile("files", "test1.jpg", "image/jpeg", "content".getBytes());
		MockMultipartFile file2 = new MockMultipartFile("files", "test2.exe", "application/exe", "content".getBytes());

		// when & then
		mockMvc.perform(multipart("/api/images")
						.file(file1)
						.file(file2)
						.param("uploaderId", "user123")
						.param("category", "GALLERY")
						.contentType(MediaType.MULTIPART_FORM_DATA))
				.andExpect(status().isBadRequest());
	}

	// ===== 엣지 케이스 테스트 =====

	@Test
	@DisplayName("파일이 전혀 없으면 400 에러")
	void saveImage_noFile_badRequest() throws Exception {
		// when & then
		mockMvc.perform(multipart("/api/images")
						.param("uploaderId", "user123")
						.param("category", "PROFILE")
						.contentType(MediaType.MULTIPART_FORM_DATA))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("uploaderId가 빈 문자열이면 400 에러")
	void saveImage_blankUploaderId_badRequest() throws Exception {
		// given
		MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "content".getBytes());

		// when & then
		mockMvc.perform(multipart("/api/images")
						.file(file)
						.param("uploaderId", "  ")
						.param("category", "PROFILE")
						.contentType(MediaType.MULTIPART_FORM_DATA))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("category가 빈 문자열이면 400 에러")
	void saveImage_blankCategory_badRequest() throws Exception {
		// given
		MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "content".getBytes());

		// when & then
		mockMvc.perform(multipart("/api/images")
						.file(file)
						.param("uploaderId", "user123")
						.param("category", "  ")
						.contentType(MediaType.MULTIPART_FORM_DATA))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("대소문자 혼합 category도 정상 처리")
	void saveImage_mixedCaseCategory_success() throws Exception {
		// given
		MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "content".getBytes());
		Map<String, String> result = Map.of("id", "image-123", "fileName", "test.jpg");

		when(imageSaveService.saveImage(any(MultipartFile.class), anyString(), anyString())).thenReturn(result);

		// when & then
		mockMvc.perform(multipart("/api/images")
						.file(file)
						.param("uploaderId", "user123")
						.param("category", "ProFile")  // 대소문자 혼합
						.contentType(MediaType.MULTIPART_FORM_DATA))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("여러 타입의 이미지 파일 업로드 성공")
	void saveImages_variousFileTypes_success() throws Exception {
		// given
		MockMultipartFile file1 = new MockMultipartFile("files", "test.jpg", "image/jpeg", "content".getBytes());
		MockMultipartFile file2 = new MockMultipartFile("files", "test.png", "image/png", "content".getBytes());
		MockMultipartFile file3 = new MockMultipartFile("files", "test.webp", "image/webp", "content".getBytes());
		Map<String, String> result = Map.of("id1", "test.jpg", "id2", "test.png", "id3", "test.webp");

		when(imageSaveService.saveImages(anyList(), anyString(), anyString())).thenReturn(result);

		// when & then
		mockMvc.perform(multipart("/api/images")
						.file(file1)
						.file(file2)
						.file(file3)
						.param("uploaderId", "user123")
						.param("category", "GALLERY")
						.contentType(MediaType.MULTIPART_FORM_DATA))
				.andExpect(status().isOk());
	}
}
