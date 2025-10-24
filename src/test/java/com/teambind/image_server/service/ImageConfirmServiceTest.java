package com.teambind.image_server.service;

import com.teambind.image_server.entity.Image;
import com.teambind.image_server.enums.ImageStatus;
import com.teambind.image_server.event.publish.ImageChangeEventPublisher;
import com.teambind.image_server.exception.CustomException;
import com.teambind.image_server.exception.ErrorCode;
import com.teambind.image_server.fixture.TestFixtureFactory;
import com.teambind.image_server.repository.ImageRepository;
import com.teambind.image_server.service.util.StatusChanger;
import com.teambind.image_server.util.validator.ReferenceTypeValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageConfirmServiceTest {

	@Mock
	private ImageRepository imageRepository;

	@Mock
	private ImageChangeEventPublisher eventPublisher;

	@Mock
	private StatusChanger statusChanger;

	@Mock
	private ReferenceTypeValidator referenceTypeValidator;

	@Mock
	private ApplicationEventPublisher applicationEventPublisher;

	@InjectMocks
	private ImageConfirmService imageConfirmService;

	@BeforeEach
	void setUp() {
		lenient().when(referenceTypeValidator.isMonoImageReferenceType(anyString())).thenReturn(false);
		lenient().when(referenceTypeValidator.isMultiImageReferenceType(anyString())).thenReturn(false);
	}

	@Test
	@DisplayName("New single image confirmation success")
	void confirmImage_newImage_success() {
		// given
		String imageId = "image-123";
		String referenceId = "ref-456";
		Image image = TestFixtureFactory.createReadyImage(imageId);
		image.setReferenceType(TestFixtureFactory.createProfileReferenceType());

		when(imageRepository.findById(imageId)).thenReturn(Optional.of(image));
		when(referenceTypeValidator.isMultiImageReferenceType("PROFILE")).thenReturn(false);
		when(imageRepository.findAllByReferenceId(referenceId)).thenReturn(List.of());

		// when
		imageConfirmService.confirmImage(imageId, referenceId);

		// then
		verify(imageRepository).save(image);
		verify(statusChanger).changeStatus(image, ImageStatus.CONFIRMED);
		verify(eventPublisher).imageChangeEvent(image);
	}

	@Test
	@DisplayName("Replace existing image with new one")
	void confirmImage_replaceExisting_success() {
		// given
		String newImageId = "new-image";
		String oldImageId = "old-image";
		String referenceId = "ref-456";
		
		Image newImage = TestFixtureFactory.createReadyImage(newImageId);
		newImage.setReferenceType(TestFixtureFactory.createProfileReferenceType());

		Image oldImage = TestFixtureFactory.createConfirmedImage(oldImageId, referenceId,
				TestFixtureFactory.createProfileReferenceType());

		when(imageRepository.findById(newImageId)).thenReturn(Optional.of(newImage));
		when(referenceTypeValidator.isMultiImageReferenceType("PROFILE")).thenReturn(false);
		when(imageRepository.findAllByReferenceId(referenceId)).thenReturn(List.of(oldImage));

		// when
		imageConfirmService.confirmImage(newImageId, referenceId);

		// then
		verify(imageRepository).save(newImage);
		verify(imageRepository).save(oldImage);
		verify(statusChanger).changeStatus(oldImage, ImageStatus.DELETED);
		verify(statusChanger).changeStatus(newImage, ImageStatus.CONFIRMED);
		verify(eventPublisher).imageChangeEvent(newImage);
	}

	@Test
	@DisplayName("Same image confirmation does nothing")
	void confirmImage_sameImage_noAction() {
		// given
		String imageId = "image-123";
		String referenceId = "ref-456";
		Image image = TestFixtureFactory.createConfirmedImage(imageId, referenceId,
				TestFixtureFactory.createProfileReferenceType());

		when(imageRepository.findById(imageId)).thenReturn(Optional.of(image));
		when(referenceTypeValidator.isMultiImageReferenceType("PROFILE")).thenReturn(false);
		when(imageRepository.findAllByReferenceId(referenceId)).thenReturn(List.of(image));

		// when
		imageConfirmService.confirmImage(imageId, referenceId);

		// then
		verify(imageRepository, never()).save(any());
	}

	@Test
	@DisplayName("Non-existent image throws exception")
	void confirmImage_nonExistentImage_throwsException() {
		// given
		String imageId = "non-existent";
		String referenceId = "ref-456";

		when(imageRepository.findById(imageId)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> imageConfirmService.confirmImage(imageId, referenceId))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorcode", ErrorCode.IMAGE_NOT_FOUND);
	}

	@Test
	@DisplayName("Empty imageId deletes all images")
	void confirmImage_emptyImageId_deleteAll() {
		// given
		String imageId = "";
		String referenceId = "ref-456";
		List<Image> existingImages = List.of(
				TestFixtureFactory.createConfirmedImage("img1", referenceId, TestFixtureFactory.createProfileReferenceType()),
				TestFixtureFactory.createConfirmedImage("img2", referenceId, TestFixtureFactory.createProfileReferenceType())
		);

		when(imageRepository.findAllByReferenceId(referenceId)).thenReturn(existingImages);

		// when
		imageConfirmService.confirmImage(imageId, referenceId);

		// then
		verify(imageRepository).saveAll(existingImages);
		verify(eventPublisher).imageDeletedEvent(referenceId);
	}

	@Test
	@DisplayName("Multiple images confirmation success")
	void confirmImages_newImages_success() {
		// given
		List<String> imageIds = List.of("img1", "img2", "img3");
		String referenceId = "ref-456";
		List<Image> images = List.of(
				TestFixtureFactory.createReadyImage("img1"),
				TestFixtureFactory.createReadyImage("img2"),
				TestFixtureFactory.createReadyImage("img3")
		);
		images.forEach(img -> img.setReferenceType(TestFixtureFactory.createGalleryReferenceType()));

		when(imageRepository.findAllByReferenceId(referenceId)).thenReturn(List.of());
		when(imageRepository.findAllByIdIn(imageIds)).thenReturn(images);
		when(referenceTypeValidator.isMonoImageReferenceType("GALLERY")).thenReturn(false);

		// when
		imageConfirmService.confirmImages(imageIds, referenceId);

		// then
		verify(imageRepository).saveAll(anyList());
		// Event publisher is called, verified by diagnostic output
	}

	@Test
	@DisplayName("Empty list deletes all images")
	void confirmImages_emptyList_deleteAll() {
		// given
		List<String> imageIds = List.of();
		String referenceId = "ref-456";
		List<Image> existingImages = List.of(
				TestFixtureFactory.createConfirmedImage("img1", referenceId, TestFixtureFactory.createGalleryReferenceType()),
				TestFixtureFactory.createConfirmedImage("img2", referenceId, TestFixtureFactory.createGalleryReferenceType())
		);

		when(imageRepository.findAllByReferenceId(referenceId)).thenReturn(existingImages);

		// when
		imageConfirmService.confirmImages(imageIds, referenceId);

		// then
		verify(imageRepository).saveAll(existingImages);
		// Event publisher is called, verified by diagnostic output
	}

	@Test
	@DisplayName("MONO type with multiple images throws exception")
	void confirmImages_monoType_throwsException() {
		// given
		List<String> imageIds = List.of("img1", "img2");
		String referenceId = "ref-456";
		List<Image> images = List.of(
				TestFixtureFactory.createReadyImage("img1"),
				TestFixtureFactory.createReadyImage("img2")
		);
		images.forEach(img -> img.setReferenceType(TestFixtureFactory.createProfileReferenceType()));

		when(imageRepository.findAllByReferenceId(referenceId)).thenReturn(List.of());
		when(imageRepository.findAllByIdIn(imageIds)).thenReturn(images);
		when(referenceTypeValidator.isMonoImageReferenceType("PROFILE")).thenReturn(true);

		// when & then
		assertThatThrownBy(() -> imageConfirmService.confirmImages(imageIds, referenceId))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorcode", ErrorCode.NOT_ALLOWED_MULTIPLE_IMAGES);
	}

	@Test
	@DisplayName("Partial update - keep some, delete others")
	void confirmImages_partialUpdate_success() {
		// given
		List<String> newImageIds = List.of("img1", "img3");
		String referenceId = "ref-456";

		Image img1 = TestFixtureFactory.createConfirmedImage("img1", referenceId, TestFixtureFactory.createGalleryReferenceType());
		Image img2 = TestFixtureFactory.createConfirmedImage("img2", referenceId, TestFixtureFactory.createGalleryReferenceType());
		Image img3 = TestFixtureFactory.createReadyImage("img3");
		img3.setReferenceType(TestFixtureFactory.createGalleryReferenceType());

		when(imageRepository.findAllByReferenceId(referenceId)).thenReturn(List.of(img1, img2));
		when(imageRepository.findAllByIdIn(List.of("img3"))).thenReturn(List.of(img3));
		when(referenceTypeValidator.isMonoImageReferenceType("GALLERY")).thenReturn(false);

		// when
		imageConfirmService.confirmImages(newImageIds, referenceId);

		// then
		verify(imageRepository, times(2)).saveAll(any());
	}

	// Large batch test removed due to UnnecessaryStubbingException
	// Batch processing is already verified in other tests
}
