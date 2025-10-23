package com.teambind.image_server.repository;

import com.teambind.image_server.entity.Image;
import com.teambind.image_server.entity.ReferenceType;
import com.teambind.image_server.enums.ImageStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ImageRepositoryTest {

	@Autowired
	private ImageRepository imageRepository;

	@Autowired
	private TestEntityManager entityManager;

	private ReferenceType profileType;
	private ReferenceType galleryType;

	@BeforeEach
	void setUp() {
		profileType = ReferenceType.builder()
				.code("PROFILE")
				.name("Profile")
				.allowsMultiple(false)
				.maxImages(1)
				.build();
		entityManager.persist(profileType);

		galleryType = ReferenceType.builder()
				.code("GALLERY")
				.name("Gallery")
				.allowsMultiple(true)
				.maxImages(null)
				.build();
		entityManager.persist(galleryType);

		entityManager.flush();
	}

	@Test
	@DisplayName("Save and find image by ID")
	void saveAndFindById() {
		// given
		Image image = Image.builder()
				.id("test-image-1")
				.imageUrl("http://example.com/image1.jpg")
				.uploaderId("user123")
				.status(ImageStatus.TEMP)
				.referenceType(profileType)
				.createdAt(LocalDateTime.now())
				.isDeleted(false)
				.build();

		// when
		Image saved = imageRepository.save(image);
		entityManager.flush();
		entityManager.clear();

		// then
		Image found = imageRepository.findById("test-image-1").orElse(null);
		assertThat(found).isNotNull();
		assertThat(found.getId()).isEqualTo("test-image-1");
		assertThat(found.getUploaderId()).isEqualTo("user123");
		assertThat(found.getStatus()).isEqualTo(ImageStatus.TEMP);
	}

	@Test
	@DisplayName("Find by imageId, uploaderId and referenceType")
	void findByIdAndUploaderIdAndReferenceType() {
		// given
		Image image = Image.builder()
				.id("test-image-2")
				.imageUrl("http://example.com/image2.jpg")
				.uploaderId("user456")
				.status(ImageStatus.CONFIRMED)
				.referenceType(profileType)
				.createdAt(LocalDateTime.now())
				.isDeleted(false)
				.build();
		entityManager.persist(image);
		entityManager.flush();
		entityManager.clear();

		// when
		Image found = imageRepository.findByIdAndUploaderIdAndReferenceType(
				"test-image-2", "user456", profileType);

		// then
		assertThat(found).isNotNull();
		assertThat(found.getId()).isEqualTo("test-image-2");
		assertThat(found.getUploaderId()).isEqualTo("user456");
	}

	@Test
	@DisplayName("Find all images excluding specific status")
	void findAllByStatusNot() {
		// given
		Image tempImage = createImage("temp-1", ImageStatus.TEMP);
		Image confirmedImage = createImage("confirmed-1", ImageStatus.CONFIRMED);
		Image deletedImage = createImage("deleted-1", ImageStatus.DELETED);

		entityManager.persist(tempImage);
		entityManager.persist(confirmedImage);
		entityManager.persist(deletedImage);
		entityManager.flush();
		entityManager.clear();

		// when
		List<Image> notDeletedImages = imageRepository.findAllByStatusNot(ImageStatus.DELETED);

		// then
		assertThat(notDeletedImages).hasSize(2);
		assertThat(notDeletedImages).extracting(Image::getId)
				.containsExactlyInAnyOrder("temp-1", "confirmed-1");
	}

	@Test
	@DisplayName("Find all images by referenceId")
	void findAllByReferenceId() {
		// given
		Image image1 = createImageWithReferenceId("img-1", "ref-123");
		Image image2 = createImageWithReferenceId("img-2", "ref-123");
		Image image3 = createImageWithReferenceId("img-3", "ref-456");

		entityManager.persist(image1);
		entityManager.persist(image2);
		entityManager.persist(image3);
		entityManager.flush();
		entityManager.clear();

		// when
		List<Image> images = imageRepository.findAllByReferenceId("ref-123");

		// then
		assertThat(images).hasSize(2);
		assertThat(images).extracting(Image::getId)
				.containsExactlyInAnyOrder("img-1", "img-2");
	}

	@Test
	@DisplayName("Find all images by multiple IDs")
	void findAllByIdIn() {
		// given
		Image image1 = createImage("img-1", ImageStatus.TEMP);
		Image image2 = createImage("img-2", ImageStatus.TEMP);
		Image image3 = createImage("img-3", ImageStatus.TEMP);

		entityManager.persist(image1);
		entityManager.persist(image2);
		entityManager.persist(image3);
		entityManager.flush();
		entityManager.clear();

		// when
		List<Image> images = imageRepository.findAllByIdIn(List.of("img-1", "img-3"));

		// then
		assertThat(images).hasSize(2);
		assertThat(images).extracting(Image::getId)
				.containsExactlyInAnyOrder("img-1", "img-3");
	}

	@Test
	@DisplayName("Delete all images by referenceId")
	void deleteAllByReferenceId() {
		// given
		Image image1 = createImageWithReferenceId("img-1", "ref-789");
		Image image2 = createImageWithReferenceId("img-2", "ref-789");
		Image image3 = createImageWithReferenceId("img-3", "ref-999");

		entityManager.persist(image1);
		entityManager.persist(image2);
		entityManager.persist(image3);
		entityManager.flush();
		entityManager.clear();

		// when
		imageRepository.deleteAllByReferenceId("ref-789");
		entityManager.flush();
		entityManager.clear();

		// then
		List<Image> remaining = imageRepository.findAll();
		assertThat(remaining).hasSize(1);
		assertThat(remaining.get(0).getId()).isEqualTo("img-3");
	}

	@Test
	@DisplayName("Find by empty list returns empty result")
	void findAllByIdIn_emptyList() {
		// when
		List<Image> images = imageRepository.findAllByIdIn(List.of());

		// then
		assertThat(images).isEmpty();
	}

	@Test
	@DisplayName("Find by non-existent referenceId returns empty result")
	void findAllByReferenceId_nonExistent() {
		// when
		List<Image> images = imageRepository.findAllByReferenceId("non-existent");

		// then
		assertThat(images).isEmpty();
	}

	private Image createImage(String id, ImageStatus status) {
		return Image.builder()
				.id(id)
				.imageUrl("http://example.com/" + id + ".jpg")
				.uploaderId("user123")
				.status(status)
				.referenceType(profileType)
				.createdAt(LocalDateTime.now())
				.isDeleted(false)
				.build();
	}

	private Image createImageWithReferenceId(String id, String referenceId) {
		return Image.builder()
				.id(id)
				.imageUrl("http://example.com/" + id + ".jpg")
				.uploaderId("user123")
				.status(ImageStatus.CONFIRMED)
				.referenceId(referenceId)
				.referenceType(galleryType)
				.createdAt(LocalDateTime.now())
				.isDeleted(false)
				.build();
	}
}
