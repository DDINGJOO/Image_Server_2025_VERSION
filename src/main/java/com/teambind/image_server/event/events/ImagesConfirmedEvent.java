package com.teambind.image_server.event.events;


import com.teambind.image_server.entity.Image;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * 이미지 확정 완료 도메인 이벤트
 * <p>
 * 이미지들이 확정 처리되었을 때 발행되는 이벤트입니다.
 * 이 이벤트를 통해 ImageSequence 생성 및 외부 이벤트 발행이 트리거됩니다.
 *
 * @author Image Server Team
 * @since 2.0
 */
@Getter
@AllArgsConstructor
public class ImagesConfirmedEvent {
	
	/**
	 * 참조 ID (상품 ID, 게시글 ID 등)
	 */
	private final String referenceId;
	
	/**
	 * 확정된 이미지 리스트 (순서대로 정렬됨)
	 */
	private final List<Image> confirmedImages;
	
	/**
	 * 확정된 이미지 개수
	 */
	public int getImageCount() {
		return confirmedImages != null ? confirmedImages.size() : 0;
	}
	
	/**
	 * 이미지가 없는지 확인
	 */
	public boolean isEmpty() {
		return confirmedImages == null || confirmedImages.isEmpty();
	}
}
