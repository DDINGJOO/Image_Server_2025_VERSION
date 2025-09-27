package com.teambind.image_server.repository;


import com.teambind.image_server.entity.Image;
import com.teambind.image_server.entity.ReferenceType;
import com.teambind.image_server.enums.ImageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImageRepository extends JpaRepository<Image, String> {
    Image findByIdAndUploaderIdAndReferenceType(String imageId, String uploaderId, ReferenceType referenceType);

    List<Image> findAllByStatusNot(ImageStatus status);
}
