package com.teambind.image_server.repository;


import com.teambind.image_server.entity.ImageSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 이미지 순서 관리 리포지토리
 *
 * @author Image Server Team
 * @since 2.0
 */
@Repository
public interface ImageSequenceRepository extends JpaRepository<ImageSequence, Long> {
	
	/**
	 * referenceId의 모든 시퀀스를 순서대로 조회합니다.
	 * Image를 함께 fetch join으로 가져와서 N+1 문제를 방지합니다.
	 *
	 * @param referenceId 참조 ID (상품 ID, 게시글 ID 등)
	 * @return 순서대로 정렬된 ImageSequence 리스트
	 */
	@Query("SELECT s FROM ImageSequence s " +
			"JOIN FETCH s.image " +
			"WHERE s.referenceId = :referenceId " +
			"ORDER BY s.seqNumber ASC")
	List<ImageSequence> findByReferenceIdOrderBySeqNumberAsc(@Param("referenceId") String referenceId);
	
	/**
	 * referenceId의 모든 시퀀스를 삭제합니다.
	 *
	 * @param referenceId 참조 ID
	 */
	@Modifying
	@Transactional
	@Query("DELETE FROM ImageSequence s WHERE s.referenceId = :referenceId")
	void deleteByReferenceId(@Param("referenceId") String referenceId);
	
	/**
	 * 특정 이미지가 어떤 referenceId들에 사용되는지 조회합니다.
	 *
	 * @param imageId 이미지 ID
	 * @return 해당 이미지를 사용하는 모든 ImageSequence
	 */
	List<ImageSequence> findByImageId(String imageId);
	
	/**
	 * 특정 referenceId의 이미지 개수를 조회합니다.
	 *
	 * @param referenceId 참조 ID
	 * @return 이미지 개수
	 */
	@Query("SELECT COUNT(s) FROM ImageSequence s WHERE s.referenceId = :referenceId")
	long countByReferenceId(@Param("referenceId") String referenceId);
	
	/**
	 * 특정 referenceId와 imageId 조합이 존재하는지 확인합니다.
	 *
	 * @param referenceId 참조 ID
	 * @param imageId     이미지 ID
	 * @return 존재 여부
	 */
	boolean existsByReferenceIdAndImageId(String referenceId, String imageId);
}
