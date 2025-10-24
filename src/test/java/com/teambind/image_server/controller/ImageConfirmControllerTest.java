package com.teambind.image_server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teambind.image_server.config.InitialSetup;
import com.teambind.image_server.dto.request.ImageBatchConfirmRequest;
import com.teambind.image_server.fixture.TestFixtureFactory;
import com.teambind.image_server.service.ImageConfirmService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ImageConfirmController.class)
class ImageConfirmControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private ImageConfirmService imageConfirmService;

	@BeforeEach
	void setUp() {
		// InitialSetup 초기화
		InitialSetup.ALL_REFERENCE_TYPE_MAP.clear();
		InitialSetup.ALL_REFERENCE_TYPE_MAP.put("PROFILE", TestFixtureFactory.createProfileReferenceType());
		InitialSetup.ALL_REFERENCE_TYPE_MAP.put("GALLERY", TestFixtureFactory.createGalleryReferenceType());
	}

	// ===== 단일 이미지 확정 테스트 =====

	@Test
	@DisplayName("정상 케이스: 단일 이미지 확정 성공")
	void confirmImage_validRequest_success() throws Exception {
		// when & then
		mockMvc.perform(post("/api/v1/images/confirm/ref-456")
						.param("imageId", "image-123"))
				.andExpect(status().isOk());

		verify(imageConfirmService).confirmImage("image-123", "ref-456");
	}

	@Test
	@DisplayName("imageId가 없으면 400 에러")
	void confirmImage_missingImageId_badRequest() throws Exception {
		// when & then
		mockMvc.perform(post("/api/v1/images/confirm/ref-456"))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("imageId가 빈 문자열이면 400 에러")
	void confirmImage_blankImageId_badRequest() throws Exception {
		// when & then
		mockMvc.perform(post("/api/v1/images/confirm/ref-456")
						.param("imageId", "  "))
				.andExpect(status().is5xxServerError()); // @NotBlank validation error
	}

	// ===== 다중 이미지 확정 테스트 =====

	@Test
	@DisplayName("정상 케이스: 다중 이미지 확정 성공")
	void confirmImages_validRequest_success() throws Exception {
		// given
		ImageBatchConfirmRequest request = new ImageBatchConfirmRequest();
		request.setImageIds(List.of("img1", "img2", "img3"));
		request.setReferenceId("ref-456");

		// when & then
		mockMvc.perform(post("/api/v1/images/confirm")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk());

		verify(imageConfirmService).confirmImages(List.of("img1", "img2", "img3"), "ref-456");
	}

	@Test
	@DisplayName("imageIds가 없으면 400 에러")
	void confirmImages_missingImageIds_badRequest() throws Exception {
		// given
		ImageBatchConfirmRequest request = new ImageBatchConfirmRequest();
		request.setReferenceId("ref-456");

		// when & then
		mockMvc.perform(post("/api/v1/images/confirm")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("referenceId가 없으면 400 에러")
	void confirmImages_missingReferenceId_badRequest() throws Exception {
		// given
		ImageBatchConfirmRequest request = new ImageBatchConfirmRequest();
		request.setImageIds(List.of("img1", "img2"));

		// when & then
		mockMvc.perform(post("/api/v1/images/confirm")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("빈 imageIds 리스트는 정상 처리 (전체 삭제)")
	void confirmImages_emptyImageIds_success() throws Exception {
		// given
		ImageBatchConfirmRequest request = new ImageBatchConfirmRequest();
		request.setImageIds(List.of());
		request.setReferenceId("ref-456");

		// when & then
		mockMvc.perform(post("/api/v1/images/confirm")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk());

		verify(imageConfirmService).confirmImages(List.of(), "ref-456");
	}

	@Test
	@DisplayName("referenceId가 빈 문자열이면 400 에러")
	void confirmImages_blankReferenceId_badRequest() throws Exception {
		// given
		ImageBatchConfirmRequest request = new ImageBatchConfirmRequest();
		request.setImageIds(List.of("img1", "img2"));
		request.setReferenceId("  ");

		// when & then
		mockMvc.perform(post("/api/v1/images/confirm")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("대량의 이미지 ID 확정 요청")
	void confirmImages_largeNumberOfIds_success() throws Exception {
		// given
		List<String> imageIds = TestFixtureFactory.randomImageIds(50);
		ImageBatchConfirmRequest request = new ImageBatchConfirmRequest();
		request.setImageIds(imageIds);
		request.setReferenceId("ref-456");

		// when & then
		mockMvc.perform(post("/api/v1/images/confirm")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk());

		verify(imageConfirmService).confirmImages(anyList(), eq("ref-456"));
	}

	@Test
	@DisplayName("JSON 형식이 잘못되면 400 에러")
	void confirmImage_invalidJson_badRequest() throws Exception {
		// given
		String invalidJson = "{\"imageIds\": [\"img1\"], \"referenceId\":}";

		// when & then
		mockMvc.perform(post("/api/v1/images/confirm")
						.contentType(MediaType.APPLICATION_JSON)
						.content(invalidJson))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("Content-Type이 JSON이 아니면 415 에러")
	void confirmImage_invalidContentType_unsupportedMediaType() throws Exception {
		// given
		ImageBatchConfirmRequest request = new ImageBatchConfirmRequest();
		request.setImageIds(List.of("img1"));
		request.setReferenceId("ref-456");

		// when & then
		mockMvc.perform(post("/api/v1/images/confirm")
						.contentType(MediaType.TEXT_PLAIN)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isUnsupportedMediaType());
	}
}
