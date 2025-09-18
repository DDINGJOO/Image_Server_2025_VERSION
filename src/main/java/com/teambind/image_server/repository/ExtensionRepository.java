package com.teambind.image_server.repository;


import com.teambind.image_server.entity.Extension;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExtensionRepository extends JpaRepository<Extension, Long> {
}
