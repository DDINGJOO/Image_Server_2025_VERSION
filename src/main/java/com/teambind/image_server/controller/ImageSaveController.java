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
    public ResponseEntity<String> saveImage(@RequestParam MultipartFile file, String referenceId, String uploaderId, String category) throws CustomException {
        Image image = imageSaveService.saveImage(file, referenceId, uploaderId, category);
        return ResponseEntity.ok().body(image.getId());
    }

    @PostMapping("/batch")
    public ResponseEntity<List<String>> saveImage(@RequestParam List<MultipartFile> file, String referenceId, String uploaderId, String category) throws CustomException {
        List<Image> image = imageSaveService.saveImages(file, referenceId, uploaderId, category);
        List<String> imageIds = new ArrayList<>();
        for (Image img : image) {
            imageIds.add(img.getId());
        }

        return ResponseEntity.ok().body(imageIds);
    }
}
