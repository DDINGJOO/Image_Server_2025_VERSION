package com.teambind.image_server.event.events;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 다중 이미지 변경 이벤트 래퍼
 * <p>
 * 빈 배열 전송 시에도 referenceId를 포함하기 위한 래퍼 클래스입니다.
 * Consumer가 빈 배열을 받았을 때 어떤 referenceId의 이미지를 삭제해야 하는지 알 수 있도록 합니다.
 *
 * @author Image Server Team
 * @since 2.1
 */
@Data
@AllArgsConstructor
public class ImagesChangeEventWrapper {
	
	/**
	 * 참조 ID (상품 ID, 게시글 ID 등)
	 * 빈 배열인 경우에도 이 필드를 통해 어떤 대상의 이미지를 삭제할지 알 수 있습니다.
	 */
	private String referenceId;
	
	/**
	 * 이미지 변경 이벤트 리스트
	 * - 비어있지 않으면: 해당 이미지들로 전체 교체
	 * - 비어있으면: 전체 삭제
	 */
	private List<SequentialImageChangeEvent> images;
}
