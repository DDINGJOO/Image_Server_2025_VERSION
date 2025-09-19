package com.teambind.image_server.repository;


import com.teambind.image_server.entity.Image;
import com.teambind.image_server.entity.ReferenceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImageRepository extends JpaRepository<Image, String> {
    Image findByIdAndUploaderIdAndReferenceType(String imageId, String uploaderId, ReferenceType referenceType);

}
