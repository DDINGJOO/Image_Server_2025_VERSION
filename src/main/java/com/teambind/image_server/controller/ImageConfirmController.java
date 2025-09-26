package com.teambind.image_server.controller;


import com.teambind.image_server.entity.Image;
import com.teambind.image_server.event.publish.ImageChangeEventPublisher;
import com.teambind.image_server.service.ImageConfirmService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageConfirmController {
    private final ImageConfirmService imageConfirmService;
    private final ImageChangeEventPublisher eventPublisher;

    //TODO CONVERT EVENT Handling
    @PatchMapping("/{imageId}/confirm")
    public void confirmImage(@PathVariable(name = "imageId") String imageId) throws Exception {
        Image image = imageConfirmService.confirmImage(imageId);
        eventPublisher.imageChangeEvent(image);
    }
}
