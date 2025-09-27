package com.teambind.image_server.service;


import com.teambind.image_server.entity.Image;
import com.teambind.image_server.enums.ImageStatus;
import com.teambind.image_server.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleService {

    private final ImageRepository imageRepository;

    public void cleanUpUnusedImages() {
        List<Image> images = imageRepository.findAllByStatusNot(ImageStatus.CONFIRMED);
        List<Image> imagesToDelete = new ArrayList<>();
        for (Image image : images) {
            if (image.getCreatedAt().plusDays(2).isBefore(java.time.LocalDateTime.now())) {
                imagesToDelete.add(image);
            }
        }
        imageRepository.deleteAll(imagesToDelete);
    }
}
