package com.teambind.image_server.repository;


import com.teambind.image_server.entity.ImageSequence;
import com.teambind.image_server.entity.key.ImageSequenceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImageSequenceRepository extends JpaRepository<ImageSequence, ImageSequenceId> {
}
