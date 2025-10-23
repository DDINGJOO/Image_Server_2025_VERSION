package com.teambind.image_server.entity;

import com.teambind.image_server.fixture.TestFixtureFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReferenceTypeTest {

	@Test
	@DisplayName("단일 이미지만 허용하는 타입 - allowsMultiple=false")
	void isSingleImageOnly_allowsMultipleFalse_returnsTrue() {
		// given
		ReferenceType referenceType = TestFixtureFactory.createMonoReferenceType("PROFILE");

		// when
		boolean result = referenceType.isSingleImageOnly();

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("다중 이미지 허용 타입 - allowsMultiple=true")
	void isSingleImageOnly_allowsMultipleTrue_returnsFalse() {
		// given
		ReferenceType referenceType = TestFixtureFactory.createMultiReferenceType("GALLERY");

		// when
		boolean result = referenceType.isSingleImageOnly();

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("maxImages=1이면 단일 이미지만 허용")
	void isSingleImageOnly_maxImagesOne_returnsTrue() {
		// given
		ReferenceType referenceType = ReferenceType.builder()
				.code("TEST")
				.name("Test")
				.allowsMultiple(true)
				.maxImages(1)
				.build();

		// when
		boolean result = referenceType.isSingleImageOnly();

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("다중 이미지 허용 여부 확인")
	void isMultipleImagesAllowed_allowsMultipleTrue_returnsTrue() {
		// given
		ReferenceType referenceType = TestFixtureFactory.createMultiReferenceType("GALLERY");

		// when
		boolean result = referenceType.isMultipleImagesAllowed();

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("이미지 개수 허용 여부 - 단일 타입에 1개")
	void isImageCountAllowed_singleType_one_returnsTrue() {
		// given
		ReferenceType referenceType = TestFixtureFactory.createMonoReferenceType("PROFILE");

		// when
		boolean result = referenceType.isImageCountAllowed(1);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("이미지 개수 허용 여부 - 단일 타입에 2개")
	void isImageCountAllowed_singleType_two_returnsFalse() {
		// given
		ReferenceType referenceType = TestFixtureFactory.createMonoReferenceType("PROFILE");

		// when
		boolean result = referenceType.isImageCountAllowed(2);

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("이미지 개수 허용 여부 - 다중 타입에 여러 개")
	void isImageCountAllowed_multiType_multiple_returnsTrue() {
		// given
		ReferenceType referenceType = TestFixtureFactory.createMultiReferenceType("GALLERY");

		// when
		boolean result = referenceType.isImageCountAllowed(5);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("이미지 개수 0은 허용되지 않음")
	void isImageCountAllowed_zero_returnsFalse() {
		// given
		ReferenceType referenceType = TestFixtureFactory.createMultiReferenceType("GALLERY");

		// when
		boolean result = referenceType.isImageCountAllowed(0);

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("이미지 개수 음수는 허용되지 않음")
	void isImageCountAllowed_negative_returnsFalse() {
		// given
		ReferenceType referenceType = TestFixtureFactory.createMultiReferenceType("GALLERY");

		// when
		boolean result = referenceType.isImageCountAllowed(-1);

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("maxImages 제한이 있을 때 초과하면 실패")
	void isImageCountAllowed_exceedsMaxImages_returnsFalse() {
		// given
		ReferenceType referenceType = ReferenceType.builder()
				.code("PRODUCT")
				.name("Product")
				.allowsMultiple(true)
				.maxImages(10)
				.build();

		// when
		boolean result = referenceType.isImageCountAllowed(11);

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("maxImages 제한 내에서는 허용")
	void isImageCountAllowed_withinMaxImages_returnsTrue() {
		// given
		ReferenceType referenceType = ReferenceType.builder()
				.code("PRODUCT")
				.name("Product")
				.allowsMultiple(true)
				.maxImages(10)
				.build();

		// when
		boolean result = referenceType.isImageCountAllowed(10);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("유효한 최대 이미지 개수 - 단일 타입")
	void getEffectiveMaxImages_singleType_returnsOne() {
		// given
		ReferenceType referenceType = TestFixtureFactory.createMonoReferenceType("PROFILE");

		// when
		int maxImages = referenceType.getEffectiveMaxImages();

		// then
		assertThat(maxImages).isEqualTo(1);
	}

	@Test
	@DisplayName("유효한 최대 이미지 개수 - 다중 타입 무제한")
	void getEffectiveMaxImages_multiType_returnsMaxValue() {
		// given
		ReferenceType referenceType = TestFixtureFactory.createMultiReferenceType("GALLERY");

		// when
		int maxImages = referenceType.getEffectiveMaxImages();

		// then
		assertThat(maxImages).isEqualTo(Integer.MAX_VALUE);
	}

	@Test
	@DisplayName("유효한 최대 이미지 개수 - maxImages 설정됨")
	void getEffectiveMaxImages_withMaxImages_returnsMaxImages() {
		// given
		ReferenceType referenceType = ReferenceType.builder()
				.code("PRODUCT")
				.name("Product")
				.allowsMultiple(true)
				.maxImages(10)
				.build();

		// when
		int maxImages = referenceType.getEffectiveMaxImages();

		// then
		assertThat(maxImages).isEqualTo(10);
	}

	@Test
	@DisplayName("검증 메시지 - 허용되면 null")
	void getValidationMessage_allowed_returnsNull() {
		// given
		ReferenceType referenceType = TestFixtureFactory.createMultiReferenceType("GALLERY");

		// when
		String message = referenceType.getValidationMessage(5);

		// then
		assertThat(message).isNull();
	}

	@Test
	@DisplayName("검증 메시지 - 단일 타입에 2개")
	void getValidationMessage_singleTypeExceeded_returnsMessage() {
		// given
		ReferenceType referenceType = TestFixtureFactory.createMonoReferenceType("PROFILE");

		// when
		String message = referenceType.getValidationMessage(2);

		// then
		assertThat(message).contains("1개만 설정");
		assertThat(message).contains("2개");
	}

	@Test
	@DisplayName("검증 메시지 - maxImages 초과")
	void getValidationMessage_exceedsMaxImages_returnsMessage() {
		// given
		ReferenceType referenceType = ReferenceType.builder()
				.code("PRODUCT")
				.name("Product")
				.allowsMultiple(true)
				.maxImages(10)
				.build();

		// when
		String message = referenceType.getValidationMessage(15);

		// then
		assertThat(message).contains("최대 10개");
		assertThat(message).contains("15개");
	}

	@Test
	@DisplayName("toString 메서드 테스트")
	void toString_containsAllFields() {
		// given
		ReferenceType referenceType = ReferenceType.builder()
				.code("PROFILE")
				.name("프로필")
				.allowsMultiple(false)
				.maxImages(1)
				.build();

		// when
		String result = referenceType.toString();

		// then
		assertThat(result).contains("PROFILE");
		assertThat(result).contains("프로필");
		assertThat(result).contains("false");
		assertThat(result).contains("1");
	}
}
