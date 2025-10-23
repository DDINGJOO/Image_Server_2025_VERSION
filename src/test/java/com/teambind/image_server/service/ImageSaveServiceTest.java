package com.teambind.image_server.service;

import com.teambind.image_server.config.InitialSetup;
import com.teambind.image_server.entity.Image;
import com.teambind.image_server.enums.ImageStatus;
import com.teambind.image_server.exception.CustomException;
import com.teambind.image_server.exception.ErrorCode;
import com.teambind.image_server.fixture.TestFixtureFactory;
import com.teambind.image_server.repository.ImageRepository;
import com.teambind.image_server.util.helper.ExtensionParser;
import com.teambind.image_server.util.helper.UrlHelper;
import com.teambind.image_server.util.store.LocalImageStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageSaveServiceTest {

	@Mock
	private UrlHelper urlHelper;

	@Mock
	private ImageRepository imageRepository;

	@Mock
	private LocalImageStorage imageStorage;

	@Mock
	private ExtensionParser extensionParser;

	@InjectMocks
	private ImageSaveService imageSaveService;

	@BeforeEach
	void setUp() {
		InitialSetup.ALL_REFERENCE_TYPE_MAP.clear();
		InitialSetup.EXTENSION_MAP.clear();

		InitialSetup.ALL_REFERENCE_TYPE_MAP.put("PROFILE", TestFixtureFactory.createProfileReferenceType());
		InitialSetup.ALL_REFERENCE_TYPE_MAP.put("GALLERY", TestFixtureFactory.createGalleryReferenceType());
		InitialSetup.EXTENSION_MAP.put("JPG", TestFixtureFactory.createJpgExtension());
		InitialSetup.EXTENSION_MAP.put("PNG", TestFixtureFactory.createPngExtension());
		InitialSetup.EXTENSION_MAP.put("WEBP", TestFixtureFactory.createWebpExtension());
	}

	@Test
	@DisplayName("Single image save with WebP conversion success")
	void saveImage_webpConversion_success() throws Exception {
		// given
		MultipartFile file = TestFixtureFactory.createValidImageFile();
		byte[] webpBytes = new byte[]{1, 2, 3, 4};

		when(extensionParser.extensionParse("test-image.jpg")).thenReturn("JPG");
		when(urlHelper.getUrl(anyString())).thenReturn("http://example.com/image.webp");
		when(imageRepository.save(any(Image.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(imageStorage.store(any(byte[].class), anyString())).thenReturn("stored-path");

		// Mock static ImageUtil.toWebp
		try (MockedStatic<com.teambind.image_server.util.convertor.ImageUtil> imageUtilMock =
				mockStatic(com.teambind.image_server.util.convertor.ImageUtil.class)) {

			imageUtilMock.when(() -> com.teambind.image_server.util.convertor.ImageUtil.toWebp(any(), anyFloat()))
					.thenReturn(webpBytes);

			// when
			Map<String, String> result = imageSaveService.saveImage(file, "user123", "PROFILE");

			// then
			assertThat(result).containsKeys("id", "fileName");
			assertThat(result.get("fileName")).isEqualTo("test-image.jpg");
			verify(imageRepository).save(any(Image.class));
			verify(imageStorage).store(eq(webpBytes), anyString());
		}
	}

	@Test
	@DisplayName("Image save with WebP conversion failure - fallback to original")
	void saveImage_webpFails_fallbackToOriginal() throws Exception {
		// given
		MultipartFile file = TestFixtureFactory.createValidImageFile();

		when(extensionParser.extensionParse("test-image.jpg")).thenReturn("JPG");
		when(urlHelper.getUrl(anyString())).thenReturn("http://example.com/image.jpg");
		when(imageRepository.save(any(Image.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(imageStorage.store(any(byte[].class), anyString())).thenReturn("stored-path");

		// Mock static ImageUtil.toWebp to throw Rosetta error
		try (MockedStatic<com.teambind.image_server.util.convertor.ImageUtil> imageUtilMock =
				mockStatic(com.teambind.image_server.util.convertor.ImageUtil.class)) {

			imageUtilMock.when(() -> com.teambind.image_server.util.convertor.ImageUtil.toWebp(any(), anyFloat()))
					.thenThrow(new RuntimeException("rosetta error: failed"));

			// when
			Map<String, String> result = imageSaveService.saveImage(file, "user123", "PROFILE");

			// then
			assertThat(result).containsKeys("id", "fileName");
			verify(imageRepository).save(any(Image.class));
			verify(imageStorage).store(any(byte[].class), anyString());
		}
	}

	@Test
	@DisplayName("Invalid file name throws exception")
	void saveImage_invalidFileName_throwsException() {
		// given
		MultipartFile file = TestFixtureFactory.createImageFile(null, "image/jpeg", "content".getBytes());

		// when & then
		assertThatThrownBy(() -> imageSaveService.saveImage(file, "user123", "PROFILE"))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorcode", ErrorCode.INVALID_FILE_NAME);
	}

	@Test
	@DisplayName("Blank file name throws exception")
	void saveImage_blankFileName_throwsException() {
		// given
		MultipartFile file = TestFixtureFactory.createImageFile("  ", "image/jpeg", "content".getBytes());

		// when & then
		assertThatThrownBy(() -> imageSaveService.saveImage(file, "user123", "PROFILE"))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorcode", ErrorCode.INVALID_FILE_NAME);
	}

	@Test
	@DisplayName("Multiple images save successfully")
	void saveImages_multipleFiles_success() throws Exception {
		// given
		List<MultipartFile> files = TestFixtureFactory.createValidImageFiles(3);

		when(extensionParser.extensionParse(anyString())).thenReturn("JPG");
		when(urlHelper.getUrl(anyString())).thenReturn("http://example.com/image.webp");
		when(imageRepository.save(any(Image.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(imageStorage.store(any(byte[].class), anyString())).thenReturn("stored-path");

		try (MockedStatic<com.teambind.image_server.util.convertor.ImageUtil> imageUtilMock =
				mockStatic(com.teambind.image_server.util.convertor.ImageUtil.class)) {

			imageUtilMock.when(() -> com.teambind.image_server.util.convertor.ImageUtil.toWebp(any(), anyFloat()))
					.thenReturn(new byte[]{1, 2, 3});

			// when
			Map<String, String> result = imageSaveService.saveImages(files, "user123", "GALLERY");

			// then
			assertThat(result).hasSize(3);
			verify(imageRepository, times(3)).save(any(Image.class));
			verify(imageStorage, times(3)).store(any(byte[].class), anyString());
		}
	}

	@Test
	@DisplayName("Empty file list returns empty result")
	void saveImages_emptyList_returnsEmpty() {
		// given
		List<MultipartFile> files = List.of();

		// when
		Map<String, String> result = imageSaveService.saveImages(files, "user123", "GALLERY");

		// then
		assertThat(result).isEmpty();
		verify(imageRepository, never()).save(any());
	}

	@Test
	@DisplayName("Image conversion error throws exception")
	void saveImage_conversionError_throwsException() {
		// given
		MultipartFile file = TestFixtureFactory.createValidImageFile();

		when(extensionParser.extensionParse("test-image.jpg")).thenReturn("JPG");

		try (MockedStatic<com.teambind.image_server.util.convertor.ImageUtil> imageUtilMock =
				mockStatic(com.teambind.image_server.util.convertor.ImageUtil.class)) {

			imageUtilMock.when(() -> com.teambind.image_server.util.convertor.ImageUtil.toWebp(any(), anyFloat()))
					.thenThrow(new RuntimeException("Corrupted image data"));

			// when & then
			assertThatThrownBy(() -> imageSaveService.saveImage(file, "user123", "PROFILE"))
					.isInstanceOf(CustomException.class)
					.hasFieldOrPropertyWithValue("errorcode", ErrorCode.IMAGE_SAVE_FAILED);
		}
	}

	@Test
	@DisplayName("Saved image has correct status and properties")
	void saveImage_imageProperties_correct() throws Exception {
		// given
		MultipartFile file = TestFixtureFactory.createValidImageFile();
		byte[] webpBytes = new byte[]{1, 2, 3, 4};

		when(extensionParser.extensionParse("test-image.jpg")).thenReturn("JPG");
		when(urlHelper.getUrl(anyString())).thenReturn("http://example.com/image.webp");
		when(imageStorage.store(any(byte[].class), anyString())).thenReturn("stored-path");

		when(imageRepository.save(any(Image.class))).thenAnswer(invocation -> {
			Image image = invocation.getArgument(0);
			assertThat(image.getStatus()).isEqualTo(ImageStatus.TEMP);
			assertThat(image.getUploaderId()).isEqualTo("user123");
			assertThat(image.getReferenceType()).isNotNull();
			assertThat(image.getCreatedAt()).isNotNull();
			assertThat(image.isDeleted()).isFalse();
			return image;
		});

		try (MockedStatic<com.teambind.image_server.util.convertor.ImageUtil> imageUtilMock =
				mockStatic(com.teambind.image_server.util.convertor.ImageUtil.class)) {

			imageUtilMock.when(() -> com.teambind.image_server.util.convertor.ImageUtil.toWebp(any(), anyFloat()))
					.thenReturn(webpBytes);

			// when
			imageSaveService.saveImage(file, "user123", "PROFILE");

			// then
			verify(imageRepository).save(any(Image.class));
		}
	}

	@Test
	@DisplayName("Different file extensions are handled correctly")
	void saveImage_variousExtensions_success() throws Exception {
		// given
		MultipartFile jpgFile = TestFixtureFactory.createImageFile("test.jpg", "image/jpeg", "content".getBytes());
		MultipartFile pngFile = TestFixtureFactory.createImageFile("test.png", "image/png", "content".getBytes());

		when(extensionParser.extensionParse("test.jpg")).thenReturn("JPG");
		when(extensionParser.extensionParse("test.png")).thenReturn("PNG");
		when(urlHelper.getUrl(anyString())).thenReturn("http://example.com/image.webp");
		when(imageRepository.save(any(Image.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(imageStorage.store(any(byte[].class), anyString())).thenReturn("stored-path");

		try (MockedStatic<com.teambind.image_server.util.convertor.ImageUtil> imageUtilMock =
				mockStatic(com.teambind.image_server.util.convertor.ImageUtil.class)) {

			imageUtilMock.when(() -> com.teambind.image_server.util.convertor.ImageUtil.toWebp(any(), anyFloat()))
					.thenReturn(new byte[]{1, 2, 3});

			// when
			Map<String, String> result1 = imageSaveService.saveImage(jpgFile, "user1", "PROFILE");
			Map<String, String> result2 = imageSaveService.saveImage(pngFile, "user2", "PROFILE");

			// then
			assertThat(result1.get("fileName")).isEqualTo("test.jpg");
			assertThat(result2.get("fileName")).isEqualTo("test.png");
			verify(imageRepository, times(2)).save(any(Image.class));
		}
	}
}
