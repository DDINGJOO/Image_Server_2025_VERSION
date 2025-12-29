package com.teambind.image_server.service;


import com.teambind.image_server.entity.Image;
import com.teambind.image_server.entity.ImageSequence;
import com.teambind.image_server.repository.ImageSequenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 이미지 시퀀스 관리 서비스
 * <p>
 * ImageSequence 엔티티의 생성, 수정, 삭제를 담당합니다.
 *
 * @author Image Server Team
 * @since 2.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImageSequenceService {
	
	private final ImageSequenceRepository imageSequenceRepository;
	
	/**
	 * 기존 시퀀스를 삭제하고 새로운 시퀀스를 생성합니다.
	 * <p>
	 * REQUIRES_NEW로 새로운 트랜잭션을 시작하여 실행됩니다.
	 * 이는 TransactionalEventListener의 AFTER_COMMIT 단계에서도 정상 동작하도록 합니다.
	 * <p>
	 * 트랜잭션 내에서 실행되며, 다음 단계로 진행됩니다:
	 * 1. 기존 referenceId의 모든 시퀀스 삭제
	 * 2. 새로운 이미지 리스트로 시퀀스 생성 (순서는 리스트 순서 그대로)
	 * 3. 데이터베이스에 저장
	 *
	 * @param referenceId 참조 ID (상품 ID, 게시글 ID 등)
	 * @param images      확정된 이미지 리스트 (순서대로)
	 * @return 생성된 ImageSequence 리스트
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public List<ImageSequence> recreateSequences(String referenceId, List<Image> images) {
		log.info("Recreating image sequences for referenceId: {}, imageCount: {}", referenceId, images.size());
		
		// 1. 기존 시퀀스 삭제
		imageSequenceRepository.deleteByReferenceId(referenceId);
		log.debug("Deleted existing sequences for referenceId: {}", referenceId);
		
		// 2. 이미지가 없으면 빈 리스트 반환
		if (images == null || images.isEmpty()) {
			log.info("No images to create sequences for referenceId: {}", referenceId);
			return new ArrayList<>();
		}
		
		// 3. 새로운 시퀀스 생성
		List<ImageSequence> sequences = new ArrayList<>();
		for (int i = 0; i < images.size(); i++) {
			Image image = images.get(i);
			ImageSequence sequence = ImageSequence.of(referenceId, image, i);
			sequences.add(sequence);
			log.debug("Created sequence: referenceId={}, imageId={}, seqNumber={}",
					referenceId, image.getId(), i);
		}
		
		// 4. 일괄 저장
		List<ImageSequence> savedSequences = imageSequenceRepository.saveAll(sequences);
		log.info("Successfully saved {} image sequences for referenceId: {}",
				savedSequences.size(), referenceId);
		
		return savedSequences;
	}
	
	/**
	 * 특정 referenceId의 모든 시퀀스를 조회합니다.
	 *
	 * @param referenceId 참조 ID
	 * @return 순서대로 정렬된 ImageSequence 리스트
	 */
	@Transactional(readOnly = true)
	public List<ImageSequence> getSequences(String referenceId) {
		log.debug("Getting sequences for referenceId: {}", referenceId);
		return imageSequenceRepository.findByReferenceIdOrderBySeqNumberAsc(referenceId);
	}
	
	/**
	 * 특정 referenceId의 모든 시퀀스를 삭제합니다.
	 * <p>
	 * REQUIRES_NEW로 새로운 트랜잭션을 시작하여 실행됩니다.
	 * 이는 TransactionalEventListener의 AFTER_COMMIT 단계에서도 정상 동작하도록 합니다.
	 *
	 * @param referenceId 참조 ID
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void deleteSequences(String referenceId) {
		log.info("Deleting all sequences for referenceId: {}", referenceId);
		imageSequenceRepository.deleteByReferenceId(referenceId);
	}
	
	/**
	 * 특정 referenceId의 이미지 개수를 조회합니다.
	 *
	 * @param referenceId 참조 ID
	 * @return 이미지 개수
	 */
	@Transactional(readOnly = true)
	public long countSequences(String referenceId) {
		return imageSequenceRepository.countByReferenceId(referenceId);
	}
}
