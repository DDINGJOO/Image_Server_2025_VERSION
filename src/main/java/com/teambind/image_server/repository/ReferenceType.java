package com.teambind.image_server.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReferenceType extends JpaRepository<ReferenceType, Integer> {
}
