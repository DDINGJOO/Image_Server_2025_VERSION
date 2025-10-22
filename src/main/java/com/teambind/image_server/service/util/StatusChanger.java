package com.teambind.image_server.service.util;

import com.teambind.image_server.entity.Image;
import com.teambind.image_server.entity.StatusHistory;
import com.teambind.image_server.enums.ImageStatus;
import com.teambind.image_server.repository.StatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Transactional
public class StatusChanger {
	// 히스토리 반영 + 스테이터스 변경
	
	private final StatusHistoryRepository statusHistoryRepository;
	
	public Image changeStatus(Image image, ImageStatus newStatus) {
		// 기존 상태를 먼저 보관하고, 그 다음 변경해야 히스토리의 old/new가 정확해진다.
		ImageStatus oldStatus = image.getStatus();
		image.setStatus(newStatus);
		
		// 양방향 연관관계 유지: orphanRemoval에 의해 삭제되지 않도록 이미지 컬렉션에도 추가한다.
		StatusHistory history = StatusHistory.builder()
				.image(image)
				.oldStatus(oldStatus)
				.newStatus(newStatus)
				.updatedAt(LocalDateTime.now())
				.updatedBy("SYSTEM")
				.build();
		statusHistoryRepository.save(history);
		
		
		if (image.getStatusHistories() != null) {
			image.getStatusHistories().add(history);
		}
		return image;
	}
}
