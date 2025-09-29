package com.teambind.image_server.event.publish;


import com.teambind.image_server.entity.Image;
import com.teambind.image_server.event.EventPublisher;
import com.teambind.image_server.event.events.ImageChangeEvent;
import com.teambind.image_server.event.events.SequentialImageChangeEvent;
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
        ImageChangeEvent imageChangeEvent = new ImageChangeEvent(image.getReferenceId(), image.getImageUrl());
        eventPublisher.publish(topic, imageChangeEvent);
    }

    public void imagesChangeEvent(List<Image> image) {
        if (image == null) return;
        String topic = image.getFirst().getReferenceType().getCode().toLowerCase() + "-image-changed";
        List<SequentialImageChangeEvent> imageChangeEvent = new ArrayList<>();
        for (Image i : image) {
            new SequentialImageChangeEvent(i.getId(), i.getImageUrl());
        }
        eventPublisher.publish(topic, imageChangeEvent);
    }
}
