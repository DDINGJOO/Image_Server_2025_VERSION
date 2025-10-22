package com.teambind.image_server.event.publish;


import com.teambind.image_server.entity.Image;
import com.teambind.image_server.entity.ImageSequence;
import com.teambind.image_server.event.EventPublisher;
import com.teambind.image_server.event.events.ImageChangeEvent;
import com.teambind.image_server.event.events.SequentialImageChangeEvent;
import com.teambind.image_server.util.InitialSetup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ImageChangeEventPublisher {
	private final EventPublisher eventPublisher;
	
	
	public void imageChangeEvent(Image image) {
		if (image == null) return;
		String topic = image.getReferenceType().getCode().toLowerCase() + "-image-changed";
		ImageChangeEvent imageChangeEvent = new ImageChangeEvent(image.getReferenceId(), image.getReferenceId(), image.getImageUrl());
		eventPublisher.publish(topic, imageChangeEvent);
	}
	
	public void imageDeletedEvent(String referenceId) {
		if (referenceId == null) return;
		String topic = InitialSetup.ALL_REFERENCE_TYPE_MAP.get(referenceId).getCode().toLowerCase() + "-image-changed";
		ImageChangeEvent imageChangeEvent = new ImageChangeEvent(referenceId, null, null);
		eventPublisher.publish(topic, imageChangeEvent);
	}
	
	public void imagesChangeEvent(List<ImageSequence> imageSequences) {
		if (imageSequences == null || imageSequences.isEmpty()) {
			return;
		}
		
		// 첫 번째 이미지에서 ReferenceType 가져오기
		ImageSequence firstSequence = imageSequences.get(0);
		Image firstImage = firstSequence.getImage();
		String topic = firstImage.getReferenceType().getCode().toLowerCase() + "-image-changed";
		
		// SequentialImageChangeEvent 리스트 생성
		List<SequentialImageChangeEvent> events = new ArrayList<>();
		for (ImageSequence sequence : imageSequences) {
			Image img = sequence.getImage();
			SequentialImageChangeEvent event = new SequentialImageChangeEvent(
					img.getId(),
					img.getImageUrl(),
					img.getReferenceId(),
					sequence.getSeqNumber()
			);
			events.add(event);
		}
		
		// 이벤트 발행
		eventPublisher.publish(topic, events);
	}
}
