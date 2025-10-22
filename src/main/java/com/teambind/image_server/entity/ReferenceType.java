package com.teambind.image_server.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 참조 타입 엔티티
 * <p>
 * 이미지가 연결될 수 있는 대상 타입을 정의합니다.
 * (예: PRODUCT, USER, POST, PROFILE 등)
 *
 * @author Image Server Team
 * @since 1.0
 */
@Entity
@Table(name = "reference_types", uniqueConstraints = @UniqueConstraint(name = "uk_reference_type_code", columnNames = "code"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferenceType {
	
	/**
	 * 기본 키
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	
	/**
	 * 참조 타입 코드 (대문자, 유니크)
	 * 예: PRODUCT, USER, POST, PROFILE, BANNER, CATEGORY
	 */
	@Column(name = "code", nullable = false, length = 32)
	private String code;
	
	/**
	 * 참조 타입 이름 (표시용)
	 * 예: 상품, 사용자, 게시글, 프로필
	 */
	@Column(name = "name", nullable = false, length = 64)
	private String name;
	
	/**
	 * 다중 이미지 허용 여부
	 * - true: 여러 개의 이미지 허용 (PRODUCT, POST 등)
	 * - false: 단일 이미지만 허용 (PROFILE 등)
	 */
	@Column(name = "allows_multiple", nullable = false)
	private Boolean allowsMultiple;
	
	/**
	 * 최대 이미지 개수
	 * - null: 무제한
	 * - 숫자: 최대 개수 제한
	 * 예: PROFILE은 1, PRODUCT는 10 등
	 */
	@Column(name = "max_images")
	private Integer maxImages;
	
	/**
	 * 설명 (선택사항)
	 */
	@Column(name = "description", length = 255)
	private String description;
	
	// ==================== 비즈니스 로직 메서드 ====================
	
	/**
	 * 단일 이미지만 허용하는지 확인
	 *
	 * @return 단일 이미지만 허용하면 true
	 */
	public boolean isSingleImageOnly() {
		return Boolean.FALSE.equals(allowsMultiple) ||
				(maxImages != null && maxImages == 1);
	}
	
	/**
	 * 다중 이미지를 허용하는지 확인
	 *
	 * @return 다중 이미지를 허용하면 true
	 */
	public boolean isMultipleImagesAllowed() {
		return Boolean.TRUE.equals(allowsMultiple);
	}
	
	/**
	 * 특정 개수의 이미지가 허용되는지 검증
	 *
	 * @param count 이미지 개수
	 * @return 허용되면 true
	 */
	public boolean isImageCountAllowed(int count) {
		if (count <= 0) {
			return false;
		}
		
		// 단일 이미지만 허용하는 경우
		if (isSingleImageOnly() && count > 1) {
			return false;
		}
		
		// 최대 개수 제한이 있는 경우
		if (maxImages != null && count > maxImages) {
			return false;
		}
		
		return true;
	}
	
	/**
	 * 최대 이미지 개수 반환
	 *
	 * @return 최대 개수 (무제한이면 Integer.MAX_VALUE)
	 */
	public int getEffectiveMaxImages() {
		if (maxImages != null) {
			return maxImages;
		}
		return isSingleImageOnly() ? 1 : Integer.MAX_VALUE;
	}
	
	/**
	 * 검증 메시지 생성
	 *
	 * @param actualCount 실제 이미지 개수
	 * @return 검증 실패 메시지 (허용되면 null)
	 */
	public String getValidationMessage(int actualCount) {
		if (isImageCountAllowed(actualCount)) {
			return null;
		}
		
		if (isSingleImageOnly()) {
			return String.format("%s는 이미지를 1개만 설정할 수 있습니다. (현재: %d개)",
					name, actualCount);
		}
		
		if (maxImages != null) {
			return String.format("%s는 최대 %d개의 이미지만 설정할 수 있습니다. (현재: %d개)",
					name, maxImages, actualCount);
		}
		
		return "이미지 개수가 올바르지 않습니다.";
	}
	
	// ==================== toString ====================
	
	@Override
	public String toString() {
		return String.format("ReferenceType{code='%s', name='%s', allowsMultiple=%s, maxImages=%s}",
				code, name, allowsMultiple, maxImages);
	}
}
