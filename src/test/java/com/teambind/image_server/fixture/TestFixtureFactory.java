package com.teambind.image_server.fixture;

import com.teambind.image_server.entity.Extension;
import com.teambind.image_server.entity.Image;
import com.teambind.image_server.entity.ReferenceType;
import com.teambind.image_server.entity.StorageObject;
import com.teambind.image_server.enums.ImageStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 테스트용 픽스처 팩토리
 * - 중복 코드 제거를 위한 테스트 데이터 생성 유틸리티
 */
public class TestFixtureFactory {

	// ===== MultipartFile 팩토리 =====

	public static MultipartFile createValidImageFile() {
		return createImageFile("test-image.jpg", "image/jpeg", "test image content".getBytes());
	}

	public static MultipartFile createValidPngFile() {
		return createImageFile("test-image.png", "image/png", "test image content".getBytes());
	}

	public static MultipartFile createValidWebpFile() {
		return createImageFile("test-image.webp", "image/webp", "test image content".getBytes());
	}

	public static MultipartFile createImageFile(String filename, String contentType, byte[] content) {
		return new MockMultipartFile("file", filename, contentType, content);
	}

	public static MultipartFile createEmptyFile() {
		return new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);
	}

	public static MultipartFile createFileWithoutExtension() {
		return new MockMultipartFile("file", "noextension", "image/jpeg", "content".getBytes());
	}

	public static MultipartFile createFileWithInvalidExtension() {
		return new MockMultipartFile("file", "test.exe", "application/exe", "content".getBytes());
	}

	public static MultipartFile createFileWithDoubleExtension() {
		return new MockMultipartFile("file", "test.php.jpg", "image/jpeg", "content".getBytes());
	}

	public static MultipartFile createFileWithPathTraversal() {
		return new MockMultipartFile("file", "../../etc/passwd.jpg", "image/jpeg", "content".getBytes());
	}

	public static MultipartFile createHiddenFile() {
		return new MockMultipartFile("file", ".hidden.jpg", "image/jpeg", "content".getBytes());
	}

	public static List<MultipartFile> createValidImageFiles(int count) {
		List<MultipartFile> files = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			files.add(createImageFile("test-image-" + i + ".jpg", "image/jpeg", ("content-" + i).getBytes()));
		}
		return files;
	}

	// ===== Extension 팩토리 =====

	public static Extension createExtension(String code) {
		return Extension.builder()
				.code(code.toUpperCase())
				.name(code + " format")
				.build();
	}

	public static Extension createJpgExtension() {
		return createExtension("JPG");
	}

	public static Extension createPngExtension() {
		return createExtension("PNG");
	}

	public static Extension createWebpExtension() {
		return createExtension("WEBP");
	}

	// ===== ReferenceType 팩토리 =====

	public static ReferenceType createReferenceType(String code, boolean allowsMultiple) {
		return ReferenceType.builder()
				.code(code.toUpperCase())
				.name(code)
				.description(code + " reference type")
				.allowsMultiple(allowsMultiple)
				.build();
	}

	public static ReferenceType createMonoReferenceType(String code) {
		return createReferenceType(code, false);
	}

	public static ReferenceType createMultiReferenceType(String code) {
		return createReferenceType(code, true);
	}

	public static ReferenceType createProfileReferenceType() {
		return createMonoReferenceType("PROFILE");
	}

	public static ReferenceType createGalleryReferenceType() {
		return createMultiReferenceType("GALLERY");
	}

	// ===== Image 팩토리 =====

	public static Image createImage() {
		return createImage(UUID.randomUUID().toString(), ImageStatus.TEMP, null, null);
	}

	public static Image createImage(String id) {
		return createImage(id, ImageStatus.TEMP, null, null);
	}

	public static Image createImage(ImageStatus status) {
		return createImage(UUID.randomUUID().toString(), status, null, null);
	}

	public static Image createImage(String id, ImageStatus status) {
		return createImage(id, status, null, null);
	}

	public static Image createImage(String id, ImageStatus status, String referenceId, ReferenceType referenceType) {
		return Image.builder()
				.id(id)
				.status(status)
				.referenceId(referenceId)
				.referenceType(referenceType)
				.imageUrl("https://example.com/images/" + id)
				.uploaderId("test-uploader")
				.createdAt(LocalDateTime.now())
				.isDeleted(false)
				.build();
	}

	public static Image createConfirmedImage(String id, String referenceId, ReferenceType referenceType) {
		return createImage(id, ImageStatus.CONFIRMED, referenceId, referenceType);
	}

	public static Image createTempImage(String id) {
		return createImage(id, ImageStatus.TEMP, null, null);
	}

	public static Image createDeletedImage(String id) {
		return createImage(id, ImageStatus.DELETED, null, null);
	}

	public static List<Image> createImages(int count) {
		List<Image> images = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			images.add(createImage("image-" + i));
		}
		return images;
	}

	public static List<Image> createConfirmedImages(int count, String referenceId, ReferenceType referenceType) {
		List<Image> images = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			images.add(createConfirmedImage("image-" + i, referenceId, referenceType));
		}
		return images;
	}

	// ===== StorageObject 팩토리 =====

	public static StorageObject createStorageObject(Image image, Extension originFormat, Extension convertedFormat) {
		return StorageObject.builder()
				.image(image)
				.originFormat(originFormat)
				.convertedFormat(convertedFormat)
				.originSize(1024L)
				.convertedSize(512L)
				.storageLocation("PROFILE/2025/01/01/" + image.getId() + ".webp")
				.build();
	}

	// ===== 편의 메서드 =====

	public static String randomImageId() {
		return UUID.randomUUID().toString();
	}

	public static List<String> randomImageIds(int count) {
		List<String> ids = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			ids.add(randomImageId());
		}
		return ids;
	}
}
