package com.teambind.image_server.dto;

import com.teambind.image_server.dto.request.ImageUploadRequest;
import com.teambind.image_server.fixture.TestFixtureFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class ImageUploadRequestTest {

	@Test
	@DisplayName("단일 업로드 확인 - file이 있고 비어있지 않음")
	void isSingleUpload_withFile_returnsTrue() {
		// given
		ImageUploadRequest request = ImageUploadRequest.builder()
				.file(TestFixtureFactory.createValidImageFile())
				.build();

		// when
		boolean result = request.isSingleUpload();

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("단일 업로드 확인 - file이 null")
	void isSingleUpload_nullFile_returnsFalse() {
		// given
		ImageUploadRequest request = ImageUploadRequest.builder()
				.file(null)
				.build();

		// when
		boolean result = request.isSingleUpload();

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("단일 업로드 확인 - file이 비어있음")
	void isSingleUpload_emptyFile_returnsFalse() {
		// given
		ImageUploadRequest request = ImageUploadRequest.builder()
				.file(TestFixtureFactory.createEmptyFile())
				.build();

		// when
		boolean result = request.isSingleUpload();

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("다중 업로드 확인 - files가 있고 비어있지 않음")
	void isMultiUpload_withFiles_returnsTrue() {
		// given
		ImageUploadRequest request = ImageUploadRequest.builder()
				.files(TestFixtureFactory.createValidImageFiles(3))
				.build();

		// when
		boolean result = request.isMultiUpload();

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("다중 업로드 확인 - files가 null")
	void isMultiUpload_nullFiles_returnsFalse() {
		// given
		ImageUploadRequest request = ImageUploadRequest.builder()
				.files(null)
				.build();

		// when
		boolean result = request.isMultiUpload();

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("다중 업로드 확인 - files가 빈 리스트")
	void isMultiUpload_emptyList_returnsFalse() {
		// given
		ImageUploadRequest request = ImageUploadRequest.builder()
				.files(new ArrayList<>())
				.build();

		// when
		boolean result = request.isMultiUpload();

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("단일과 다중 모두 false인 경우")
	void bothSingleAndMulti_false() {
		// given
		ImageUploadRequest request = ImageUploadRequest.builder()
				.build();

		// when & then
		assertThat(request.isSingleUpload()).isFalse();
		assertThat(request.isMultiUpload()).isFalse();
	}

	@Test
	@DisplayName("Builder 패턴으로 모든 필드 설정")
	void builder_allFields() {
		// when
		ImageUploadRequest request = ImageUploadRequest.builder()
				.file(TestFixtureFactory.createValidImageFile())
				.uploaderId("user123")
				.category("PROFILE")
				.build();

		// then
		assertThat(request.getFile()).isNotNull();
		assertThat(request.getUploaderId()).isEqualTo("user123");
		assertThat(request.getCategory()).isEqualTo("PROFILE");
	}

	@Test
	@DisplayName("Setter로 필드 변경 가능")
	void setter_modifiesFields() {
		// given
		ImageUploadRequest request = new ImageUploadRequest();

		// when
		request.setFile(TestFixtureFactory.createValidImageFile());
		request.setUploaderId("user456");
		request.setCategory("GALLERY");

		// then
		assertThat(request.getFile()).isNotNull();
		assertThat(request.getUploaderId()).isEqualTo("user456");
		assertThat(request.getCategory()).isEqualTo("GALLERY");
	}
}
