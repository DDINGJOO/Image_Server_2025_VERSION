package com.teambind.image_server.event.handler;


import com.teambind.image_server.entity.ImageSequence;
import com.teambind.image_server.event.events.ImagesConfirmedEvent;
import com.teambind.image_server.event.publish.ImageChangeEventPublisher;
import com.teambind.image_server.service.ImageSequenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * 이미지 시퀀스 관련 이벤트 핸들러
 * <p>
 * 이미지 확정 이벤트를 받아서 ImageSequence를 생성하고 외부 이벤트를 발행합니다.
 * TransactionalEventListener를 사용하여 트랜잭션 내에서 안전하게 처리됩니다.
 *
 * @author Image Server Team
 * @since 2.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ImageSequenceEventHandler {
	
	private final ImageSequenceService imageSequenceService;
	private final ImageChangeEventPublisher eventPublisher;
	
	/**
	 * 이미지 확정 이벤트 핸들러
	 * <p>
	 * 트랜잭션 커밋 후에 실행되며, 다음 단계로 진행됩니다:
	 * 1. ImageSequence 재생성 (기존 삭제 + 새로 생성)
	 * 2. 외부 시스템에 이미지 변경 이벤트 발행
	 * <p>
	 * AFTER_COMMIT을 사용하여 DB 커밋이 성공한 후에만 Kafka 발행을 수행합니다.
	 * 이를 통해 DB 저장의 안전성을 보장하고, Kafka 발행 실패가 DB 트랜잭션에 영향을 주지 않도록 합니다.
	 * Kafka 발행 실패 시에도 DB는 안전하게 저장됩니다.
	 *
	 * @param event 이미지 확정 이벤트
	 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleImagesConfirmed(ImagesConfirmedEvent event) {
		log.info("Handling ImagesConfirmedEvent: referenceId={}, imageCount={}",
				event.getReferenceId(), event.getImageCount());

		try {
			// 1. 이미지가 없는 경우 (전체 삭제)
			if (event.isEmpty()) {
				log.info("No images in event, deleting all sequences for referenceId: {}",
						event.getReferenceId());
				imageSequenceService.deleteSequences(event.getReferenceId());
				
				// 전체 삭제 이벤트 발행 (빈 배열)
				if (event.getReferenceTypeCode() != null) {
					eventPublisher.imagesDeletedEvent(event.getReferenceId(), event.getReferenceTypeCode());
					log.info("Published empty array deletion event for referenceId: {}", event.getReferenceId());
				}
				return;
			}
			
			// 2. ImageSequence 재생성 (기존 삭제 + 새로 생성)
			List<ImageSequence> sequences = imageSequenceService.recreateSequences(
					event.getReferenceId(),
					event.getConfirmedImages()
			);
			
			log.info("Created {} image sequences for referenceId: {}",
					sequences.size(), event.getReferenceId());
			
			// 3. 외부 이벤트 발행 (Kafka, RabbitMQ 등)
			if (!sequences.isEmpty()) {
				eventPublisher.imagesChangeEvent(sequences);
				log.info("Published image change event for referenceId: {}",
						event.getReferenceId());
			}
			
		} catch (Exception e) {
			log.error("Failed to handle ImagesConfirmedEvent for referenceId: {}",
					event.getReferenceId(), e);
			throw e; // 예외를 다시 던져서 트랜잭션 롤백
		}
	}
}
