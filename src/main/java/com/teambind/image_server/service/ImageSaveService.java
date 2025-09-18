package com.teambind.image_server.service;

import com.teambind.image_server.entity.Image;
import com.teambind.image_server.exception.CustomException;
import com.teambind.image_server.util.convertor.ImageUtil;
import com.teambind.image_server.util.store.LocalImageStorage;
import com.teambind.image_server.util.validator.ExtensionValidator;
import com.teambind.image_server.util.validator.ReferenceValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageSaveService {
    private final ExtensionValidator extensionValidator;
    private final ReferenceValidator referenceValidator;
    private final LocalImageStorage imageStorage;


    public Image saveImage(MultipartFile file, String fileName, String uploaderId, Long sequence, String category) {
        if (!extensionValidator.isValid(fileName)) {
            //TODO
            throw new IllegalArgumentException("Invalid file extension");
        }
        if (!referenceValidator.referenceValidate(category)) {
            //TODO
            throw new IllegalArgumentException("Invalid reference type");
        }

        String uuid = UUID.randomUUID().toString();
        String webpFileName = uuid + ".webp";
        String datePath = LocalDateTime.now().toLocalDate().toString().replace("-", "/");
        String storedPath = category.toUpperCase() + "/" + datePath + "/" + webpFileName;

        byte[] webpBytes;
        try {
            // ① WebP 변환
            webpBytes = ImageUtil.toWebp(file, 0.8f);
            imageStorage.store(webpBytes, storedPath);

        } catch (CustomException | IOException e) {
            throw new RuntimeException(e);
        }

        Image image = Image.builder()
                //TODO
                .build();


        return image;
    }


}
