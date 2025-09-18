package com.teambind.image_server.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface Extension extends JpaRepository<Extension, Long> {
}
