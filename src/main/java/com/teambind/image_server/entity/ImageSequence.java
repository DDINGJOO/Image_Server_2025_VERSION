package com.teambind.image_server.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 이미지 순서 관리 엔티티
 * <p>
 * 특정 참조(상품, 게시글 등)에 연결된 이미지들의 순서를 관리합니다.
 *
 * @author Image Server Team
 * @since 2.0
 */
@Entity
@Table(
		name = "image_sequence",
		indexes = {
				@Index(name = "idx_reference_seq", columnList = "reference_id, seq_number"),
				@Index(name = "idx_image_id", columnList = "image_id")
		},
		uniqueConstraints = {
				@UniqueConstraint(name = "uk_reference_image", columnNames = {"reference_id", "image_id"}),
				@UniqueConstraint(name = "uk_reference_seq", columnNames = {"reference_id", "seq_number"})
		}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ImageSequence implements Comparable<ImageSequence> {
	
	/**
	 * 기본 키 (Auto Increment)
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;
	
	/**
	 * 참조 ID (상품 ID, 게시글 ID 등)
	 * 이미지가 속한 대상의 식별자
	 */
	@Column(name = "reference_id", nullable = false, length = 255)
	private String referenceId;
	
	/**
	 * 이미지 ID
	 */
	@Column(name = "image_id", nullable = false, length = 255)
	private String imageId;
	
	/**
	 * 이미지 엔티티 (Lazy Loading)
	 * fetch join으로 조회할 때 사용
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "image_id", insertable = false, updatable = false)
	private Image image;
	
	/**
	 * 순서 번호 (0부터 시작)
	 * 작은 번호가 먼저 표시됨
	 */
	@Column(name = "seq_number", nullable = false)
	private Integer seqNumber;
	
	/**
	 * 생성 시간
	 */
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;
	
	/**
	 * 수정 시간
	 */
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;
	
	// ==================== 생명주기 콜백 ====================
	
	/**
	 * ImageSequence 생성 (권장 방법)
	 *
	 * @param referenceId 참조 ID
	 * @param imageId     이미지 ID
	 * @param seqNumber   순서 번호 (0부터 시작)
	 * @return ImageSequence 인스턴스
	 */
	public static ImageSequence of(String referenceId, String imageId, Integer seqNumber) {
		return ImageSequence.builder()
				.referenceId(referenceId)
				.imageId(imageId)
				.seqNumber(seqNumber)
				.build();
	}
	
	/**
	 * Image 엔티티와 함께 ImageSequence 생성
	 *
	 * @param referenceId 참조 ID
	 * @param image       이미지 엔티티
	 * @param seqNumber   순서 번호
	 * @return ImageSequence 인스턴스
	 */
	public static ImageSequence of(String referenceId, Image image, Integer seqNumber) {
		return ImageSequence.builder()
				.referenceId(referenceId)
				.imageId(image.getId())
				.image(image)
				.seqNumber(seqNumber)
				.build();
	}
	
	// ==================== 정적 팩토리 메서드 ====================
	
	/**
	 * 첫 번째 순서로 ImageSequence 생성
	 *
	 * @param referenceId 참조 ID
	 * @param imageId     이미지 ID
	 * @return ImageSequence 인스턴스 (seqNumber = 0)
	 */
	public static ImageSequence createFirst(String referenceId, String imageId) {
		return of(referenceId, imageId, 0);
	}
	
	/**
	 * 엔티티 생성 시 자동으로 생성 시간 설정
	 */
	@PrePersist
	protected void onCreate() {
		this.createdAt = LocalDateTime.now();
		this.updatedAt = LocalDateTime.now();
		validateSequenceNumber();
	}
	
	/**
	 * 엔티티 수정 시 자동으로 수정 시간 갱신
	 */
	@PreUpdate
	protected void onUpdate() {
		this.updatedAt = LocalDateTime.now();
		validateSequenceNumber();
	}
	
	// ==================== 연관 관계 편의 메서드 ====================
	
	/**
	 * Image 엔티티 설정
	 * Lazy Loading된 Image를 명시적으로 설정할 때 사용
	 *
	 * @param image 이미지 엔티티
	 */
	public void setImage(Image image) {
		this.image = image;
		if (image != null && !image.getId().equals(this.imageId)) {
			this.imageId = image.getId();
		}
	}
	
	// ==================== 비즈니스 로직 메서드 ====================
	
	/**
	 * 첫 번째 순서인지 확인
	 *
	 * @return 첫 번째이면 true
	 */
	public boolean isFirst() {
		return seqNumber != null && seqNumber == 0;
	}
	
	/**
	 * 대표 이미지인지 확인 (첫 번째와 동일)
	 *
	 * @return 대표 이미지이면 true
	 */
	public boolean isThumbnail() {
		return isFirst();
	}
	
	/**
	 * 순서 번호 변경
	 * 유효성 검증 포함
	 *
	 * @param newSeqNumber 새 순서 번호
	 * @throws IllegalArgumentException 순서 번호가 음수인 경우
	 */
	public void updateSequenceNumber(Integer newSeqNumber) {
		if (newSeqNumber == null || newSeqNumber < 0) {
			throw new IllegalArgumentException("순서 번호는 0 이상이어야 합니다: " + newSeqNumber);
		}
		this.seqNumber = newSeqNumber;
	}
	
	/**
	 * 순서 번호 증가
	 */
	public void incrementSequence() {
		this.seqNumber++;
	}
	
	/**
	 * 순서 번호 감소
	 *
	 * @throws IllegalStateException 순서 번호가 0인 경우
	 */
	public void decrementSequence() {
		if (seqNumber == 0) {
			throw new IllegalStateException("순서 번호는 0보다 작을 수 없습니다");
		}
		this.seqNumber--;
	}
	
	/**
	 * 같은 referenceId에 속하는지 확인
	 *
	 * @param other 비교 대상
	 * @return 같은 그룹이면 true
	 */
	public boolean isSameReference(ImageSequence other) {
		if (other == null) {
			return false;
		}
		return Objects.equals(this.referenceId, other.referenceId);
	}
	
	/**
	 * 같은 이미지를 참조하는지 확인
	 *
	 * @param other 비교 대상
	 * @return 같은 이미지이면 true
	 */
	public boolean isSameImage(ImageSequence other) {
		if (other == null) {
			return false;
		}
		return Objects.equals(this.imageId, other.imageId);
	}
	
	// ==================== 검증 메서드 ====================
	
	/**
	 * 순서 번호 유효성 검증
	 *
	 * @throws IllegalStateException 순서 번호가 유효하지 않은 경우
	 */
	private void validateSequenceNumber() {
		if (seqNumber == null || seqNumber < 0) {
			throw new IllegalStateException(
					String.format("순서 번호는 0 이상이어야 합니다. referenceId=%s, imageId=%s, seqNumber=%s",
							referenceId, imageId, seqNumber));
		}
	}
	
	/**
	 * 필수 필드 검증
	 *
	 * @throws IllegalStateException 필수 필드가 null인 경우
	 */
	public void validate() {
		if (referenceId == null || referenceId.trim().isEmpty()) {
			throw new IllegalStateException("referenceId는 필수입니다");
		}
		if (imageId == null || imageId.trim().isEmpty()) {
			throw new IllegalStateException("imageId는 필수입니다");
		}
		validateSequenceNumber();
	}
	
	// ==================== Comparable 구현 ====================
	
	/**
	 * 순서 번호 기준으로 비교
	 * 같은 referenceId 내에서만 의미 있음
	 *
	 * @param other 비교 대상
	 * @return 비교 결과 (음수, 0, 양수)
	 */
	@Override
	public int compareTo(ImageSequence other) {
		if (other == null) {
			return 1;
		}
		
		// 같은 referenceId인 경우 순서 번호로 비교
		if (this.isSameReference(other)) {
			return Integer.compare(this.seqNumber, other.seqNumber);
		}
		
		// 다른 referenceId인 경우 referenceId로 비교
		return this.referenceId.compareTo(other.referenceId);
	}
	
	// ==================== equals & hashCode ====================
	
	/**
	 * 동등성 비교 (id 기반)
	 * id가 null인 경우(persist 전) 모든 필드로 비교
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		
		ImageSequence that = (ImageSequence) o;
		
		// id가 있으면 id로만 비교
		if (id != null && that.id != null) {
			return Objects.equals(id, that.id);
		}
		
		// id가 없으면 비즈니스 키로 비교 (referenceId + imageId는 유니크)
		return Objects.equals(referenceId, that.referenceId) &&
				Objects.equals(imageId, that.imageId);
	}
	
	/**
	 * 해시코드 생성 (id 기반)
	 * id가 null인 경우 비즈니스 키로 생성
	 */
	@Override
	public int hashCode() {
		// id가 있으면 id로만 해시
		if (id != null) {
			return Objects.hash(id);
		}
		// id가 없으면 비즈니스 키로 해시 (referenceId + imageId는 유니크)
		return Objects.hash(referenceId, imageId);
	}
	
	// ==================== toString ====================
	
	/**
	 * 디버깅용 문자열 표현
	 */
	@Override
	public String toString() {
		return String.format("ImageSequence{id=%d, referenceId='%s', imageId='%s', seqNumber=%d}",
				id, referenceId, imageId, seqNumber);
	}
	
	/**
	 * 상세 정보 포함 문자열 표현
	 */
	public String toDetailString() {
		return String.format("ImageSequence{id=%d, referenceId='%s', imageId='%s', seqNumber=%d, " +
						"isFirst=%b, createdAt=%s, updatedAt=%s}",
				id, referenceId, imageId, seqNumber, isFirst(), createdAt, updatedAt);
	}
}
