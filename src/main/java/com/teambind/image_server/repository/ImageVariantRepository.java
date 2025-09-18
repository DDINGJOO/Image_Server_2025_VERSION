package com.teambind.image_server.repository;


import com.teambind.image_server.entity.ImageVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImageVariantRepository extends JpaRepository<ImageVariant, Long> {
}
