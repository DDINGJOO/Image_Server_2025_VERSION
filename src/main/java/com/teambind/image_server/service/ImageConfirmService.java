package com.teambind.image_server.service;


import com.teambind.image_server.entity.Image;
import com.teambind.image_server.entity.ReferenceType;
import com.teambind.image_server.enums.ImageStatus;
import com.teambind.image_server.event.events.SequentialImageChangeEvent;
import com.teambind.image_server.exception.CustomException;
import com.teambind.image_server.exception.ErrorCode;
import com.teambind.image_server.repository.ImageRepository;
import com.teambind.image_server.util.statuschanger.StatusChanger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.teambind.image_server.ImageServerApplication.referenceTypeMap;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ImageConfirmService {
    private final ImageRepository imageRepository;
    private final StatusChanger statusChanger;

    public Image confirmImage(String imageId, String referenceId) {

        log.info("Confirming image with id: {}", imageId);
        if (imageId == null || imageId.isEmpty()) {
            throw new CustomException(ErrorCode.IMAGE_NOT_FOUND);
        }
        Image image = imageRepository.findById(imageId).orElseThrow(() -> new CustomException(ErrorCode.IMAGE_NOT_FOUND));
        image.setReferenceId(referenceId);
        if (image.getStatus().equals(ImageStatus.CONFIRMED))
            throw new CustomException(ErrorCode.IMAGE_ALREADY_CONFIRMED);
        image = statusChanger.changeStatus(image, ImageStatus.CONFIRMED);

        if (image.getReferenceType().getName().equals("PROFILE")) {
            deleteOldProfileImg(imageId, image.getUploaderId(), referenceTypeMap.get("PROFILE"));
        }
        imageRepository.save(image);
        return image;
    }

    public void deleteOldProfileImg(String imageId, String uploaderId, ReferenceType referenceType) {
        log.info("Confirming profile image with id: {}", imageId);

        Image oldProfile = imageRepository.findByIdAndUploaderIdAndReferenceType(imageId, uploaderId, referenceType);

        if (oldProfile == null) {
            return;
        }

        oldProfile = statusChanger.changeStatus(oldProfile, ImageStatus.DELETED);
        imageRepository.save(oldProfile);
    }


    @Transactional
    public List<Image> confirmImages(List<String> imageId, String referenceId) {
        List<Image> images = imageRepository.findAllByReferenceId(referenceId);

        List<SequentialImageChangeEvent> imageChangeEvents = new ArrayList<>();
        // 기존 이미지 삭제 대기 처리
        for (Image image : images) {
            image = statusChanger.changeStatus(image, ImageStatus.DELETED);
            imageRepository.save(image);
        }

        // 나머지 이미지 확정 처리
        List<Image> confirmedImages = imageRepository.findAllByIdIn(imageId);
        for (Image image : confirmedImages) {
            statusChanger.changeStatus(image, ImageStatus.CONFIRMED);
        }
        imageRepository.saveAll(confirmedImages);
        Map<String, Image> confirmedMap = confirmedImages.stream()
                .collect(Collectors.toMap(Image::getId, Function.identity()));

        //imageId 의 id 순서랑 똑같은 Image리스트를 만들고 싶어
        List<Image> orderedConfirmed = new ArrayList<>();
        for (String id : imageId) {
            Image img = confirmedMap.get(id);
            if (img != null) {
                orderedConfirmed.add(img);
            }
        }
        return orderedConfirmed;
    }
}
