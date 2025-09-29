package com.teambind.image_server.controller;


import com.teambind.image_server.entity.Image;
import com.teambind.image_server.event.publish.ImageChangeEventPublisher;
import com.teambind.image_server.service.ImageConfirmService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageConfirmController {
    private final ImageConfirmService imageConfirmService;
    private final ImageChangeEventPublisher eventPublisher;

    //TODO CONVERT EVENT Handling
    @PatchMapping("/{referenceId}/{imageId}/confirm")
    public void confirmImage(@PathVariable(name = "imageId") List<String> imageId, @PathVariable(name = "referenceId") String referenceId) {
        if (imageId.size() == 1) {
            Image image = imageConfirmService.confirmImage(imageId.getFirst(), referenceId);
            eventPublisher.imageChangeEvent(image);
        } else {
            eventPublisher.imagesChangeEvent
                    (imageConfirmService.confirmImages(imageId, referenceId));

        }
    }

}
