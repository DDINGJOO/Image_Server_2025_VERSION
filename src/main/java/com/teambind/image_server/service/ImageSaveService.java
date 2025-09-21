package com.teambind.image_server.service;

import com.teambind.image_server.entity.Image;
import com.teambind.image_server.entity.StorageObject;
import com.teambind.image_server.enums.ImageStatus;
import com.teambind.image_server.exception.CustomException;
import com.teambind.image_server.exception.ErrorCode;
import com.teambind.image_server.repository.ImageRepository;
import com.teambind.image_server.util.convertor.ImageUtil;
import com.teambind.image_server.util.helper.UrlHelper;
import com.teambind.image_server.util.store.LocalImageStorage;
import com.teambind.image_server.util.validator.ExtensionParser;
import com.teambind.image_server.util.validator.ExtensionValidator;
import com.teambind.image_server.util.validator.ReferenceValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.teambind.image_server.ImageServerApplication.extensionMap;
import static com.teambind.image_server.ImageServerApplication.referenceTypeMap;

@Service
@RequiredArgsConstructor
public class ImageSaveService {
    private final ExtensionValidator extensionValidator;
    private final UrlHelper urlHelper;
    private final ReferenceValidator referenceValidator;
    private final ImageRepository imageRepository;
    private final LocalImageStorage imageStorage;
    private final ExtensionParser extensionParser;

    public Image saveImage(MultipartFile file, String uploaderId, String category) throws CustomException {
        if (!extensionValidator.isValid(file.getOriginalFilename())) {
            throw new CustomException(ErrorCode.FILE_EXTENSION_NOT_FOUND);
        }
        if (!referenceValidator.referenceValidate(category)) {
            throw new CustomException(ErrorCode.REFERENCE_TYPE_NOT_FOUND);
        }
        return saveImage(file, file.getOriginalFilename(), uploaderId, category);
    }

    private Image saveImage(MultipartFile file, String fileName, String uploaderId, String category) throws CustomException {


        String uuid = UUID.randomUUID().toString(); // ImageId
        String webpFileName = uuid + ".webp";
        String datePath = LocalDateTime.now().toLocalDate().toString().replace("-", "/");
        String storedPath = category.toUpperCase() + "/" + datePath + "/" + webpFileName;

        byte[] webpBytes;
        try {
            // WebP 변환
            webpBytes = ImageUtil.toWebp(file, 0.8f);
            imageStorage.store(webpBytes, storedPath);

        } catch (CustomException | IOException e) {
            throw new CustomException(ErrorCode.IMAGE_SAVE_FAILED);
        }

        // 이미지 확정 시에 순서 및 기존 이미지 삭제 처리
        Image image = Image.builder()
                .id(uuid)
                .createdAt(LocalDateTime.now())
                .idDeleted(false)
                .status(ImageStatus.TEMP)
                .referenceType(referenceTypeMap.get(category.toUpperCase()))
                .imageUrl(urlHelper.getUrl(storedPath))
                .uploaderId(uploaderId)
                .build();

        StorageObject storageObject = StorageObject.builder()
                .image(image)
                .convertedFormat(extensionMap.get("WEBP"))
                .originFormat(extensionMap.get(extensionParser.extensionParse(fileName).toUpperCase()))
                .originSize(file.getSize())
                .convertedSize((long) webpBytes.length)
                .storageLocation(storedPath)
                .build();

        image.setStorageObject(storageObject);
        imageRepository.save(image);
        return image;
    }


    public List<Image> saveImages(List<MultipartFile> files, String uploaderId, String category) throws CustomException {

        List<Image> images = new ArrayList<>();
        for (MultipartFile file : files) {
            images.add(saveImage(file, file.getOriginalFilename(), uploaderId, category));
        }
        return images;
    }


}
