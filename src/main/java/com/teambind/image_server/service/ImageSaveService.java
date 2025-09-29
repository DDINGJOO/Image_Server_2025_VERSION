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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public Map<String, String> saveImage(MultipartFile file, String uploaderId, String category) throws CustomException {
        if (!extensionValidator.isValid(file.getOriginalFilename())) {
            throw new CustomException(ErrorCode.FILE_EXTENSION_NOT_FOUND);
        }
        if (!referenceValidator.referenceValidate(category)) {
            throw new CustomException(ErrorCode.REFERENCE_TYPE_NOT_FOUND);
        }
        return saveImage(file, file.getOriginalFilename(), uploaderId, category);
    }

    private Map<String, String> saveImage(MultipartFile file, String fileName, String uploaderId, String category) throws CustomException {
        String uuid = UUID.randomUUID().toString(); // ImageId
        String datePath = LocalDateTime.now().toLocalDate().toString().replace("-", "/");

        // 기본값들 준비
        String originExtUpper = extensionParser.extensionParse(fileName).toUpperCase();
        String originExtLower = originExtUpper.toLowerCase();

        String storedPath; // 실제 저장 경로 (성공/폴백에 따라 달라짐)
        byte[] savedBytes; // 실제로 저장된 바이트
        String convertedFormatCode; // 저장된 파일의 포맷 코드

        try {
            // 1) WebP 변환 시도
            byte[] webpBytes = ImageUtil.toWebp(file, 0.8f);
            String webpFileName = uuid + ".webp";
            storedPath = category.toUpperCase() + "/" + datePath + "/" + webpFileName;
            imageStorage.store(webpBytes, storedPath);

            savedBytes = webpBytes;
            convertedFormatCode = "WEBP";
        } catch (Exception e) {
            // 2) 예외 유형 판단: Rosetta/네이티브 로더 관련 문제라면 원본 그대로 저장 (운영 환경 안정성 우선)
            String msg = e.getMessage() == null ? "" : e.getMessage();
            boolean rosettaLike = msg.contains("rosetta error") || msg.contains("/lib64/ld-linux-x86-64.so.2");

            if (rosettaLike) {
                try {
                    String fallbackName = uuid + "." + originExtLower;
                    storedPath = category.toUpperCase() + "/" + datePath + "/" + fallbackName;
                    byte[] originalBytes = file.getBytes();
                    imageStorage.store(originalBytes, storedPath);

                    savedBytes = originalBytes;
                    convertedFormatCode = originExtUpper; // WebP가 아닌 원본 포맷으로 저장됨
                } catch (IOException ioEx) {
                    System.err.println("[IO_EXCEPTION_FALLBACK] cause=" + ioEx.getClass().getName() + ", message=" + ioEx.getMessage());
                    throw new CustomException(ErrorCode.IOException);
                }
            } else {
                // 손상된 이미지 등 일반적 변환 실패는 IMAGE_SAVE_FAILED로 매핑하여 기존 기대 동작 유지
                System.err.println("[IMAGE_CONVERT_FAIL] cause=" + e.getClass().getName() + ", message=" + msg);
                throw new CustomException(ErrorCode.IMAGE_SAVE_FAILED);
            }
        }

        // 3) 엔티티 빌드 및 저장
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
                .convertedFormat(extensionMap.get(convertedFormatCode))
                .originFormat(extensionMap.get(originExtUpper))
                .originSize(file.getSize())
                .convertedSize((long) savedBytes.length)
                .storageLocation(storedPath)
                .build();

        image.setStorageObject(storageObject);
        imageRepository.save(image);
        return Map.of("id", fileName, "url", image.getImageUrl());
    }

    public Map<String, String> saveImages(List<MultipartFile> files, String uploaderId, String category) throws CustomException {
        Map<String, String> images = new HashMap<>();
        for (MultipartFile file : files) {
            images.put(file.getOriginalFilename(), saveImage(file, file.getOriginalFilename(), uploaderId, category).get("url"));
        }
        return images;
    }
}
