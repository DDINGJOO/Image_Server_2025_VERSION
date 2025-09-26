package com.teambind.image_server.event.publish;


import com.teambind.image_server.entity.Image;
import com.teambind.image_server.event.EventPublisher;
import com.teambind.image_server.event.events.ImageChangeEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ImageChangeEventPublisher {
    private final EventPublisher eventPublisher;


    public void imageChangeEvent(Image image) {
        if (image == null) return;
        String topic = image.getReferenceType().getCode().toLowerCase() + "-image-changed";
        ImageChangeEvent imageChangeEvent = new ImageChangeEvent(image.getReferenceId(), image.getImageUrl());
        eventPublisher.publish(topic, imageChangeEvent);
    }
}
