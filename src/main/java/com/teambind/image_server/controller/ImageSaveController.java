package com.teambind.image_server.controller;


import com.teambind.image_server.entity.Image;
import com.teambind.image_server.exception.CustomException;
import com.teambind.image_server.service.ImageSaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/images")
public class ImageSaveController {
    private final ImageSaveService imageSaveService;

    @PostMapping
    public ResponseEntity<String> saveImage(@RequestParam MultipartFile file, String uploaderId, String category) throws CustomException {
        String imageId = imageSaveService.saveImage(file, uploaderId, category).getId();
        return ResponseEntity.ok().body(imageId);
    }

    @PostMapping("/batch")
    public ResponseEntity<List<String>> saveImage(@RequestParam List<MultipartFile> file, String uploaderId, String category) throws CustomException {
        List<Image> imageId = imageSaveService.saveImages(file, uploaderId, category);
        List<String> imageIds = new ArrayList<>();
        for (Image img : imageId) {
            imageIds.add(img.getId());
        }

        return ResponseEntity.ok().body(imageIds);
    }
}
